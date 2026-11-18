const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_founder";
const ADMIN_EMAIL = "littleb0750@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied everywhere", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tournaments").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("adminConfig").doc("master").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Rogue",
      email: "rogue@test.com",
      phone: "9999999999",
      bgmiIgn: "RogueOP",
      bgmiUid: "5123456789",
      depositBalance: 0,
      winningBalance: 0,
      matchesPlayed: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profile isolation, zero-balance creation, and non-inflation update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@test.com", email_verified: true }).firestore();
  const now = pastDate(10000);

  // User cannot create profile with free money
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@test.com",
      phone: "9876543210",
      bgmiIgn: "AliceOP",
      bgmiUid: "5111222333",
      depositBalance: 5000,
      winningBalance: 0,
      matchesPlayed: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid creation with 0 balance succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@test.com",
      phone: "9876543210",
      bgmiIgn: "AliceOP",
      bgmiUid: "5111222333",
      depositBalance: 0,
      winningBalance: 0,
      matchesPlayed: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Profile edit succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bgmiIgn: "NaxoraAlice",
      updatedAt: pastDate(1000),
    })
  );

  // Self-crediting balance fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      depositBalance: 999,
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user read & list fail (PII protection)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Admin bootstrap email requires email_verified=true and guards adminConfig", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@test.com",
    email_verified: true,
  }).firestore();

  // Spoofed unverified admin email is rejected
  await assertFails(
    spoofedAdminDb.collection("adminConfig").doc("master").set({
      adminEmail: ADMIN_EMAIL,
      recoveryEmail: ADMIN_EMAIL,
      passcodeHash: "a".repeat(64),
      updatedByUid: "spoof_uid",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Verified founder admin succeeds with registered recoveryEmail + audit log
  await assertSucceeds(
    realAdminDb.collection("adminConfig").doc("master").set({
      adminEmail: ADMIN_EMAIL,
      recoveryEmail: ADMIN_EMAIL,
      passcodeHash: "a".repeat(64),
      recoveryHint: "Founder PIN",
      updatedByUid: ADMIN_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(
    realAdminDb.collection("adminRecoveryLogs").doc("log_1").set({
      adminUid: ADMIN_UID,
      recoveryEmail: ADMIN_EMAIL,
      eventType: "PASSWORD_RESET",
      note: "Reset via registered recovery email",
      createdAt: pastDate(),
    })
  );

  // Normal user cannot read adminConfig or adminRecoveryLogs, and audit logs are immutable
  await assertFails(aliceDb.collection("adminConfig").doc("master").get());
  await assertFails(aliceDb.collection("adminRecoveryLogs").get());
  await assertFails(
    realAdminDb.collection("adminRecoveryLogs").doc("log_1").update({ note: "Tampered" })
  );
  await assertFails(
    realAdminDb.collection("adminRecoveryLogs").doc("log_1").delete()
  );
});

test("Admin recovery email verification enforces verified matching email on reset", async () => {
  const founderDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const RECOVERY_UID = "mod_recovery_uid";
  const RECOVERY_EMAIL = "recovery.admin@naxora.com";
  const OTHER_ADMIN_UID = "mod_other_uid";

  const created = pastDate(10000);

  await assertSucceeds(
    founderDb.collection("adminConfig").doc("master").set({
      adminEmail: ADMIN_EMAIL,
      recoveryEmail: RECOVERY_EMAIL,
      passcodeHash: "a".repeat(64),
      recoveryHint: "Obsidian Vault",
      updatedByUid: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    founderDb.collection("admins").doc(RECOVERY_UID).set({
      uid: RECOVERY_UID,
      email: RECOVERY_EMAIL,
      role: "MODERATOR",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    founderDb.collection("admins").doc(OTHER_ADMIN_UID).set({
      uid: OTHER_ADMIN_UID,
      email: "other.mod@naxora.com",
      role: "MODERATOR",
      createdAt: created,
      updatedAt: created,
    })
  );

  const otherAdminDb = testEnv.authenticatedContext(OTHER_ADMIN_UID, {
    email: "other.mod@naxora.com",
    email_verified: true,
  }).firestore();
  await assertFails(
    otherAdminDb.collection("adminConfig").doc("master").update({
      adminEmail: ADMIN_EMAIL,
      recoveryEmail: RECOVERY_EMAIL,
      passcodeHash: "b".repeat(64),
      recoveryHint: "Hijacked",
      updatedByUid: OTHER_ADMIN_UID,
      updatedAt: pastDate(1000),
    })
  );

  const unverifiedRecoveryDb = testEnv.authenticatedContext(RECOVERY_UID, {
    email: RECOVERY_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(
    unverifiedRecoveryDb.collection("adminConfig").doc("master").update({
      adminEmail: ADMIN_EMAIL,
      recoveryEmail: RECOVERY_EMAIL,
      passcodeHash: "b".repeat(64),
      recoveryHint: "Unverified",
      updatedByUid: RECOVERY_UID,
      updatedAt: pastDate(1000),
    })
  );

  const verifiedRecoveryDb = testEnv.authenticatedContext(RECOVERY_UID, {
    email: RECOVERY_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    verifiedRecoveryDb.collection("adminConfig").doc("master").update({
      adminEmail: ADMIN_EMAIL,
      recoveryEmail: RECOVERY_EMAIL,
      passcodeHash: "c".repeat(64),
      recoveryHint: "Rotated Key",
      updatedByUid: RECOVERY_UID,
      updatedAt: pastDate(1000),
    })
  );
});

test("Tournament 25-slot booking atomicity and protected Room ID/Password", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@test.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@test.com",
    email_verified: true,
  }).firestore();

  const now = pastDate(20000);

  // Seed user Alice with 100 deposit balance via admin
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@test.com",
      phone: "9876543210",
      bgmiIgn: "AliceOP",
      bgmiUid: "5111222333",
      depositBalance: 0,
      winningBalance: 0,
      matchesPlayed: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      depositBalance: 100,
      updatedAt: pastDate(10000),
    })
  );

  // Admin creates 25-slot Squad tournament + RoomAccess
  await assertSucceeds(
    adminDb.collection("tournaments").doc("tourney_1").set({
      title: "Naxora Pro Scrims #1",
      mapName: "Erangel",
      mode: "SQUAD",
      maxSlots: 25,
      playersPerSlot: 4,
      bookedSlotNumbers: [],
      entryFee: 50,
      prizePool: 1000,
      perKillPrize: 20,
      startTimeMillis: Date.now() + 3600000,
      status: "UPCOMING",
      roomPublished: true,
      roomForceVisible: true,
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("tournaments").doc("tourney_1").collection("roomAccess").doc("credentials").set({
      roomId: "99441122",
      roomPassword: "NAX77",
      roomPublished: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Before joining, Alice & Bob CANNOT read room credentials
  await assertFails(
    aliceDb.collection("tournaments").doc("tourney_1").collection("roomAccess").doc("credentials").get()
  );

  // Aligned tournament list query succeeds; unfiltered query fails
  await assertSucceeds(
    aliceDb.collection("tournaments").where("status", "in", ["UPCOMING", "LIVE", "COMPLETED"]).get()
  );
  await assertFails(aliceDb.collection("tournaments").get());

  // Atomic batch join: deduct balance + create registration + update bookedSlotNumbers + create ENTRY_FEE tx
  const batch = aliceDb.batch();
  batch.update(aliceDb.collection("users").doc(ALICE_UID), {
    depositBalance: 50,
    matchesPlayed: 1,
    updatedAt: pastDate(1000),
  });
  batch.set(
    aliceDb.collection("tournaments").doc("tourney_1").collection("registrations").doc(ALICE_UID),
    {
      userId: ALICE_UID,
      tournamentId: "tourney_1",
      slotNumber: 7,
      teamName: "Team Naxora",
      playerIgns: ["AliceOP", "ViperOP", "ClutchGod", "ShadowX"],
      playerUids: ["5111222333", "5222333444", "5333444555", "5444555666"],
      entryFeePaid: 50,
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    }
  );
  batch.update(aliceDb.collection("tournaments").doc("tourney_1"), {
    bookedSlotNumbers: [7],
    updatedAt: pastDate(1000),
  });
  batch.set(aliceDb.collection("walletTransactions").doc("tx_entry_1"), {
    userId: ALICE_UID,
    userName: "Alice",
    type: "ENTRY_FEE",
    amount: 50,
    status: "APPROVED",
    note: "Joined Naxora Pro Scrims #1 (Slot #7)",
    createdAt: pastDate(1000),
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(batch.commit());

  // After joining, Alice CAN read room credentials, Bob still CANNOT
  await assertSucceeds(
    aliceDb.collection("tournaments").doc("tourney_1").collection("roomAccess").doc("credentials").get()
  );
  await assertFails(
    bobDb.collection("tournaments").doc("tourney_1").collection("roomAccess").doc("credentials").get()
  );

  // Bob cannot read Alice's registration or list all registrations
  await assertFails(
    bobDb.collection("tournaments").doc("tourney_1").collection("registrations").doc(ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("tournaments").doc("tourney_1").collection("registrations").get()
  );
  // Bob's filtered query for his own registration succeeds
  await assertSucceeds(
    bobDb.collection("tournaments").doc("tourney_1").collection("registrations").where("userId", "==", BOB_UID).get()
  );
});

test("Wallet transactions: 12-digit UTR deposit, admin approval, and query isolation", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@test.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@test.com",
    email_verified: true,
  }).firestore();

  // Invalid UTR (not 12 digits) rejected
  await assertFails(
    aliceDb.collection("walletTransactions").doc("dep_bad").set({
      userId: ALICE_UID,
      userName: "Alice",
      type: "DEPOSIT",
      amount: 200,
      status: "PENDING",
      utrNumber: "12345",
      note: "UPI Add Cash",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // User cannot create pre-approved deposit
  await assertFails(
    aliceDb.collection("walletTransactions").doc("dep_cheat").set({
      userId: ALICE_UID,
      userName: "Alice",
      type: "DEPOSIT",
      amount: 200,
      status: "APPROVED",
      utrNumber: "123456789012",
      note: "Cheat",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Valid pending deposit with 12-digit UTR succeeds
  await assertSucceeds(
    aliceDb.collection("walletTransactions").doc("dep_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      type: "DEPOSIT",
      amount: 200,
      status: "PENDING",
      utrNumber: "123456789012",
      note: "UPI Add Cash",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Filtered user query succeeds; unfiltered query fails for non-admin
  await assertSucceeds(
    aliceDb.collection("walletTransactions").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("walletTransactions").get());
  await assertFails(bobDb.collection("walletTransactions").doc("dep_1").get());

  // User cannot self-approve
  await assertFails(
    aliceDb.collection("walletTransactions").doc("dep_1").update({
      status: "APPROVED",
      note: "Self approved",
      updatedAt: pastDate(1000),
    })
  );

  // Admin approves pending deposit
  await assertSucceeds(
    adminDb.collection("walletTransactions").doc("dep_1").update({
      status: "APPROVED",
      note: "Verified by Admin",
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state lock: even admin cannot re-mutate APPROVED transaction
  await assertFails(
    adminDb.collection("walletTransactions").doc("dep_1").update({
      status: "REJECTED",
      note: "Reverted",
      updatedAt: pastDate(500),
    })
  );
});

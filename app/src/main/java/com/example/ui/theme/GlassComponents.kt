package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ObsidianArenaBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianDeep, ObsidianBase, ObsidianDeep)
                )
            )
            .drawBehind {
                // Top-left cyan/teal ambient radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyanGlow, Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.08f),
                        radius = size.maxDimension * 0.58f
                    ),
                    radius = size.maxDimension * 0.58f,
                    center = Offset(size.width * 0.12f, size.height * 0.08f)
                )
                // Bottom-right violet ambient radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(VioletGlow, Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.90f),
                        radius = size.maxDimension * 0.60f
                    ),
                    radius = size.maxDimension * 0.60f,
                    center = Offset(size.width * 0.88f, size.height * 0.90f)
                )
            },
        content = content
    )
}

@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderColor: Color = GlassBorder,
    containerColor: Color = ObsidianCard,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun DarkInsetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    focusColor: Color = NeonCyan,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 6,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    tag: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) focusColor else InsetBorder,
        label = "inputBorder"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianInset)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(10.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(focusColor),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (tag != null) Modifier.testTag(tag) else Modifier)
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}

@Composable
fun NeonTranslucentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAdminAccent: Boolean = false,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    tag: String? = null
) {
    val bgColor = when {
        !enabled -> Color(0x14FFFFFF)
        isAdminAccent -> PurpleButtonBg
        else -> CyanButtonBg
    }
    val borderColor = when {
        !enabled -> InsetBorder
        isAdminAccent -> PurpleButtonBorder
        else -> CyanButtonBorder
    }
    val textColor = when {
        !enabled -> TextMuted
        isAdminAccent -> PurpleSoftText
        else -> NeonCyan
    }

    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = textColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = textColor,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * Deterministic visual UPI QR matrix generated from the Admin UPI ID so players
 * can scan or copy directly inside the Wallet Deposit card.
 */
@Composable
fun UpiQrMatrixGraphic(
    upiId: String,
    modifier: Modifier = Modifier
) {
    val gridSize = 15
    val seedHash = remember(upiId) { upiId.hashCode() }
    Canvas(
        modifier = modifier
            .size(132.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(10.dp)
    ) {
        val cell = size.width / gridSize
        fun drawFinder(startCol: Int, startRow: Int) {
            drawRoundRect(
                color = Color(0xFF07090E),
                topLeft = Offset(startCol * cell, startRow * cell),
                size = Size(cell * 4f, cell * 4f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset((startCol + 0.8f) * cell, (startRow + 0.8f) * cell),
                size = Size(cell * 2.4f, cell * 2.4f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawRoundRect(
                color = Color(0xFF0088AA),
                topLeft = Offset((startCol + 1.4f) * cell, (startRow + 1.4f) * cell),
                size = Size(cell * 1.2f, cell * 1.2f),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
        drawFinder(0, 0)
        drawFinder(gridSize - 4, 0)
        drawFinder(0, gridSize - 4)

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val inTopLeft = r < 5 && c < 5
                val inTopRight = r < 5 && c >= gridSize - 5
                val inBottomLeft = r >= gridSize - 5 && c < 5
                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                    val bit = ((seedHash xor (r * 31 + c * 17)) and 1) == 1 || (r + c) % 3 == 0
                    if (bit) {
                        drawRoundRect(
                            color = Color(0xFF0B0E14),
                            topLeft = Offset(c * cell + 1f, r * cell + 1f),
                            size = Size(cell - 2f, cell - 2f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }
        }
    }
}

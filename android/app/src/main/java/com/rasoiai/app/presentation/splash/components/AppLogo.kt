package com.rasoiai.app.presentation.splash.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * RasoiAI App Logo - A cooking pot with steam
 *
 * The logo represents Indian cooking with a traditional cooking pot (handi/patila)
 * in the brand's primary orange color.
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier.aspectRatio(1f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCookingPot(color)
        }
    }
}

private fun DrawScope.drawCookingPot(color: Color) {
    val width = size.width
    val height = size.height
    val strokeWidth = width * 0.06f

    // Pot body (rounded rectangle / oval shape)
    val potTop = height * 0.35f
    val potBottom = height * 0.85f
    val potLeft = width * 0.15f
    val potRight = width * 0.85f
    val potWidth = potRight - potLeft
    val potHeight = potBottom - potTop

    // Draw pot body
    drawRoundRect(
        color = color,
        topLeft = Offset(potLeft, potTop),
        size = Size(potWidth, potHeight),
        cornerRadius = CornerRadius(potWidth * 0.25f, potHeight * 0.3f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Draw pot lid / rim at top
    val rimY = potTop
    val rimLeft = potLeft - width * 0.05f
    val rimRight = potRight + width * 0.05f
    drawLine(
        color = color,
        start = Offset(rimLeft, rimY),
        end = Offset(rimRight, rimY),
        strokeWidth = strokeWidth * 1.5f,
        cap = StrokeCap.Round
    )

    // Draw lid handle (small circle/knob on top)
    val handleCenterX = width * 0.5f
    val handleCenterY = potTop - height * 0.08f
    drawCircle(
        color = color,
        radius = width * 0.05f,
        center = Offset(handleCenterX, handleCenterY)
    )

    // Draw handles on sides
    val handleY = potTop + potHeight * 0.3f
    val handleSize = width * 0.08f

    // Left handle
    drawCircle(
        color = color,
        radius = handleSize,
        center = Offset(potLeft - handleSize * 0.3f, handleY),
        style = Stroke(width = strokeWidth * 0.8f)
    )

    // Right handle
    drawCircle(
        color = color,
        radius = handleSize,
        center = Offset(potRight + handleSize * 0.3f, handleY),
        style = Stroke(width = strokeWidth * 0.8f)
    )

    // Draw steam lines
    drawSteam(color, width, potTop)
}

private fun DrawScope.drawSteam(color: Color, width: Float, potTop: Float) {
    val steamStrokeWidth = width * 0.04f
    val steamColor = color.copy(alpha = 0.7f)

    // Steam line 1 (left)
    val steam1Path = Path().apply {
        moveTo(width * 0.35f, potTop - width * 0.15f)
        quadraticBezierTo(
            width * 0.30f, potTop - width * 0.25f,
            width * 0.35f, potTop - width * 0.35f
        )
    }
    drawPath(
        path = steam1Path,
        color = steamColor,
        style = Stroke(width = steamStrokeWidth, cap = StrokeCap.Round)
    )

    // Steam line 2 (center)
    val steam2Path = Path().apply {
        moveTo(width * 0.5f, potTop - width * 0.15f)
        quadraticBezierTo(
            width * 0.55f, potTop - width * 0.28f,
            width * 0.5f, potTop - width * 0.40f
        )
    }
    drawPath(
        path = steam2Path,
        color = steamColor,
        style = Stroke(width = steamStrokeWidth, cap = StrokeCap.Round)
    )

    // Steam line 3 (right)
    val steam3Path = Path().apply {
        moveTo(width * 0.65f, potTop - width * 0.15f)
        quadraticBezierTo(
            width * 0.70f, potTop - width * 0.25f,
            width * 0.65f, potTop - width * 0.35f
        )
    }
    drawPath(
        path = steam3Path,
        color = steamColor,
        style = Stroke(width = steamStrokeWidth, cap = StrokeCap.Round)
    )
}

package com.ost.application.ui.component

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.toPath
import kotlinx.coroutines.launch

// === КОНСТАНТЫ РАЗМЕРА ===
private const val SCALE_HUGE = 2.0f
private const val SCALE_LARGE = 1.8f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
enum class ExpressiveShapeType(val visualScale: Float) {
    CIRCLE(SCALE_LARGE),
    SQUARE(SCALE_LARGE),
    ARCH(SCALE_LARGE),
    OVAL(SCALE_LARGE),
    PILL(SCALE_LARGE),
    COOKIE_4(SCALE_HUGE),
    COOKIE_9(SCALE_HUGE),
    CLOVER_4(SCALE_HUGE),
    CLOVER_8(SCALE_HUGE)
}

@Composable
fun ExpressiveShapeBackground(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    iconSize: Dp,
    forcedShape: ExpressiveShapeType? = null,
    onClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val morphProgress = remember { Animatable(0f) }

    val shapeState = remember {
        ShapeState().apply {
            val startT = forcedShape ?: ExpressiveShapeType.entries.random()
            val endT = forcedShape ?: ExpressiveShapeType.entries.random()

            startPolygon = getM3Shape(startT)
            endPolygon = getM3Shape(endT)
            startScale = startT.visualScale
            endScale = endT.visualScale
            type = startT

            morph = Morph(startPolygon, endPolygon)
        }
    }

    LaunchedEffect(forcedShape) {
        if (forcedShape != null && forcedShape != shapeState.type) {
            shapeState.startPolygon = shapeState.endPolygon
            shapeState.startScale = shapeState.endScale
            shapeState.endPolygon = getM3Shape(forcedShape)
            shapeState.endScale = forcedShape.visualScale
            shapeState.type = forcedShape
            shapeState.morph = Morph(shapeState.startPolygon, shapeState.endPolygon)

            morphProgress.snapTo(0f)
            morphProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val path = remember { Path() }
    val transformMatrix = remember { Matrix() }
    val pathBounds = remember { RectF() }

    Box(
        modifier = modifier
            .size(iconSize)
            .clickable(
                enabled = forcedShape == null,
                interactionSource = interactionSource,
                indication = null
            ) {
                shapeState.current = shapeState.target
                var newType = ExpressiveShapeType.entries.random()
                while (newType == shapeState.type) {
                    newType = ExpressiveShapeType.entries.random()
                }

                shapeState.startPolygon = shapeState.endPolygon
                shapeState.startScale = shapeState.endScale
                shapeState.endPolygon = getM3Shape(newType)
                shapeState.endScale = newType.visualScale
                shapeState.type = newType
                shapeState.morph = Morph(shapeState.startPolygon, shapeState.endPolygon)

                scope.launch {
                    morphProgress.snapTo(0f)
                    morphProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
                    )
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val drawSize = this.size
            val radius = drawSize.minDimension / 2f

            if (radius > 0) {
                path.reset()
                transformMatrix.reset()

                val currentProgress = morphProgress.value
                val currentVisualScale = lerp(shapeState.startScale, shapeState.endScale, currentProgress)

                shapeState.morph.toPath(progress = currentProgress, path = path)

                path.computeBounds(pathBounds, true)
                val centerX = pathBounds.centerX()
                val centerY = pathBounds.centerY()
                transformMatrix.postTranslate(-centerX, -centerY)

                val finalScale = radius * currentVisualScale
                transformMatrix.postScale(finalScale, finalScale)

                transformMatrix.postTranslate(drawSize.width / 2f, drawSize.height / 2f)

                path.transform(transformMatrix)
                drawPath(path = path.asComposePath(), color = color)
            }
        }
    }
}

private class ShapeState {
    var type: ExpressiveShapeType = ExpressiveShapeType.CIRCLE
    var current: ExpressiveShapeType = ExpressiveShapeType.CIRCLE
    var target: ExpressiveShapeType = ExpressiveShapeType.CIRCLE

    var startPolygon: RoundedPolygon = RoundedPolygon.circle(4)
    var endPolygon: RoundedPolygon = RoundedPolygon.circle(4)
    var startScale: Float = 1f
    var endScale: Float = 1f

    var morph: Morph = Morph(startPolygon, endPolygon)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun getM3Shape(type: ExpressiveShapeType): RoundedPolygon {
    return when (type) {
        ExpressiveShapeType.CIRCLE -> MaterialShapes.Circle
        ExpressiveShapeType.SQUARE -> MaterialShapes.Square
        ExpressiveShapeType.ARCH -> MaterialShapes.Arch
        ExpressiveShapeType.OVAL -> MaterialShapes.Oval
        ExpressiveShapeType.PILL -> MaterialShapes.Pill
        ExpressiveShapeType.COOKIE_4 -> MaterialShapes.Cookie4Sided
        ExpressiveShapeType.COOKIE_9 -> MaterialShapes.Cookie9Sided
        ExpressiveShapeType.CLOVER_4 -> MaterialShapes.Clover4Leaf
        ExpressiveShapeType.CLOVER_8 -> MaterialShapes.Clover8Leaf
    }
}
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.ost.application.minigames.components
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.toPath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
class MorphPolygonShape(
    private val morph: Morph,
    private val percentage: Float
) : Shape {
    private val matrix = Matrix()
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        matrix.reset()
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f)
        val path = morph.toPath(progress = percentage)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

package com.ost.application.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun WavyDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(
        modifier = modifier
            .width(100.dp)
            .height(5.dp)
    ) {
        val path = Path()
        val waveLength = 60f
        val waveHeight = size.height
        val halfWaveHeight = waveHeight / 2

        val numWaves = (size.width / waveLength).toInt()

        if (numWaves == 0) return@Canvas

        val totalWavesWidth = numWaves * waveLength

        val startX = (size.width - totalWavesWidth) / 2

        path.moveTo(startX, halfWaveHeight)

        repeat(numWaves) {
            path.relativeQuadraticBezierTo(
                dx1 = waveLength / 4,
                dy1 = -waveHeight,
                dx2 = waveLength / 2,
                dy2 = 0f
            )
            path.relativeQuadraticBezierTo(
                dx1 = waveLength / 4,
                dy1 = waveHeight,
                dx2 = waveLength / 2,
                dy2 = 0f
            )
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)

package com.ost.application.ui.activity.welcome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ost.application.R
import com.ost.application.ui.activity.setup.SetupActivity
import com.ost.application.ui.component.ExpressiveShapeBackground
import com.ost.application.ui.component.ExpressiveShapeType
import com.ost.application.ui.component.LanguagePickerDialog
import com.ost.application.ui.theme.OSTToolsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.xmlpull.v1.XmlPullParser
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OSTToolsTheme {
                WelcomeScreen(
                    onGetStartedClick = {
                        val intent = Intent(this, SetupActivity::class.java)
                        startActivity(intent)
                        finish()
                    },
                    onLanguageSelected = { locale ->
                        LocaleHelper.setLocale(locale)
                    }
                )
            }
        }
    }
}

@Stable
class Particle(
    initialX: Float,
    initialY: Float,
    var vx: Float,
    var vy: Float,
    initialShape: ExpressiveShapeType,
    val color: Color,
    val sizeDp: Dp,
    var vRotation: Float
) {
    var x by mutableFloatStateOf(initialX)
    var y by mutableFloatStateOf(initialY)
    var shape by mutableStateOf(initialShape)
    var rotation by mutableFloatStateOf(0f)
    var isDragging by mutableStateOf(false)
}

private fun getRandomDifferentShape(currentShape: ExpressiveShapeType): ExpressiveShapeType {
    var newShape = ExpressiveShapeType.entries.random()
    while (newShape == currentShape) {
        newShape = ExpressiveShapeType.entries.random()
    }
    return newShape
}

@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit,
    onLanguageSelected: (Locale?) -> Unit
) {
    val context = LocalContext.current
    val showLanguageDialog = remember { mutableStateOf(false) }
    val supportedLocales = remember { parseSupportedLocales(context) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val availableColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.inversePrimary
    )

    val particles = remember { mutableStateListOf<Particle>() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                val velocityTracker = VelocityTracker()
                var draggedParticle: Particle? = null

                detectDragGestures(
                    onDragStart = { offset ->
                        val firstP = particles.firstOrNull() ?: return@detectDragGestures
                        val radiusPx = with(density) { firstP.sizeDp.toPx() / 2f }

                        draggedParticle = particles.find { p ->
                            val centerX = p.x + radiusPx
                            val centerY = p.y + radiusPx
                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            (dx * dx + dy * dy) < (radiusPx * radiusPx)
                        }

                        draggedParticle?.let { p ->
                            p.isDragging = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            velocityTracker.resetTracking()
                        }
                    },
                    onDragEnd = {
                        draggedParticle?.let { p ->
                            p.isDragging = false
                            val velocity = velocityTracker.calculateVelocity()
                            p.vx = (velocity.x / 60f).coerceIn(-25f, 25f)
                            p.vy = (velocity.y / 60f).coerceIn(-25f, 25f)
                        }
                        draggedParticle = null
                    },
                    onDragCancel = {
                        draggedParticle?.isDragging = false
                        draggedParticle = null
                    },
                    onDrag = { change, dragAmount ->
                        draggedParticle?.let { p ->
                            change.consume()
                            velocityTracker.addPointerInputChange(change)
                            p.x += dragAmount.x
                            p.y += dragAmount.y
                            p.vx = 0f
                            p.vy = 0f
                        }
                    }
                )
            }
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        val targetSizePx = screenWidthPx / 3f
        val targetSizeDp = with(density) { targetSizePx.toDp() }
        val radius = targetSizePx / 2f

        LaunchedEffect(screenWidthPx, screenHeightPx) {
            if (particles.isEmpty() && screenWidthPx > 0 && screenHeightPx > 0) {
                repeat(5) {
                    val startX = Random.nextFloat() * (screenWidthPx - targetSizePx)
                    val startY = Random.nextFloat() * (screenHeightPx - targetSizePx)

                    val speedBase = 3f
                    var vx = (Random.nextFloat() * 2 - 1) * speedBase
                    if (kotlin.math.abs(vx) < 0.5f) vx = if (vx < 0) -1f else 1f
                    var vy = (Random.nextFloat() * 2 - 1) * speedBase
                    if (kotlin.math.abs(vy) < 0.5f) vy = if (vy < 0) -1f else 1f

                    particles.add(
                        Particle(
                            initialX = startX,
                            initialY = startY,
                            vx = vx,
                            vy = vy,
                            initialShape = ExpressiveShapeType.entries.random(),
                            color = availableColors.random(),
                            sizeDp = targetSizeDp,
                            vRotation = (Random.nextFloat() * 2 - 1) * 0.5f
                        )
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            while (isActive) {
                withFrameNanos { _ ->

                    particles.forEach { p ->
                        if (!p.isDragging) {
                            var nextX = p.x + p.vx
                            var nextY = p.y + p.vy

                            p.rotation += p.vRotation

                            if (nextX <= 0f) { nextX = 0f; p.vx *= -1 }
                            if (nextX + targetSizePx >= screenWidthPx) { nextX =
                                screenWidthPx - targetSizePx; p.vx *= -1 }
                            if (nextY <= 0f) { nextY = 0f; p.vy *= -1 }
                            if (nextY + targetSizePx >= screenHeightPx) { nextY =
                                screenHeightPx - targetSizePx; p.vy *= -1 }

                            p.x = nextX
                            p.y = nextY
                        }
                    }

                    for (i in particles.indices) {
                        for (j in i + 1 until particles.size) {
                            val p1 = particles[i]
                            val p2 = particles[j]

                            val dx = (p2.x + radius) - (p1.x + radius)
                            val dy = (p2.y + radius) - (p1.y + radius)
                            val distSq = dx * dx + dy * dy
                            val minDistSq = targetSizePx * targetSizePx

                            if (distSq < minDistSq && distSq > 0.001f) {
                                val dist = sqrt(distSq)
                                val overlap = targetSizePx - dist
                                val nx = dx / dist
                                val ny = dy / dist

                                if (p1.isDragging && !p2.isDragging) {
                                    p2.x += nx * overlap
                                    p2.y += ny * overlap
                                    p2.vx += nx * 2f
                                    p2.vy += ny * 2f
                                    if (kotlin.math.abs(p2.vx) > 5f) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                } else if (!p1.isDragging && p2.isDragging) {
                                    p1.x -= nx * overlap
                                    p1.y -= ny * overlap
                                    p1.vx -= nx * 2f
                                    p1.vy -= ny * 2f
                                    if (kotlin.math.abs(p1.vx) > 5f) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                } else {
                                    val moveX = nx * overlap * 0.5f
                                    val moveY = ny * overlap * 0.5f
                                    p1.x -= moveX
                                    p1.y -= moveY
                                    p2.x += moveX
                                    p2.y += moveY

                                    val rvx = p2.vx - p1.vx
                                    val rvy = p2.vy - p1.vy
                                    val vn = rvx * nx + rvy * ny

                                    if (vn < 0) {
                                        val impulse = -vn
                                        p1.vx -= impulse * nx
                                        p1.vy -= impulse * ny
                                        p2.vx += impulse * nx
                                        p2.vy += impulse * ny

                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        p1.shape = getRandomDifferentShape(p1.shape)
                                        p2.shape = getRandomDifferentShape(p2.shape)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        particles.forEach { particle ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .offset { IntOffset(particle.x.roundToInt(), particle.y.roundToInt()) }
                    .graphicsLayer {
                        rotationZ = particle.rotation
                        scaleX = if (particle.isDragging) 1.1f else 1f
                        scaleY = if (particle.isDragging) 1.1f else 1f
                    }
            ) {
                ExpressiveShapeBackground(
                    iconSize = particle.sizeDp,
                    color = particle.color,
                    forcedShape = particle.shape
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Spacer(modifier = Modifier
                .padding(top = 32.dp)
                .size(32.dp))

            Spacer(modifier = Modifier.weight(1f))

            val headerFontFamily = remember {
                FontFamily(
                    Font(
                        resId = R.font.google_sans_flex,
                        variationSettings = FontVariation.Settings(
                            FontVariation.weight(500),
                            FontVariation.width(110f)
                        )
                    )
                )
            }
            Text(
                text = stringResource(R.string.hi_there),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = headerFontFamily,
                    fontSize = 56.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            val currentLocale = LocaleHelper.getCurrentLocale()
            val langName = currentLocale.displayLanguage.replaceFirstChar { it.titlecase() }

            FilledTonalButton(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                onClick = { showLanguageDialog.value = true }
            ) {
                Icon(painterResource(R.drawable.ic_public_24dp), null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = langName)
            }

            Spacer(modifier = Modifier.height(48.dp))

            MorphingStartButton(
                onClick = onGetStartedClick,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        if (showLanguageDialog.value) {
            LanguagePickerDialog(
                supportedLocales = supportedLocales,
                selectedLocale = LocaleHelper.getCurrentLocale(),
                onLanguageSelected = {
                    onLanguageSelected(it)
                    showLanguageDialog.value = false
                },
                onConfirm = { showLanguageDialog.value = false },
                onDismiss = { showLanguageDialog.value = false }
            )
        }
    }
}

fun parseSupportedLocales(context: Context): List<Locale> {
    val locales = mutableListOf<Locale>()
    try {
        val parser = context.resources.getXml(R.xml.locales_config)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                val langTag = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                if (langTag != null) {
                    locales.add(Locale.forLanguageTag(langTag))
                }
            }
            eventType = parser.next()
        }
    } catch (e: Exception) {
        Log.e("LocaleParser", "Error parsing locales_config", e)
        return listOf(Locale.ENGLISH)
    }
    val current = LocaleHelper.getCurrentLocale()
    return locales.sortedWith(compareByDescending<Locale> { it.language == current.language }
        .thenByDescending { it.language == "en" })
}

@ExperimentalTextApi
@Composable
fun MorphingStartButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            delay(600)
            isSuccess = false
        }
    }
    val targetWeight = when {
        isSuccess -> 1000f
        isPressed -> 100f
        else -> 700f
    }
    val animatedWeight by animateFloatAsState(
        targetValue = targetWeight,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "fontWeight"
    )
    val targetWidth = if (isSuccess) 150f else 100f
    val animatedWidth by animateFloatAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "fontWidth"
    )
    val targetCorner = if (isPressed) 16.dp else 32.dp
    val animatedCorner by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = tween(durationMillis = 200),
        label = "corner"
    )

    val flexFontFamily = remember(animatedWeight, animatedWidth) {
        FontFamily(
            Font(
                resId = R.font.google_sans_flex,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(animatedWeight.toInt().coerceIn(1, 1000)),
                    FontVariation.width(animatedWidth)
                )
            )
        )
    }

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isSuccess = true
            onClick()
        },
        interactionSource = interactionSource,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(animatedCorner),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = stringResource(R.string.lets_go).uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = flexFontFamily,
                fontSize = 24.sp
            )
        )
    }
}
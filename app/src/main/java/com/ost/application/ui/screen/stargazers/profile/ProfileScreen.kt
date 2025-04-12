package com.ost.application.ui.screen.stargazers.profile // ЗАМЕНИ НА СВОЙ ПАКЕТ

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ost.application.R
import com.ost.application.data.model.Stargazer

// --- ЗАГЛУШКА ТЕМЫ ---
@Composable
fun OSTToolsTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreenWithSharedZoom(
    stargazer: Stargazer,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isZoomed by remember { mutableStateOf(false) }
    val avatarKey = "avatar-${stargazer.id}"
    val context = LocalContext.current // Контекст нужен для R

    // Обработка системной кнопки "Назад" для закрытия зума
    BackHandler(enabled = isZoomed) {
        isZoomed = false
    }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        // Определяем эффект размытия (или null, если API < 31)
        val blurEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            remember { RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP).asComposeRenderEffect() }
        } else {
            null // Фоллбэк для старых API
        }

        // Определяем затемнение для фона (используем всегда, но поверх размытия на новых API)
        Color.Black.copy(alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.5f else 0.85f)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                // TopAppBar теперь будет затемняться и размываться вместе с контентом
                TopAppBar(
                    title = { Text(stargazer.getDisplayName()) },
                    navigationIcon = {
                        IconButton(onClick = { if (isZoomed) isZoomed = false else onFinish() }) {
                            Icon(painterResource(R.drawable.ic_arrow_back_24dp), contentDescription = "Back")
                        }
                    },
                    // Добавляем graphicsLayer для анимации прозрачности и эффектов
                    modifier = Modifier.graphicsLayer {
                        // Анимируем прозрачность TopAppBar при зуме
                        alpha = if (isZoomed) 0f else 1f
                        // Применяем размытие и затемнение, если зум активен
                        renderEffect = if (isZoomed) blurEffect else null
                    }
                )
            }
        ) { innerPadding ->

            AnimatedContent(
                targetState = isZoomed,
                modifier = Modifier.padding(innerPadding), // Применяем паддинг сюда
                label = "profileZoomAnimation",
                transitionSpec = {
                    // Переход для AnimatedContent. sharedElement сам сделает основную магию.
                    fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(animationSpec = tween(300)) using
                            SizeTransform(clip = true) { initialSize, targetSize -> // clip = true ВАЖНО для формы
                                tween(350, easing = FastOutSlowInEasing)
                            }
                }
            ) { targetStateIsZoomed ->

                if (targetStateIsZoomed) {
                    // --- СОСТОЯНИЕ: ЗУМ ---
                    Box(
                        modifier = Modifier
                            .fillMaxSize() // Полупрозрачный фон оверлея
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { isZoomed = false },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(stargazer.avatar_url)
                                .crossfade(false)
                                .placeholder(R.drawable.ic_account_circle_24dp)
                                .error(R.drawable.ic_account_circle_24dp)
                                .build(),
                            contentDescription = "Zoomed Avatar",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 32.dp)
                                // --- sharedElement для БОЛЬШОЙ аватарки ---
                                .sharedElement(
                                    rememberSharedContentState(key = avatarKey),
                                    animatedVisibilityScope = this@AnimatedContent, // <<<--- ПРАВИЛЬНЫЙ СКОУП
                                    boundsTransform = { _, _ -> tween(350, easing = FastOutSlowInEasing) },
                                    clipInOverlayDuringTransition = OverlayClip(CircleShape)
                                )
                        )
                    }
                } else {
                    // --- СОСТОЯНИЕ: ОБЫЧНЫЙ ПРОФИЛЬ ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                            // --- Применяем размытие к фону Column ---
                            .graphicsLayer {
                                // Анимация размытия при переходе в зум
                                renderEffect = if (targetStateIsZoomed) blurEffect else null // Используем targetState
                            }
                        ,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(stargazer.avatar_url)
                                .crossfade(false)
                                .placeholder(R.drawable.ic_account_circle_24dp)
                                .error(R.drawable.ic_account_circle_24dp)
                                .build(),
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                // --- sharedElement для МАЛЕНЬКОЙ аватарки ---
                                .sharedElement(
                                    rememberSharedContentState(key = avatarKey),
                                    animatedVisibilityScope = this@AnimatedContent, // <<<--- ПРАВИЛЬНЫЙ СКОУП
                                    boundsTransform = { _, _ -> tween(350, easing = FastOutSlowInEasing) },
                                    clipInOverlayDuringTransition = OverlayClip(CircleShape)
                                )
                                .clickable { isZoomed = true }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Остальной контент профиля ---
                        // Оставляем его видимым, он будет под размытием
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stargazer.getDisplayName(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(stargazer.html_url, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { /* openUrl */ })
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                SocialIconButton(icon = painterResource(id = R.drawable.about_page_github), contentDescription = "GitHub") {}
                                stargazer.twitter_username?.let { SocialIconButton(icon = painterResource(id = R.drawable.x_logo), contentDescription = "X") {} }
                                stargazer.email?.let { SocialIconButton(icon = painterResource(R.drawable.ic_mail_24dp), contentDescription = "Mail") {} }
                                stargazer.blog?.let { if(it.startsWith("http")) SocialIconButton(icon = painterResource(R.drawable.ic_internet_24dp), contentDescription = "Website") {} }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                DetailItem(icon = painterResource(R.drawable.ic_location_on_24dp), text = stargazer.location)
                                DetailItem(icon = painterResource(R.drawable.ic_domain_24dp), text = stargazer.company)
                                DetailItem(icon = painterResource(R.drawable.ic_mail_24dp), text = stargazer.email)
                                DetailItem(icon = painterResource(R.drawable.ic_info_24dp), text = stargazer.bio)
                            }
                        }
                    } // Конец Column обычного профиля
                } // Конец else
            } // Конец AnimatedContent
        } // Конец Scaffold
    } // Конец SharedTransitionLayout
}


// --- Вспомогательные Composable с твоими иконками ---
@Composable
private fun SocialIconButton(
    icon: Any, // Оставляем Any для поддержки Painter и ImageVector
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        when (icon) {
            is ImageVector -> Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
            is Painter -> Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
            else -> Icon(painterResource(R.drawable.ic_error_24dp), contentDescription = "Error", modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun DetailItem(
    icon: Any, // Any для поддержки Painter и ImageVector
    text: String?,
    modifier: Modifier = Modifier
) {
    if (!text.isNullOrBlank()) {
        Row(
            modifier = modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconModifier = Modifier.size(20.dp)
            val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
            when(icon) {
                is ImageVector -> Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = iconModifier)
                is Painter -> Icon(painter = icon, contentDescription = null, tint = iconTint, modifier = iconModifier)
                else -> Spacer(modifier = iconModifier)
            }

            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
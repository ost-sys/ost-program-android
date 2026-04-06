package com.ost.application.ui.screen.stargazers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.ost.application.R
import com.ost.application.data.model.GitHubUser
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import com.ost.application.util.WavyDivider
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : ComponentActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val username = intent.getStringExtra("EXTRA_USERNAME") ?: return finish()
        val token = intent.getStringExtra("EXTRA_TOKEN") ?: ""

        setContent {
            OSTToolsTheme {
                val user by viewModel.user.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val error by viewModel.error.collectAsState()
                val qrCodeBitmap by viewModel.qrCodeBitmap.collectAsState()
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    viewModel.loadProfile(username, token)
                }

                SharedTransitionLayout {
                    var showQrCode by remember { mutableStateOf(false) }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("") },
                                navigationIcon = {
                                    IconButton(onClick = { finish() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                }
                            )
                        },
                        floatingActionButton = {
                            if (user != null && qrCodeBitmap != null) {
                                AnimatedVisibility(
                                    visible = !showQrCode,
                                    enter = fadeIn(animationSpec = tween(400)),
                                    exit = fadeOut(animationSpec = tween(400))
                                ) {
                                    FloatingActionButton(
                                        onClick = { showQrCode = true },
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.sharedBounds(
                                            sharedContentState = rememberSharedContentState(key = "qr_transition_box"),
                                            animatedVisibilityScope = this@AnimatedVisibility,
                                            boundsTransform = { _, _ -> tween(400) }
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_share_24dp),
                                            contentDescription = "Show QR"
                                        )
                                    }
                                }
                            }
                        },
                        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            } else if (error != null) {
                                Text(
                                    text = error ?: "Unknown error",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else if (user != null) {
                                ProfileContent(user = user!!)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showQrCode,
                        enter = fadeIn(animationSpec = tween(400)),
                        exit = fadeOut(animationSpec = tween(400))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showQrCode = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = "qr_transition_box"),
                                        animatedVisibilityScope = this@AnimatedVisibility,
                                        boundsTransform = { _, _ -> tween(400) }
                                    )
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {}
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                qrCodeBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "QR Code",
                                        modifier = Modifier.size(250.dp)
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = stringResource(id = R.string.scan_qr_code_m, user?.name ?: user?.login ?: ""),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        FilledTonalButton(
                                            onClick = { shareQrCode(context, bmp, isQuickShare = false) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(56.dp),
                                            shape = RoundedCornerShape(
                                                topStart = 24.dp,
                                                topEnd = 4.dp,
                                                bottomEnd = 4.dp,
                                                bottomStart = 24.dp
                                            )
                                        ) {
                                            Text(stringResource(R.string.share))
                                        }

                                        Spacer(modifier = Modifier.width(2.dp))

                                        ElevatedButton(
                                            onClick = { shareQrCode(context, bmp, isQuickShare = true) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(56.dp),
                                            shape = RoundedCornerShape(
                                                topStart = 4.dp,
                                                topEnd = 24.dp,
                                                bottomStart = 4.dp,
                                                bottomEnd = 24.dp
                                            )
                                        ) {
                                            Text(stringResource(R.string.quick_share))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun shareQrCode(context: Context, bitmap: Bitmap, isQuickShare: Boolean) {
        try {
            val file = File(context.cacheDir, "qr_code_shared.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (isQuickShare) {
                intent.setPackage("com.google.android.gms")
                intent.component = ComponentName("com.google.android.gms", "com.google.android.gms.nearby.sharing.ShareSheetActivity")
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    context.startActivity(Intent.createChooser(intent, "Share"))
                }
            } else {
                context.startActivity(Intent.createChooser(intent, "Share"))
            }
        } catch (e: Exception) {
        }
    }
}

data class ActionButton(
    val iconRes: Int,
    val onClick: () -> Unit
)

@Composable
fun MorphingButtonGroup(buttons: List<ActionButton>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(bottom = 32.dp)
    ) {
        buttons.forEachIndexed { index, btn ->
            val isFirst = index == 0
            val isLast = index == buttons.lastIndex

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val animSpec = tween<Int>(150)
            val topStartPercent by animateIntAsState(targetValue = if (isPressed) 50 else if (isFirst) 50 else 8, animationSpec = animSpec, label = "")
            val bottomStartPercent by animateIntAsState(targetValue = if (isPressed) 50 else if (isFirst) 50 else 8, animationSpec = animSpec, label = "")
            val topEndPercent by animateIntAsState(targetValue = if (isPressed) 50 else if (isLast) 50 else 8, animationSpec = animSpec, label = "")
            val bottomEndPercent by animateIntAsState(targetValue = if (isPressed) 50 else if (isLast) 50 else 8, animationSpec = animSpec, label = "")

            val shape = RoundedCornerShape(
                topStartPercent = topStartPercent,
                topEndPercent = topEndPercent,
                bottomEndPercent = bottomEndPercent,
                bottomStartPercent = bottomStartPercent
            )

            val backgroundColor by animateColorAsState(
                targetValue = if (isPressed) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                animationSpec = tween(150), label = ""
            )

            val iconColor by animateColorAsState(
                targetValue = if (isPressed) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                animationSpec = tween(150), label = ""
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(shape)
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current
                    ) { btn.onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(btn.iconRes),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileContent(user: GitHubUser) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = user.name ?: user.login,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${user.login}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        val buttons = mutableListOf<ActionButton>()
        buttons.add(ActionButton(R.drawable.about_page_github) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(user.htmlUrl)))
        })
        if (!user.twitterUsername.isNullOrEmpty()) {
            buttons.add(ActionButton(R.drawable.x_logo) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/${user.twitterUsername}")))
            })
        }
        if (!user.email.isNullOrEmpty()) {
            buttons.add(ActionButton(R.drawable.ic_mail_24dp) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:${user.email}".toUri()
                }
                try { context.startActivity(intent) } catch (e: Exception) {}
            })
        }
        if (!user.blog.isNullOrEmpty()) {
            buttons.add(ActionButton(R.drawable.ic_internet_24dp) {
                val url = if (user.blog.startsWith("http")) user.blog else "https://${user.blog}"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            })
        }

        if (buttons.isNotEmpty()) {
            MorphingButtonGroup(buttons = buttons)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            val details = mutableListOf<@Composable (CardPosition) -> Unit>()

            if (!user.company.isNullOrEmpty()) {
                details.add { position ->
                    CustomCardItem(
                        title = stringResource(R.string.company),
                        summary = user.company,
                        icon = R.drawable.ic_apartment_24dp,
                        position = position
                    )
                }
            }

            if (!user.location.isNullOrEmpty()) {
                details.add { position ->
                    CustomCardItem(
                        title = stringResource(R.string.location),
                        summary = user.location,
                        icon = R.drawable.ic_location_on_24dp,
                        position = position
                    )
                }
            }

            details.forEachIndexed { index, composable ->
                val position = when {
                    details.size == 1 -> CardPosition.SINGLE
                    index == 0 -> CardPosition.TOP
                    index == details.lastIndex -> CardPosition.BOTTOM
                    else -> CardPosition.MIDDLE
                }
                composable(position)
            }

            if (!user.bio.isNullOrEmpty()) {
                if (details.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        WavyDivider()
                    }
                }

                CustomCardItem(
                    title = stringResource(R.string.description),
                    summary = user.bio,
                    icon = R.drawable.ic_info_24dp,
                    position = CardPosition.SINGLE
                )
            }
        }
    }
}
package com.ost.application.ui.screen.info

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ost.application.R
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.utils.CustomCardItem
import com.ost.application.utils.SectionTitle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun InfoScreen(
    modifier: Modifier = Modifier,
    viewModel: InfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(key1 = viewModel.action) {
        viewModel.action.onEach { action ->
            when (action) {
                is InfoAction.LaunchUrl -> {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Log.e("InfoScreen", "Activity not found for URL: ${action.url}")
                    } catch (e: Exception) {
                        Log.e("InfoScreen", "Error launching URL: ${action.url}", e)
                    }
                }
                is InfoAction.ShowToast -> {
                    // context.toast(context.getString(action.messageResId))
                }
            }
        }.launchIn(this)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = viewModel::onAboutMeClick)
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(viewModel.avatarUrl)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .placeholder(R.drawable.ic_refresh_24dp)
                        .error(R.drawable.ic_error_24dp)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.click_to_see_developer_information),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp,)
                        .fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically)
                {
                    SocialLink(icon = R.drawable.about_page_youtube, onClick = viewModel::onYoutubeClick)
                    SocialLink(icon = R.drawable.about_page_telegram, onClick = viewModel::onTelegramClick)
                    SocialLink(icon = R.drawable.about_page_github, onClick = viewModel::onGithubClick)
                }
            }
        }

        item { SectionTitle(stringResource(R.string.video_tools)) }
        item {
            CustomCardItem(
                icon = R.drawable.ic_preview_24dp, iconPainter = null,
                title = stringResource(R.string.preview_tool),
                summary = "Adobe Photoshop 2024",
                status = true, onClick = viewModel::onPreviewToolClick
            )
        }
        item {
            CustomCardItem(
                icon = R.drawable.ic_screen_record_24dp, iconPainter = null,
                title = stringResource(R.string.video_recording_tool),
                summary = "OBS",
                status = true, onClick = viewModel::onRecorderToolClick
            )
        }
        item {
            CustomCardItem(
                icon = R.drawable.ic_view_in_ar_24dp, iconPainter = null,
                title = stringResource(R.string.vm),
                summary = "VMware Workstation 17 Pro",
                status = true, onClick = viewModel::onVmToolClick
            )
        }
        item {
            CustomCardItem(
                icon = R.drawable.ic_capture_24dp, iconPainter = null,
                title = stringResource(R.string.video_editing_tool),
                summary = "Sony Vegas 2020",
                status = true, onClick = viewModel::onVideoEditorClick
            )
        }

        item { SectionTitle(stringResource(R.string.current_devices)) }
        item {
            CustomCardItem(
                icon = R.drawable.ic_device_24dp, iconPainter = null,
                title = "Galaxy S21 FE 5G",
                summary = "SM-G990B",
                status = true, onClick = viewModel::onPhoneClick
            )
        }
        item {
            CustomCardItem(
                icon = R.drawable.ic_computer_24dp, iconPainter = null,
                title = stringResource(R.string.computer),
                summary = stringResource(R.string.information_pc),
                status = true, onClick = viewModel::onPcClick
            )
        }
         item {
             CustomCardItem(
                 icon = R.drawable.ic_watch_24dp, iconPainter = null,
                 title = "Galaxy Watch 4",
                 summary = stringResource(R.string.click_here_to_see_information_about_galaxy_watch4),
                 status = true, onClick = { viewModel::onWatchClick }
             )
         }
        item {
            CustomCardItem(
                icon = R.drawable.ic_galaxy_buds_24dp, iconPainter = null,
                title = "Galaxy Buds 2",
                summary = stringResource(R.string.information_headphones),
                status = true, onClick = viewModel::onHeadphonesClick
            )
        }

        item { SectionTitle(stringResource(R.string.second_devices)) }
        item {
            CustomCardItem(
                icon = R.drawable.ic_phone_iphone_24dp, iconPainter = null,
                title = "iPhone 8", summary = "iOS 16.7.10 • Apple A11 Bionic",
                status = true, onClick = viewModel::onSecondPhoneClick
            )
        }
    }

    PcInfoDialog(
        showDialog = uiState.showPcInfoDialog,
        onDismiss = viewModel::dismissPcDialog
    )
}

@Composable
fun SocialLink(icon: Int, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        Modifier.size(48.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.onPrimary,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
        )
    }
}

@Composable
fun PcInfoDialog(showDialog: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = showDialog,
        enter = scaleIn(animationSpec = tween(durationMillis = 300)) +
                fadeIn(animationSpec = tween(durationMillis = 200)),
        exit = scaleOut(animationSpec = tween(durationMillis = 300)) +
                fadeOut(animationSpec = tween(durationMillis = 200))
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.computer)) },
            text = {
                LazyColumn {
                    item { PcInfoContent() }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

@Composable
fun PcInfoContent() {
    Column {
        PcSpecItem("${stringResource(R.string.cpu)}:", "Intel Core i5-10400F")
        PcSpecItem("${stringResource(R.string.gpu)}:", "ASUS Strix GeForce GTX 1070")
        PcSpecItem("${stringResource(R.string.ram)}:", "x4 Lexar 8192 MB 2666 MHz")
        PcSpecItem("${stringResource(R.string.motherboard)}:", "ASUS Prime B560-PLUS")
        PcSpecItem("${stringResource(R.string.rom)}:", "HDD 1TB\nM2_2 SATA 256GB\nSATA SSD 240 GB")
    }
}

@Composable
fun PcSpecItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text(value)
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoScreenPreview() {
    OSTToolsTheme {
        InfoScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun PcInfoDialogPreview() {
    OSTToolsTheme {
        PcInfoDialog(showDialog = true, onDismiss = {})
    }
}
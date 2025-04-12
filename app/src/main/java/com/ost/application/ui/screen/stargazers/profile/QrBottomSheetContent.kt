package com.ost.application.ui.screen.stargazers.profile

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.imageLoader
import coil.request.ImageRequest
import com.ost.application.R
import com.ost.application.data.model.QrCodeHelper
import com.ost.application.data.model.SharingUtils.isSamsungQuickShareAvailable
import com.ost.application.data.model.Stargazer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun QrBottomSheetContent(
    stargazer: Stargazer,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val qrCodeSizeDp: Dp = 240.dp
    val qrCodeSizePx = with(density) { qrCodeSizeDp.roundToPx() }

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tempImageFile by remember { mutableStateOf<File?>(null) }
    var isGenerating by remember { mutableStateOf(true) }

    LaunchedEffect(stargazer.html_url, stargazer.avatar_url) {
        isGenerating = true
        qrBitmap = null

        val logoBitmap: Bitmap? = try {
            val request = ImageRequest.Builder(context)
                .data(stargazer.avatar_url)
                .allowHardware(false)
                .size(128, 128)
                .build()
            (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val generatedBitmap = withContext(Dispatchers.IO) {
            QrCodeHelper.generateQrCodeWithLogoBitmap(
                content = stargazer.html_url,
                size = qrCodeSizePx,
                logo = logoBitmap
            )
        }

        qrBitmap = generatedBitmap
        isGenerating = false
    }
    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        tempImageFile?.delete()
        tempImageFile = null
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data?.data != null) {
            val uri = result.data!!.data!!
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        qrBitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.image_saved_successfully), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.failed_to_save_image), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Save cancelled)", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stargazer.getDisplayName(),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.size(qrCodeSizeDp),
            contentAlignment = Alignment.Center
        ) {
            if (isGenerating) {
                CircularProgressIndicator()
            } else if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.scan_qr_code_m, stargazer.getDisplayName()),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(stringResource(R.string.error))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.scan_qr_code_m, stargazer.getDisplayName()),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        if (qrBitmap != null) {
                            try {
                                val cachePath = File(context.cacheDir, "qr_codes/")
                                cachePath.mkdirs()
                                val file = File(cachePath, "${stargazer.getDisplayName()}_qr_share_${System.currentTimeMillis()}.png")
                                FileOutputStream(file).use { out ->
                                    qrBitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                                tempImageFile = file

                                val contentUri: Uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )

                                val shareIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, contentUri)
                                    type = "image/png"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                withContext(Dispatchers.Main){
                                    shareLauncher.launch(Intent.createChooser(shareIntent, context.getString(R.string.share)))
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main){
                                    Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_SHORT).show()
                                }
                                tempImageFile?.delete()
                                tempImageFile = null
                            }
                        }
                    }
                },
                enabled = qrBitmap != null && !isGenerating
            ) {
                val shareText = if (context.isSamsungQuickShareAvailable()) {
                    stringResource(R.string.quick_share)
                } else {
                    stringResource(R.string.share)
                }
                Text(shareText)
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/png"
                        putExtra(Intent.EXTRA_TITLE, "${stargazer.getDisplayName()}_qrCode.png")
                    }
                    saveLauncher.launch(intent)
                },
                enabled = qrBitmap != null && !isGenerating
            ) {
                Text(stringResource(R.string.save_image))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
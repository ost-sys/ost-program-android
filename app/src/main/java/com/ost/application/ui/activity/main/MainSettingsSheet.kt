package com.ost.application.ui.activity.main
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.ost.application.R
import com.ost.application.core.settings.TemperatureUnit
import com.ost.application.ui.screen.settings.SettingsScreen
import com.ost.application.ui.screen.settings.SettingsUiState
import java.util.Locale
@Composable
fun SettingsSheetContent(
    state: SettingsUiState,
    onTotalDurationChange: (Float) -> Unit,
    onNoiseDurationChange: (Float) -> Unit,
    onBWNoiseDurationChange: (Float) -> Unit,
    onHorizontalDurationChange: (Float) -> Unit,
    onVerticalDurationChange: (Float) -> Unit,
    onGithubTokenChange: (String) -> Unit,
    onSaveGithubToken: () -> Unit,
    onClearGithubToken: () -> Unit,
    onAboutClick: () -> Unit,
    onCloseClick: () -> Unit,
    onLanguagePreferenceClick: () -> Unit,
    onLanguageSelected: (Locale?) -> Unit,
    onLanguageConfirm: () -> Unit,
    onLanguageDismiss: () -> Unit,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit,
    onDeveloperOptionsClick: () -> Unit = {},
    onDismissDeveloperOptionsDialog: () -> Unit = {},
    onShowLogcatClick: () -> Unit = {},
    onDismissLogcatDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            actions = {
                if (state.isDeveloperModeEnabled) {
                    IconButton(onClick = onDeveloperOptionsClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_terminal_24dp),
                            contentDescription = stringResource(R.string.developer_mode),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onAboutClick) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = stringResource(R.string.about_app)
                    )
                }
                IconButton(onClick = onCloseClick) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        SettingsScreen(
            state = state,
            onTotalDurationChange = onTotalDurationChange,
            onNoiseDurationChange = onNoiseDurationChange,
            onBWNoiseDurationChange = onBWNoiseDurationChange,
            onHorizontalDurationChange = onHorizontalDurationChange,
            onVerticalDurationChange = onVerticalDurationChange,
            onGithubTokenChange = onGithubTokenChange,
            onSaveGithubToken = onSaveGithubToken,
            onClearGithubToken = onClearGithubToken,
            onLanguagePreferenceClick = onLanguagePreferenceClick,
            onLanguageSelected = onLanguageSelected,
            onLanguageConfirm = onLanguageConfirm,
            onLanguageDismiss = onLanguageDismiss,
            onTemperatureUnitChange = onTemperatureUnitChange,
            onDeveloperOptionsClick = onDeveloperOptionsClick,
            onDismissDeveloperOptionsDialog = onDismissDeveloperOptionsDialog,
            onShowLogcatClick = onShowLogcatClick,
            onDismissLogcatDialog = onDismissLogcatDialog,
            modifier = modifier.fillMaxWidth()
        )
    }
}

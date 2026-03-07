package com.ost.application.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ost.application.R
import com.ost.application.ui.activity.welcome.LocaleHelper // Убедись, что этот импорт верный (где у тебя лежит LocaleHelper)
import java.util.Locale

@Composable
fun LanguagePickerDialog(
    supportedLocales: List<Locale>,
    selectedLocale: Locale?,
    onLanguageSelected: (Locale?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.language_dialog_title)) },
        text = {
            LazyColumn {
                item {
                    val systemLocale = LocaleHelper.getSystemLocale()
                    DialogLanguageRow(
                        text = stringResource(
                            R.string.system_default,
                            systemLocale.getDisplayName(systemLocale)
                        ),
                        selected = selectedLocale == null,
                        onClick = { onLanguageSelected(null) }
                    )
                }
                items(supportedLocales) { locale ->
                    DialogLanguageRow(
                        text = locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) },
                        selected = selectedLocale == locale,
                        onClick = { onLanguageSelected(locale) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DialogLanguageRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
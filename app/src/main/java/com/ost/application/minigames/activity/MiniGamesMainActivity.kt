@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package com.ost.application.minigames.activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ost.application.R
import com.ost.application.minigames.activity.games.MinesweeperGameActivity
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
class MiniGamesMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OSTToolsTheme {
                MiniGamesMainScreen(onNavigateBack = { finish() })
            }
        }
    }
}
@Composable
fun MiniGamesMainScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Mini Games")
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                        com.ost.application.util.BetaBadge()
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back_24dp),
                            stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            item {
                CustomCardItem(
                    title = "Minesweeper",
                    icon = R.drawable.ic_bomb_24dp,
                    onClick = {
                        val intent = Intent(context, MinesweeperGameActivity::class.java)
                        context.startActivity(intent)
                    },
                    position = CardPosition.TOP
                )
            }
            item {
                CustomCardItem(
                    title = "Tic-Tac-Toe",
                    icon = R.drawable.ic_bomb_24dp,
                    onClick = {
                    },
                    position = CardPosition.BOTTOM
                )
            }
        }
    }
}

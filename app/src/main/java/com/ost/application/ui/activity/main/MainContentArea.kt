@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.ost.application.ui.activity.main
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ost.application.ui.screen.applist.AppListScreen
import com.ost.application.ui.screen.batteryinfo.BatteryInfoScreen
import com.ost.application.ui.screen.converters.ConvertersScreen
import com.ost.application.ui.screen.cpu.CpuInfoScreen
import com.ost.application.ui.screen.deviceinfo.DeviceInfoScreen
import com.ost.application.ui.screen.display.DisplayInfoScreen
import com.ost.application.ui.screen.network.NetworkInfoScreen
import com.ost.application.ui.screen.powermenu.PowerMenuScreen
import com.ost.application.ui.screen.ram.RAMScreen
import com.ost.application.ui.screen.share.ShareScreen
import com.ost.application.ui.screen.stargazers.StargazersScreen
import com.ost.application.ui.screen.stargazers.StargazersViewModel
import com.ost.application.ui.screen.storage.StorageScreen
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ContentArea(
    selectedItemId: String?,
    stargazersViewModel: StargazersViewModel,
    modifier: Modifier = Modifier
) {
    when (selectedItemId) {
        "tools" -> ConvertersScreen(modifier = modifier)
        "power_menu" -> PowerMenuScreen(modifier = modifier)
        "share_files" -> ShareScreen(modifier = modifier)
        "app_list" -> AppListScreen()
        "stargazers" -> StargazersScreen(viewModel = stargazersViewModel)
        "about_device" -> DeviceInfoScreen(modifier = modifier)
        "cpu" -> CpuInfoScreen(modifier = modifier)
        "battery" -> BatteryInfoScreen(modifier = modifier)
        "display" -> DisplayInfoScreen(modifier = modifier)
        "network" -> NetworkInfoScreen(modifier = modifier)
        "storage" -> StorageScreen(modifier = modifier)
        "ram" -> RAMScreen(modifier = modifier)
        else -> {
            Column(
                modifier = modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Select an option from the bottom bar.")
            }
        }
    }
}

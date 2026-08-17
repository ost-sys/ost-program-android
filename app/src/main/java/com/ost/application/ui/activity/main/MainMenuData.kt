package com.ost.application.ui.activity.main
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Games
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.ost.application.R
sealed class MenuIcon {
    data class Vector(val imageVector: ImageVector) : MenuIcon()
    data class Res(@DrawableRes val resId: Int) : MenuIcon()
}
data class MenuItemData(
    val id: String,
    @StringRes val titleResId: Int,
    val icon: MenuIcon
)
internal const val MORE_ITEM_ID = "more_button_id"

internal fun createMenuItems(isRooted: Boolean): List<MenuItemData> {
    return listOfNotNull(
        MenuItemData("power_menu", R.string.power_menu, MenuIcon.Res(R.drawable.ic_power_new_24dp)),
        MenuItemData("share_files", R.string.share, MenuIcon.Res(R.drawable.ic_share_24dp)),
        MenuItemData("stargazers", R.string.stargazers, MenuIcon.Res(R.drawable.ic_stars_24dp)),
        MenuItemData("app_list", R.string.apps_list, MenuIcon.Res(R.drawable.ic_apps_24dp)),
        MenuItemData("about_device", R.string.about_device, MenuIcon.Res(R.drawable.ic_device_24dp)),
        MenuItemData("cpu", R.string.cpu, MenuIcon.Res(R.drawable.ic_developer_board_24dp)),
        MenuItemData("battery", R.string.battery, MenuIcon.Res(R.drawable.ic_battery_full_24dp)),
        MenuItemData("display", R.string.display, MenuIcon.Res(R.drawable.ic_screen_24dp)),
        MenuItemData("network", R.string.network, MenuIcon.Res(R.drawable.ic_wifi_24dp)),
        MenuItemData("storage", R.string.rom, MenuIcon.Res(R.drawable.ic_storage_24dp)),
        MenuItemData("ram", R.string.ram, MenuIcon.Res(R.drawable.ic_memory_alt_24dp))
    )
}
@Composable
fun MenuItemIcon(
    icon: MenuIcon,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    when (icon) {
        is MenuIcon.Vector -> Icon(
            imageVector = icon.imageVector,
            contentDescription = contentDescription,
            modifier = modifier
        )
        is MenuIcon.Res -> Icon(
            painter = painterResource(icon.resId),
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}

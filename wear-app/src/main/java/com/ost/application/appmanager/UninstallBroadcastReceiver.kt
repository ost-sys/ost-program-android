package com.ost.application.appmanager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
class UninstallBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
            val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            if (!replacing) {
                val packageName = intent.data?.schemeSpecificPart
                Log.d("UNINSTALLER", "Package removed: $packageName")
                _uninstallEvent.value++
            }
        }
    }
    companion object {
        private val _uninstallEvent = MutableStateFlow(0)
        val uninstallEvent = _uninstallEvent.asStateFlow()
    }
}

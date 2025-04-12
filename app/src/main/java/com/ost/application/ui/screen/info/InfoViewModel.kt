package com.ost.application.ui.screen.info

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InfoUiState(
    val showPcInfoDialog: Boolean = false
)

sealed class InfoAction {
    data class LaunchUrl(val url: String) : InfoAction()
    data class ShowToast(val messageResId: Int) : InfoAction()
}

class InfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(InfoUiState())
    val uiState: StateFlow<InfoUiState> = _uiState.asStateFlow()

    private val _action = Channel<InfoAction>(Channel.BUFFERED)
    val action = _action.receiveAsFlow()

    val avatarUrl = "https://avatars.githubusercontent.com/u/66862161?v=4"

    fun onAboutMeClick() = launchUrl("https://t.me/ost_info")
    fun onPreviewToolClick() = launchUrl("https://rsload.net/repack/kpojiuk/23242-adobe-photoshop-repack-kpojiuk-crack.html")
    fun onRecorderToolClick() = launchUrl("https://obsproject.com/")
    fun onVmToolClick() = launchUrl("https://rsload.net/repack/kpojiuk/25218-vmware-workstation-pro-repack-kpojiuk.html")
    fun onVideoEditorClick() = launchUrl("https://rsload.net/soft/editor/10312-sony-vegas-pro.html")
    fun onPhoneClick() = launchUrl("https://www.samsung.com/en/smartphones/galaxy-s21-5g/specs/")
    fun onWatchClick() = launchUrl("https://www.samsung.com/us/app/watches/galaxy-watch4/")
    fun onHeadphonesClick() = launchUrl("https://www.samsung.com/us/app/mobile-audio/galaxy-buds2/")
    fun onSecondPhoneClick() = launchUrl("https://support.apple.com/en-us/111976")
    fun onYoutubeClick() = launchUrl("https://www.youtube.com/channel/UC6wNi6iQFVSnd-eJivuG3_Q")
    fun onTelegramClick() = launchUrl("https://t.me/ost_news5566")
    fun onGithubClick() = launchUrl("https://github.com/ost-sys/")

    fun onPcClick() {
        _uiState.update { it.copy(showPcInfoDialog = true) }
    }

    fun dismissPcDialog() {
        _uiState.update { it.copy(showPcInfoDialog = false) }
    }

    private fun launchUrl(url: String) {
        viewModelScope.launch {
            _action.send(InfoAction.LaunchUrl(url))
        }
    }
}
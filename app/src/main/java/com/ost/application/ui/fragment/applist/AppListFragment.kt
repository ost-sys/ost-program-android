package com.ost.application.ui.fragment.applist

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.SeslMenuItem
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.ost.application.MainActivity
import com.ost.application.R
import com.ost.application.data.AppsRepo
import com.ost.application.databinding.FragmentAppListBinding
import com.ost.application.ui.core.base.BaseFragment
import com.ost.application.ui.core.launchAndRepeatWithViewLifecycle
import com.ost.application.ui.core.toast
import com.topjohnwu.superuser.Shell
import dev.oneuiproject.oneui.delegates.AppBarAwareYTranslator
import dev.oneuiproject.oneui.delegates.AppPickerDelegate
import dev.oneuiproject.oneui.delegates.AppPickerOp
import dev.oneuiproject.oneui.delegates.ViewYTranslator
import dev.oneuiproject.oneui.ktx.clearBadge
import dev.oneuiproject.oneui.ktx.isSoftKeyboardShowing
import dev.oneuiproject.oneui.ktx.setBadge
import dev.oneuiproject.oneui.ktx.setEntries
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.startSearchMode
import kotlinx.coroutines.flow.collectLatest

class AppListFragment : BaseFragment(),
    ViewYTranslator by AppBarAwareYTranslator(),
    AppPickerOp by AppPickerDelegate() {

    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!
    private var toolbarLayout: ToolbarLayout? = null
    private lateinit var appsViewModel: AppListModel
    private lateinit var pickApkLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentAppListBinding.inflate(inflater, container, false).also {
        _binding = it
    }.root

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pickApkLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                uri.toString()
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    startActivity(intent)
                } catch (_: Exception) {
                    toast(getString(R.string.fail))
                }
            }
        }

        setupFabClickListener()

        toolbarLayout = (requireActivity() as MainActivity).drawerLayout
        binding.nsvNoItem.translateYWithAppBar(toolbarLayout!!.appBarLayout, viewLifecycleOwner)

        val appsRepo = AppsRepo(requireContext())
        val viewModelFactory = AppListModelFactory(appsRepo)
        appsViewModel = ViewModelProvider(this, viewModelFactory)[AppListModel::class.java]

        binding.appPickerView.apply {
            itemAnimator = null
            seslSetSmoothScrollEnabled(true)
            configure(
                onGetCurrentList = { appsViewModel.appPickerScreenStateFlow.value.appList },
                onItemClicked = { _, _, appLabel ->
                    val packageManager = requireContext().packageManager
                    val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    val appInfo = installedApps.find { packageManager.getApplicationLabel(it) == appLabel }
                    val packageName = appInfo?.packageName
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                },
                onItemActionClicked = { _, _, appLabel ->
                    val packageManager = requireContext().packageManager
                    val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    val appInfo = installedApps.find { packageManager.getApplicationLabel(it) == appLabel }
                    val packageName = appInfo?.packageName
                    if (Shell.cmd("su").exec().isSuccess) {
                        val alert = AlertDialog.Builder(requireActivity())
                            .setTitle(appLabel)
                            .setMessage(R.string.application_action_selection)
                            .setPositiveButton(R.string.uninstall) { dialog, which ->
                                Shell.cmd("pm uninstall $packageName").exec()
                            }
                            .setNegativeButton(R.string.disable) { dialog, which ->
                                Shell.cmd("pm uninstall $packageName").exec()
                            }
                            .create()
                        alert.show()
                    } else {
                        toast(getString(R.string.feature_unavailable_root))
                    }
                }
            )
        }

        showProgressBar(true)

        initSpinner()

        launchAndRepeatWithViewLifecycle {
            appsViewModel.appPickerScreenStateFlow.collectLatest {
                showProgressBar(it.isLoading)
                refreshAppList()
                menuProvider.showSystemItemTitle = getString(if (!it.showSystem) R.string.show_system_apps else R.string.hide_system_apps)
            }
        }
    }

    private fun setupFabClickListener() {
        binding.fab.apply {
            setOnClickListener {
                pickApkLauncher.launch(arrayOf("application/vnd.android.package-archive"))
            }
        }
    }

    private fun showProgressBar(show: Boolean) {
        binding.apppickerProgress.isVisible = show
    }

    private fun initSpinner() {
        binding.apppickerSpinner.setEntries(
            ListTypes.entries.map { getString(it.description) }
        ) { pos, _ ->
            pos?.let { setListType(ListTypes.entries[it].type) }
        }
    }

    private fun applyFilter(query: String?) {
        binding.appPickerView.setSearchFilter(query) { itemCount ->
            updateRecyclerViewVisibility(itemCount > 0)
        }
    }

    private fun updateRecyclerViewVisibility(visible: Boolean) {
        if (visible) {
            binding.nsvNoItem.isVisible = false
            binding.appPickerView.isVisible = true
        } else {
            binding.nsvNoItem.isVisible = true
            binding.appPickerView.isVisible = false
        }
    }

    private val menuProvider = object : MenuProvider {
        private var menu: Menu? = null
        private var mShowSystemItemBadge: Badge = Badge.NONE
        var showSystemItemTitle: String = ""

        override fun onPrepareMenu(menu: Menu) {
            menu.findItem(R.id.menu_apppicker_system)?.title = showSystemItemTitle
            menu.findItem(R.id.menu_apppicker_system)?.setVisible(true)
            menu.findItem(R.id.menu_apppicker_search)?.setVisible(true)
        }

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_main, menu)
            this.menu = menu
            menu.findItem(R.id.menu_apppicker_system)?.setBadge(mShowSystemItemBadge)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_apppicker_system -> {
                    (menuItem as SeslMenuItem).badgeText = null
                    appsViewModel.toggleShowSystem()
                    (menuItem as SeslMenuItem).clearBadge()
                    true
                }
                R.id.menu_apppicker_search -> {
                    toolbarLayout!!.apply {
                        startSearchMode(
                            onStart = {
                                it.queryHint = getString(R.string.search)
                                if (!isSoftKeyboardShowing) {
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.showSoftInput(it, 0)
                                }
                            },
                            onQuery = { query, _ ->
                                applyFilter(query)
                                true
                            },
                            onEnd = {

                            },
                            onBackBehavior = ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
                        )
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            requireActivity().addMenuProvider(
                menuProvider,
                viewLifecycleOwner,
                Lifecycle.State.STARTED
            )
        } else {
            requireActivity().removeMenuProvider(menuProvider)
        }
    }

    override fun getLayoutResId(): Int = R.layout.fragment_app_list

    override fun getIconResId(): Int = dev.oneuiproject.oneui.R.drawable.ic_oui_apps_outline

    override fun getTitle(): CharSequence = getString(R.string.app_list)
}

package com.ost.application

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ost.application.activity.settings.SettingsActivity
import com.ost.application.databinding.ActivityMainBinding
import com.ost.application.ui.core.base.FragmentInfo
import com.ost.application.ui.core.drawer.DrawerListAdapter
<<<<<<< Updated upstream
import com.ost.application.ui.fragment.CurrencyConverterFragment
=======
import com.ost.application.ui.core.util.appSettingOpen
import com.ost.application.ui.core.util.warningPermissionDialog
import com.ost.application.ui.fragment.ConvertersFragment
import com.ost.application.ui.fragment.converters.CurrencyConverterFragment
>>>>>>> Stashed changes
import com.ost.application.ui.fragment.InfoFragment
import com.ost.application.ui.fragment.PowerMenuFragment
import com.ost.application.ui.fragment.WatchDeviceFragment
import com.ost.application.ui.fragment.converters.TimeCalculatorFragment
import com.ost.application.ui.fragment.converters.TimeZoneConverterFragment
import com.ost.application.ui.fragment.applist.AppListFragment
import com.ost.application.ui.fragment.phoneinfo.BatteryInfoFragment
import com.ost.application.ui.fragment.phoneinfo.CPUInfoFragment
import com.ost.application.ui.fragment.phoneinfo.CameraInfoFragment
import com.ost.application.ui.fragment.phoneinfo.DefaultInfoFragment
import com.ost.application.ui.fragment.phoneinfo.DisplayInfoFragment
import com.ost.application.ui.fragment.phoneinfo.NetworkInfoFragment
import com.ost.application.ui.fragment.stargazerslist.StargazersListFragment
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.internal.UiThreadHandler.handler
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.layout.NavDrawerLayout
import dev.oneuiproject.oneui.utils.ActivityUtils
import dev.oneuiproject.oneui.widget.Toast
import java.util.Calendar

class MainActivity : AppCompatActivity(), DrawerListAdapter.DrawerListener {
    private lateinit var countdownRunnable: Runnable
    private var mBinding: ActivityMainBinding? = null
    private var mFragmentManager: FragmentManager? = null
    private val fragments: MutableList<Fragment?> = ArrayList()
    var adapter: DrawerListAdapter? = null
<<<<<<< Updated upstream
=======

    private val multiplePermissionId = 14
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= 33) {
        arrayListOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_PHONE_STATE,
        )
    } else {
        arrayListOf(
            Manifest.permission.READ_PHONE_STATE,
        )
    }
>>>>>>> Stashed changes

    @SuppressLint("ShowToast", "MissingInflatedId", "WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding!!.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.enableEdgeToEdge()
        }

        initFragmentList()
        initDrawer()
        initFragments()

        countdownRunnable = object : Runnable {
            override fun run() {
                val now = Calendar.getInstance()
                val year = now.get(Calendar.YEAR)

                if (now.get(Calendar.MONTH) == Calendar.JANUARY && now.get(Calendar.DAY_OF_MONTH) == 1) {
                    mBinding!!.drawerLayout.setTitle(getString(R.string.happy_new_year), getString(R.string.app_name))
                } else {
                    val nextYear = year + 1
                    val targetDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, nextYear)
                        set(Calendar.MONTH, Calendar.JANUARY)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val remainingTime = calculateRemainingTime(targetDate.timeInMillis)
                    mBinding!!.drawerLayout.expandedSubtitle = remainingTime
                }

                handler.postDelayed(this, 1000)
            }
        }

        handler.post(countdownRunnable)
    }

    private val offsetRunner: Runnable = object : Runnable {
        override fun run() {
            applyOffset()
            mBinding!!.drawerLayout.postDelayed(this, 10)
        }
    }

    private fun applyOffset() {
        val offset = mBinding!!.drawerLayout.drawerOffset
        adapter!!.setOffset(offset)
    }


    private fun initFragmentList() {
        fragments.add(ConvertersFragment())
        fragments.add(PowerMenuFragment())
        fragments.add(AppListFragment())
        fragments.add(StargazersListFragment())
        fragments.add(null)
        fragments.add(DefaultInfoFragment())
        fragments.add(CPUInfoFragment())
        fragments.add(BatteryInfoFragment())
        fragments.add(DisplayInfoFragment())
        fragments.add(NetworkInfoFragment())
        fragments.add(CameraInfoFragment())
        fragments.add(null)
        fragments.add(InfoFragment())
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initDrawer() {
        adapter = DrawerListAdapter(this, fragments, this)
        mBinding!!.drawerLayout.setHeaderButtonIcon(getDrawable(dev.oneuiproject.oneui.R.drawable.ic_oui_settings_outline))
        mBinding!!.drawerLayout.setHeaderButtonTooltip(getString(R.string.settings))
<<<<<<< Updated upstream
        mBinding!!.drawerLayout.setHeaderButtonOnClickListener { v ->
=======
        mBinding!!.drawerLayout.setHeaderButtonOnClickListener {
>>>>>>> Stashed changes
            ActivityUtils.startPopOverActivity(
                this,
                Intent(this, SettingsActivity::class.java),
                null,
                ActivityUtils.POP_OVER_POSITION_TOP or ActivityUtils.POP_OVER_POSITION_CENTER_HORIZONTAL
            )
        }

        mBinding!!.drawerListView.layoutManager = LinearLayoutManager(this)
        mBinding!!.drawerListView.adapter = adapter
        mBinding!!.drawerListView.itemAnimator = null
        mBinding!!.drawerListView.setHasFixedSize(true)
        mBinding!!.drawerListView.seslSetLastRoundedCorner(true)
        mBinding!!.drawerLayout.setNavRailContentMinSideMargin(10);
        mBinding!!.drawerLayout.setDrawerStateListener { state ->
            if (!mBinding!!.drawerLayout.isLargeScreenMode) return@setDrawerStateListener
            mBinding!!.drawerLayout.removeCallbacks(offsetRunner)
            when (state) {
                DrawerLayout.DrawerState.OPEN -> null
                DrawerLayout.DrawerState.CLOSE -> applyOffset()
                DrawerLayout.DrawerState.CLOSING, DrawerLayout.DrawerState.OPENING -> mBinding!!.drawerLayout.post(offsetRunner)
            }
        }

        val initialOffset = if (mBinding!!.drawerLayout.isLargeScreenMode) {
            mBinding!!.drawerLayout.drawerOffset
        } else {
            1f
        }
        adapter!!.setOffset(initialOffset)


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_check_root) {
            val result = Shell.cmd("su").exec()
            if (result.isSuccess) {
                Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.fail, Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }

    private fun initFragments() {
        mFragmentManager = supportFragmentManager
        val transaction = mFragmentManager!!.beginTransaction()
        for (fragment in fragments) {
            if (fragment != null) transaction.add(R.id.main_content, fragment)
        }
        transaction.commit()
        mFragmentManager!!.executePendingTransactions()

        onDrawerItemSelected(0)
    }

    override fun onDrawerItemSelected(position: Int): Boolean {
        val newFragment = fragments[position]
        val transaction = mFragmentManager!!.beginTransaction()
        for (fragment in mFragmentManager!!.fragments) {
            transaction.hide(fragment)
        }
        transaction.show(newFragment!!).commit()

        if (newFragment is FragmentInfo) {
            if (!(newFragment as FragmentInfo).isAppBarEnabled) {
                mBinding!!.drawerLayout.setExpanded(false, false)
                mBinding!!.drawerLayout.isExpandable = false
            } else {
                mBinding!!.drawerLayout.isExpandable = true
                mBinding!!.drawerLayout.setExpanded(false, false)
            }
            mBinding!!.drawerLayout.collapsedSubtitle = (newFragment as FragmentInfo).title
            val now = Calendar.getInstance()
            if (now.get(Calendar.MONTH) == Calendar.JANUARY && now.get(Calendar.DAY_OF_MONTH) == 1) {
                mBinding!!.drawerLayout.setTitle(getString(R.string.happy_new_year), getString(R.string.app_name))
            } else {
                mBinding!!.drawerLayout.setTitle(getString(R.string.time_left_until_the_new_year), getString(R.string.app_name))
            }
        }
        mBinding!!.drawerLayout.setDrawerOpen(false, true)

        return true
    }

    val drawerLayout: NavDrawerLayout
        get() = mBinding!!.drawerLayout

    override fun onStart() {
        super.onStart()
        checkMultiplePermission()
    }

    private fun checkMultiplePermission(): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionNeeded.add(permission)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionNeeded.toTypedArray(),
                multiplePermissionId
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == multiplePermissionId) {
            if (grantResults.isNotEmpty()) {
                var isGrant = true
                for (element in grantResults) {
                    if (element == PackageManager.PERMISSION_DENIED) {
                        isGrant = false
                    }
                }
                if (!isGrant) {
                    var someDenied = false
                    for (permission in permissions) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permission
                            )
                        ) {
                            if (ActivityCompat.checkSelfPermission(
                                    this,
                                    permission
                                ) == PackageManager.PERMISSION_DENIED
                            ) {
                                someDenied = true
                            }
                        }
                    }
                    if (someDenied) {
                        appSettingOpen(this)
                    } else {
                        warningPermissionDialog(this) { _: DialogInterface, which: Int ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE ->
                                    checkMultiplePermission()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val KEY_REPO_NAME = "repoName"
    }

    private fun calculateRemainingTime(targetTimeMillis: Long): String {
        val now = System.currentTimeMillis()
        val distance = targetTimeMillis - now

        if (distance <= 0) {
            return getString(R.string.happy_new_year)
        }

        val days = distance / (1000 * 60 * 60 * 24)
        val hours = (distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
        val minutes = (distance % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (distance % (1000 * 60)) / 1000

        return String.format(getString(R.string.d_h_m_s), days, hours, minutes, seconds)
    }

<<<<<<< Updated upstream
=======


>>>>>>> Stashed changes
}
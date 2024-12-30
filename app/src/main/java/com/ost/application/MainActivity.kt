package com.ost.application

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.LayoutDirection
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ost.application.activity.settings.SettingsActivity
import com.ost.application.databinding.ActivityMainBinding
import com.ost.application.ui.core.base.FragmentInfo
import com.ost.application.ui.core.drawer.DrawerListAdapter
import com.ost.application.ui.fragment.InfoFragment
import com.ost.application.ui.fragment.CurrencyConverterFragment
import com.ost.application.ui.fragment.PowerMenuFragment
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
import dev.oneuiproject.oneui.layout.NavDrawerLayout
import dev.oneuiproject.oneui.utils.ActivityUtils
import dev.oneuiproject.oneui.widget.Toast
import java.util.Calendar

class MainActivity : AppCompatActivity(), DrawerListAdapter.DrawerListener {
    private lateinit var countdownRunnable: Runnable
    private var mBinding: ActivityMainBinding? = null
    private var mFragmentManager: FragmentManager? = null
    private val fragments: MutableList<Fragment?> = ArrayList()

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
                    mBinding!!.drawerLayout.setExpandedSubtitle(getString(R.string.happy_new_year))
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
                    mBinding!!.drawerLayout.setExpandedSubtitle(remainingTime)
                }

                handler.postDelayed(this, 1000)
            }
        }

        handler.post(countdownRunnable)
    }

    private fun initFragmentList() {
        fragments.add(CurrencyConverterFragment())
        fragments.add(null)
        fragments.add(DefaultInfoFragment())
        fragments.add(CPUInfoFragment())
        fragments.add(BatteryInfoFragment())
        fragments.add(DisplayInfoFragment())
        fragments.add(NetworkInfoFragment())
        fragments.add(CameraInfoFragment())
        fragments.add(null)
        fragments.add(PowerMenuFragment())
        fragments.add(AppListFragment())
        fragments.add(StargazersListFragment())
        fragments.add(null)
        fragments.add(InfoFragment())
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initDrawer() {
        mBinding!!.drawerLayout.setDrawerButtonIcon(getDrawable(dev.oneuiproject.oneui.R.drawable.ic_oui_settings_outline))
        mBinding!!.drawerLayout.setDrawerButtonTooltip(getString(R.string.settings))
        mBinding!!.drawerLayout.setDrawerButtonOnClickListener {
            ActivityUtils.startPopOverActivity(
                this@MainActivity,
                Intent(this@MainActivity, SettingsActivity::class.java),
                null,
                ActivityUtils.POP_OVER_POSITION_TOP or
                        (if (isRTL) ActivityUtils.POP_OVER_POSITION_RIGHT else ActivityUtils.POP_OVER_POSITION_LEFT)
            )
        }
        mBinding!!.drawerListView.layoutManager = LinearLayoutManager(this)
        mBinding!!.drawerListView.adapter =
            DrawerListAdapter(this, fragments, this)
        mBinding!!.drawerListView.itemAnimator = null
        mBinding!!.drawerListView.setHasFixedSize(true)
        mBinding!!.drawerListView.seslSetLastRoundedCorner(true)
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
            mBinding!!.drawerLayout.setCollapsedSubtitle(
                (newFragment as FragmentInfo).title
            )
            mBinding!!.drawerLayout.setTitle(getString(R.string.time_left_until_the_new_year), getString(R.string.app_name))
        }
        mBinding!!.drawerLayout.setDrawerOpen(false, true)

        return true
    }

    val drawerLayout: NavDrawerLayout
        get() = mBinding!!.drawerLayout

    override fun onStart() {
        super.onStart()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val res = checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
            if (res != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 123)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.read_phone_state_denied),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        private const val REQUEST_CODE_ASK_PERMISSIONS = 1002
        const val KEY_REPO_NAME = "repoName"
    }

    private val isRTL: Boolean get() = resources.configuration.layoutDirection == LayoutDirection.RTL

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
}
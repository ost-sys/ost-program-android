package com.ost.application

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.ChangeBounds
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.IntentCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ost.application.data.model.Stargazer
import com.ost.application.databinding.ActivityStargazerBinding
import com.ost.application.ui.core.util.ActivityBackAnimationDelegate
import com.ost.application.ui.core.util.loadImageFromUrl
import com.ost.application.ui.core.util.onSingleClick
import com.ost.application.ui.core.util.openUrl
import com.ost.application.ui.core.util.semSetToolTipText
import com.ost.application.ui.core.toast
import com.ost.application.ui.core.util.SharingUtils.isSamsungQuickShareAvailable
import com.ost.application.ui.core.util.SharingUtils.share
import com.topjohnwu.superuser.internal.Utils.context
import dev.oneuiproject.oneui.widget.CardItemView
import kotlinx.coroutines.launch
import dev.oneuiproject.oneui.R as designR

@SuppressLint("RestrictedApi")
class ProfileActivity : AppCompatActivity(){

    companion object{
        private const val TAG = "ProfileActivity"
        const val KEY_STARGAZER = "key_stargazer"
        const val KEY_TRANSITION_CONTAINER = "key_transition_name"
        const val KEY_TRANSITION_AVATAR = "key_transition_name_avatar"
        const val KEY_TRANSITION_NAME = "key_transition_name_sgname"
    }

    private lateinit var mBinding: ActivityStargazerBinding
    private lateinit var stargazer: Stargazer

    override fun onCreate(savedInstanceState: Bundle?) {
        setupSharedElementTransitionWindow()
        super.onCreate(savedInstanceState)

        mBinding = ActivityStargazerBinding.inflate(layoutInflater)
        setupSharedElementTransitionView()
        postponeEnterTransition()

        mBinding.toolbarLayout.apply {
            setNavigationButtonAsBack()
            isExpandable = false
            toolbar.fadeIn()
        }

        setContentView(mBinding.root)

        initContent()

        setupBottomNav()

    }

    override fun onStart() {
        super.onStart()
        ActivityBackAnimationDelegate.init(this, mBinding.root).apply{
            onBackInvoked = {
                mBinding.toolbarLayout.toolbar.alpha = 0f
                mBinding.bottomNav.alpha = 0f
                mBinding.stargazerDetailsContainer.alpha = 0f
            }
        }
    }

    private fun initContent() {
        stargazer = IntentCompat.getParcelableExtra(intent, KEY_STARGAZER, Stargazer::class.java)!!

        with (stargazer) {
            mBinding.stargazerAvatar.loadImageFromUrl(avatar_url)
            mBinding.stargazerName.text = getDisplayName()
            mBinding.stargazerGithubUrl.text = html_url


            mBinding.stargazerButtons.stargazerGithubBtn.apply {
                onSingleClick { openUrl(html_url) }
                semSetToolTipText(html_url)
            }

            email?.let {e ->
                mBinding.stargazerButtons.stargazerEmailBtn.apply {
                    isVisible = true
                    onSingleClick {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$email")
                        }

                        try {
                            startActivity(Intent.createChooser(emailIntent,
                                context.getString(R.string.select_mail_client)))
                        } catch (e: ActivityNotFoundException) {
                            toast(context.getString(R.string.no_mail_clients_found))
                        }
                    }
                    semSetToolTipText(e)
                }
            }

            twitter_username?.let {x ->
                mBinding.stargazerButtons.stargazerTwitterBtn.apply {
                    isVisible = true
                    onSingleClick { openUrl("https://x.com/$x") }
                    semSetToolTipText(x)
                }
            }

            stargazer.blog?.let {b ->
                if (b.isEmpty()) return@let
                mBinding.stargazerButtons.stargazerBlog.apply {
                    isVisible = true
                    onSingleClick { openUrl(b) }
                    semSetToolTipText(b)
                }
            }

            setupStargazerDetails()
        }
    }
    private fun setupStargazerDetails(){
        mBinding.stargazerDetailsContainer.alpha = 0f
        with (stargazer) {

            val cardDetailsMap = mapOf (
                location to designR.drawable.ic_oui_location_outline,
                company to designR.drawable.ic_oui_work,
                email to designR.drawable.ic_oui_email_outline,
                bio to designR.drawable.ic_oui_info_outline
            )

            var added = 0
            for (i in cardDetailsMap) {
                if (i.key.isNullOrEmpty()) continue
                addCardItemView(
                    icon = AppCompatResources.getDrawable(this@ProfileActivity, i.value)!!,
                    title = i.key!!,
                    showTopDivider = added > 0
                )
                added += 1
            }
        }
        mBinding.stargazerDetailsContainer.fadeIn()
    }

    private fun addCardItemView(
        icon: Drawable,
        title: String,
        showTopDivider: Boolean
    ) {
        mBinding.stargazerDetailsContainer.addView(
            CardItemView(this@ProfileActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                this.icon = icon
                this.title = title
                this.showTopDivider = showTopDivider
            }
        )
    }

    private fun setupSharedElementTransitionWindow(){
        window.apply {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            sharedElementsUseOverlay = false
            AutoTransition().let {
                it.duration = 250
                sharedElementExitTransition = it
                sharedElementEnterTransition = it
                sharedElementReturnTransition = it
                sharedElementReenterTransition = it
            }
        }
    }

    private fun setupSharedElementTransitionView(){
        mBinding.stargazerAvatar.transitionName = intent.getStringExtra(KEY_TRANSITION_AVATAR)
        mBinding.headerContainer.transitionName = intent.getStringExtra(KEY_TRANSITION_CONTAINER)
        mBinding.root.apply {
            isTransitionGroup = true
            doOnPreDraw { startPostponedEnterTransition() }
        }
        mBinding.stargazerAvatar.doOnPreDraw { startPostponedEnterTransition() }
        mBinding.stargazerAvatar.transitionName = intent.getStringExtra(KEY_TRANSITION_AVATAR)
        mBinding.stargazerName.transitionName = intent.getStringExtra(KEY_TRANSITION_NAME)
    }

    private fun setupBottomNav(){
        mBinding.bottomNav.menu.findItem(R.id.menu_sg_share).apply {
            title = if (isSamsungQuickShareAvailable()) getString(R.string.quick_share) else getString(R.string.share)
        }

        mBinding.bottomNav.setOnItemSelectedListener {
            when(it.itemId){
                R.id.menu_sg_share -> {
                    lifecycleScope.launch {
                        stargazer.asVCardFile(this@ProfileActivity).share(this@ProfileActivity)
                    }
                    true
                }
                R.id.menu_sg_qrcode -> {
                    QRBottomSheet.newInstance(stargazer).show(supportFragmentManager, null)
                    true
                }
                else -> false
            }
        }
        mBinding.bottomNav.fadeIn()
    }

    private fun View.fadeIn() = apply {
        alpha = 0f
        animate()
            .alpha(1f)
            .setStartDelay(150)
            .duration = 250
    }

}
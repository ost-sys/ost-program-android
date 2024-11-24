package com.ost.application

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ost.application.ProfileActivity.Companion.KEY_STARGAZER
import com.ost.application.data.model.Stargazer
import com.ost.application.databinding.ActivityStargazerQrBinding
import com.ost.application.ui.core.toast

class QRBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ActivityStargazerQrBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(stargazer: Stargazer): QRBottomSheet {
            return QRBottomSheet().apply{
                arguments = Bundle().apply {
                    putParcelable(KEY_STARGAZER, stargazer)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            behavior.skipCollapsed = true
            setOnShowListener {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ActivityStargazerQrBinding.inflate(inflater, container, false).also {
        _binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stargazer = BundleCompat.getParcelable(
            requireArguments(),
            KEY_STARGAZER,
            Stargazer::class.java
        )!!
        binding.sgName.text = stargazer.getDisplayName()
        binding.noteTv.text = getString(R.string.scan_qr_code_m, stargazer.getDisplayName())

        Glide.with(this)
            .load(stargazer.avatar_url)
            .circleCrop()
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    binding.qrCode.apply {
                        setContent(stargazer.html_url)
                        setIcon(resource)
                        invalidate()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        binding.quickShareBtn.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, stargazer.html_url)
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, stargazer.getDisplayName())
            shareIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.share_via))

            val pm = requireContext().packageManager
            val resInfoList = pm.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY)

            var shareAppPackageName = ""

            val quickShareAvailable = resInfoList.any { it.activityInfo.packageName.startsWith("com.google.android.gms") }
            if (quickShareAvailable) {
                shareAppPackageName = "com.google.android.gms"
            } else {
                val samsungShareAvailable = resInfoList.any { it.activityInfo.packageName == "com.samsung.android.app.sharelive" }
                if (samsungShareAvailable) {
                    shareAppPackageName = "com.samsung.android.app.sharelive"
                } else {
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
                }
            }

            shareIntent.setPackage(shareAppPackageName)
            try {
                startActivity(shareIntent)
            } catch (e: Exception) {
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
                Log.e("ShareError", "Error launching share intent: ${e.message}")
            }
        }

        binding.saveBtn.setOnClickListener {
            toast("Todo(Save image)")
        }
    }
}
package com.ost.application

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.graphics.drawable.toBitmap
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
import com.ost.application.ui.core.util.SharingUtils.isSamsungQuickShareAvailable
import com.ost.application.ui.core.util.SharingUtils.shareForResult
import java.io.File
import java.io.FileOutputStream

class QRBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ActivityStargazerQrBinding? = null
    private val binding get() = _binding!!
    private var tempImageFile: File? = null

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

        binding.quickShareBtn.apply {
            text =  if (context.isSamsungQuickShareAvailable()) context.getString(R.string.quick_share) else context.getString(R.string.share)
            setOnClickListener {
                val storageDir = requireContext().filesDir
                tempImageFile = File(storageDir, "${stargazer.getDisplayName()}_qrCode_${System.currentTimeMillis()}.png")
                FileOutputStream(tempImageFile).use { out ->
                    binding.qrCode.drawable.toBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                tempImageFile!!.shareForResult(requireContext(), shareImageResultLauncher)
            }
        }

        binding.saveBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/png"
            intent.putExtra(Intent.EXTRA_TITLE, "${stargazer.getDisplayName()}_qrCode_${System.currentTimeMillis()}.png")
            saveImageResultLauncher.launch(intent)
        }
    }

    private var shareImageResultLauncher = registerForActivityResult(StartActivityForResult()) {
        tempImageFile?.delete()
    }

    private var saveImageResultLauncher  = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = binding.qrCode.drawable.toBitmap()
            requireContext().contentResolver.openOutputStream(result.data!!.data!!)?.use { outputStream ->
                if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                    toast(getString(R.string.image_saved_successfully))
                } else {
                    toast(getString(R.string.failed_to_save_image))
                }
            } ?: run {
                toast(getString(R.string.failed_to_open_output_stream))
            }
        }
    }
}
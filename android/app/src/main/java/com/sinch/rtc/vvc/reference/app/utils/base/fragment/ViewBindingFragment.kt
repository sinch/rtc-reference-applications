package com.sinch.rtc.vvc.reference.app.utils.base.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.sinch.rtc.vvc.reference.app.utils.extensions.PermissionRequestResult

abstract class ViewBindingFragment<Binding : ViewBinding>(@LayoutRes val contentLayoutRes: Int) :
    Fragment(contentLayoutRes) {

    companion object {
        const val TAG = "ViewBindingFragment"
    }

    private var sBinding: Binding? = null

    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    private var permissionsResultCallback: (result: PermissionRequestResult) -> Unit = { _ -> }

    val binding: Binding get() = sBinding!!

    val actionBar: ActionBar? get() = (activity as? AppCompatActivity)?.supportActionBar

    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantedPermissions ->
                Log.d(TAG, "Permissions granted are $grantedPermissions")
                permissionsResultCallback(grantedPermissions)
                permissionsResultCallback = { _ -> }
            }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sBinding = setupBinding(view)
    }

    override fun onDestroyView() {
        sBinding = null
        super.onDestroyView()
    }

    abstract fun setupBinding(root: View): Binding

    fun requestPermissions(
        permissions: List<String>,
        resultCallback: (PermissionRequestResult) -> Unit
    ) {
        this.permissionsResultCallback = resultCallback
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    fun setFullScreenMode(isEnabled: Boolean) {
        if (isEnabled) {
            requireActivity().window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            actionBar?.hide()
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            requireActivity().window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            actionBar?.show()
        }
    }

}
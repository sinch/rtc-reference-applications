package com.sinch.rtc.vvc.reference.app.utils.base.fragment

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.annotation.LayoutRes
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.sinch.rtc.vvc.reference.app.navigation.main.MainViewModel


abstract class MainActivityFragment<Binding : ViewBinding>(@LayoutRes contentLayoutRes: Int) :
    ViewBindingFragment<Binding>(contentLayoutRes) {

    val mainActivityViewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val backPressedCallback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackPressed()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    open fun onBackPressed() {
        if (!findNavController().popBackStack()) {
            requireActivity().finish()
        }
    }

}
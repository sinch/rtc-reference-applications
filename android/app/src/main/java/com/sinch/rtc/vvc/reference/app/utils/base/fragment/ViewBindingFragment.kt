package com.sinch.rtc.vvc.reference.app.utils.base.fragment

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class ViewBindingFragment<Binding : ViewBinding>(@LayoutRes val contentLayoutRes: Int) :
    Fragment(contentLayoutRes) {

    private var sBinding: Binding? = null

    val binding: Binding get() = sBinding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sBinding = setupBinding(view)
    }

    override fun onDestroyView() {
        sBinding = null
        super.onDestroyView()
    }

    abstract fun setupBinding(root: View): Binding

}
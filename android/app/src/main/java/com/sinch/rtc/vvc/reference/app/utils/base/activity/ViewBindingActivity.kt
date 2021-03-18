package com.sinch.rtc.vvc.reference.app.utils.base.activity

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class ViewBindingActivity<Binding : ViewBinding> : AppCompatActivity() {

    lateinit var binding: Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = setupBinding(layoutInflater)
        setContentView(binding.root)
    }

    abstract fun setupBinding(inflater: LayoutInflater): Binding
}
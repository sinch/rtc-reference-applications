package com.sinch.rtc.vvc.reference.app.utils.jwt

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel

fun AndroidViewModel.getString(@StringRes resId: Int) =
    getApplication<Application>().getString(resId)

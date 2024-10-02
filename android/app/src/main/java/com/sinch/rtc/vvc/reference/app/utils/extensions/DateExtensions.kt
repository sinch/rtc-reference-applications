package com.sinch.rtc.vvc.reference.app.utils.extensions

import java.util.Date

val Date.isSet: Boolean get() = time != 0L

val Date.valueOrNullIfNotSet: Date? get() = if (isSet) this else null
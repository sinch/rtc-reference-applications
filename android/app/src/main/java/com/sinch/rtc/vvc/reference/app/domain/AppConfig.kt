package com.sinch.rtc.vvc.reference.app.domain

data class AppConfig(
    val name: String,
    val appKey: String,
    val appSecret: String,
    val environment: String,
    val cli: String? = null,
    val isCustom: Boolean = false
) {
    companion object {
        const val CUSTOM_CONFIG_NAME = "Custom"
    }
}

package com.sinch.rtc.vvc.reference.app.utils.extensions

import android.content.Context
import com.sinch.gson.Gson
import com.sinch.rtc.vvc.reference.app.domain.AppConfig
import com.sinch.rtc.vvc.reference.app.utils.json.JSONAssetsParser
import java.io.FileNotFoundException

private const val CONFIG_ASSET_FILENAME = "config.json"

/** Example of json.config file
{
"appKey": "***",
"appSecret": "***",
"environment": "ocra.api.sinch.com"
}
 */
val Context.defaultConfig: AppConfig
    get() {
        return try {
            jsonAssetAsObject(CONFIG_ASSET_FILENAME)
        } catch (e: FileNotFoundException) {
            throw RuntimeException(
                "Config file not present. Put config.json file inside assets " +
                        "folder first then re-run the application."
            )
        }
    }

inline fun <reified T> Context.jsonAssetAsObject(jsonFileName: String): T =
    Gson().fromJson(
        JSONAssetsParser.inputToString(assets.open(jsonFileName)),
        T::class.java
    )
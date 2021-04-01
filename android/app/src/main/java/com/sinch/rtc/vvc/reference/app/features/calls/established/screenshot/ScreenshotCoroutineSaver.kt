package com.sinch.rtc.vvc.reference.app.features.calls.established.screenshot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.sinch.android.rtc.video.VideoFrame
import com.sinch.android.rtc.video.VideoUtils
import com.sinch.rtc.vvc.reference.app.R
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ScreenshotCoroutineSaver(
    private val context: Context,
    private val title: String,
    private val videoFrame: VideoFrame
) {

    companion object {
        const val TAG = "ScreenshotCoroutineSave"
        const val NOTIFICATION_CHANNEL_ID = "ScreenshotCoroutineSaverChannelId"
        const val NOTIFICATION_ID = 456
    }

    private val yuvImage: YuvImage by lazy {
        frameAsYuvImage()
    }

    private val saveFile: File by lazy {
        File(savePath).also {
            if (!it.exists()) {
                it.createNewFile()
            }
        }
    }

    private val notificationManager: NotificationManager
        get() =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val savePath: String
        get() {
            val directory =
                context.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath.orEmpty()
            return "$directory/$title.jpg"
        }


    fun saveAsync(): Deferred<FrameCaptureResult> {
        return GlobalScope.async(Dispatchers.IO) {
            try {
                val stream = ByteArrayOutputStream().also {
                    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, it)
                }
                stream.writeTo(FileOutputStream(saveFile))
                showNotification(savePath)
                Captured(savePath)
            } catch (e: Exception) {
                Error(e)
            }
        }
    }

    private fun frameAsYuvImage(): YuvImage = VideoUtils.I420toNV21Frame(videoFrame).run {
        YuvImage(
            yuvPlanes()[0].array(),
            ImageFormat.NV21,
            width(),
            height(),
            yuvStrides()
        )
    }

    private fun showNotification(uri: String) {
        createNotificationChannel()

        val contentIntent = Intent().apply {
            action = Intent.ACTION_VIEW
            flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            setDataAndType(fileProviderUri(uri), "image/*")
        }

        val screenshotBitmap = BitmapFactory.decodeFile(uri)
        val contentPendingIntent = PendingIntent.getActivity(
            context, 0,
            contentIntent, 0
        )

        val notification = NotificationCompat.Builder(
            context, NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_baseline_screenshot_24)
            .setContentTitle(context.getString(R.string.screenshot_saved))
            .setLargeIcon(screenshotBitmap)
            .setContentIntent(contentPendingIntent)
            .setDefaults(Notification.DEFAULT_SOUND)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            NOTIFICATION_ID,
            notification
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notification_chanel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description =
                    context.getString(R.string.notification_chanel_description_screenshots)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun fileProviderUri(fileUri: String) =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            File(fileUri)
        )

}
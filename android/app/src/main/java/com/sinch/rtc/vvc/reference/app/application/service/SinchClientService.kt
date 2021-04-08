package com.sinch.rtc.vvc.reference.app.application.service

import android.app.*
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sinch.android.rtc.*
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallClient
import com.sinch.android.rtc.calling.CallClientListener
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.features.calls.incoming.IncomingCallInitialAction
import com.sinch.rtc.vvc.reference.app.features.calls.incoming.IncomingCallInitialData
import com.sinch.rtc.vvc.reference.app.navigation.main.MainActivity
import com.sinch.rtc.vvc.reference.app.storage.RTCVoiceVideoAppDatabase
import com.sinch.rtc.vvc.reference.app.storage.prefs.SharedPrefsManager
import com.sinch.rtc.vvc.reference.app.utils.jwt.FakeJWTFetcher
import com.sinch.rtc.vvc.reference.app.utils.jwt.JWTFetcher

class SinchClientService : Service(), SinchClientListener, CallClientListener {

    companion object {
        const val TAG = "SinchClientService"
        const val NOTIFICATION_CHANNEL_ID = "SinchClientServiceChannel"
        const val NOTIFICATION_ID = 25
        const val NOTIFICATION_PENDING_INTENT_ID = 1212
    }

    private val notificationManager: NotificationManager get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val isInForeground: Boolean get() = checkIfInForeground()
    private val systemVersionDisallowsExplicitActivityStart: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private val jwtFetcher: JWTFetcher by lazy {
        FakeJWTFetcher(prefsManager)
    }
    private val prefsManager: SharedPrefsManager by lazy {
        SharedPrefsManager(appContext = application)
    }
    private val userDao by lazy {
        RTCVoiceVideoAppDatabase.getDatabase(this).userDao()
    }

    private var sinchClientInstance: SinchClient? = null

    override fun onCreate() {
        super.onCreate()
        registerSinchClientIfNecessary()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return SinchClientServiceBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return true
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy gets called")
        if (sinchClientInstance != null && sinchClientInstance?.isStarted == true) {
            sinchClientInstance?.terminateGracefully()
        }
        super.onDestroy()
    }

    private fun registerSinchClientIfNecessary() {
        val loggedInUser = userDao.loadLoggedInUser()
        if (loggedInUser == null || (sinchClientInstance != null &&
                    sinchClientInstance?.isStarted == true)
        ) {
            return
        }
        Log.d(TAG, "Regsitering sinch client for user ${loggedInUser.id}")
        sinchClientInstance = Sinch.getSinchClientBuilder()
            .context(this)
            .environmentHost(prefsManager.environment)
            .userId(loggedInUser.id)
            .applicationKey(prefsManager.appKey)
            .build()
            .apply {
                addSinchClientListener(this@SinchClientService)
                callClient.addCallClientListener(this@SinchClientService)
                setSupportManagedPush(true)
                start()
            }
    }

    override fun onCredentialsRequired(clientRegistration: ClientRegistration?) {
        Log.d(TAG, "onCredentialsRequired $clientRegistration")
        val loggedInUser = userDao.loadLoggedInUser();
        if (loggedInUser != null) {
            jwtFetcher.acquireJWT(prefsManager.appKey, loggedInUser.id) { jwt ->
                clientRegistration?.register(jwt)
            }
        }
    }

    override fun onUserRegistered() {
        Log.d(TAG, "onUserRegistered called")
    }

    override fun onUserRegistrationFailed(p0: SinchError?) {
        Log.d(TAG, "onUserRegistrationFailed $p0")
    }

    override fun onPushTokenRegistered() {
        Log.d(TAG, "onPushTokenRegistered")
    }

    override fun onPushTokenRegistrationFailed(p0: SinchError?) {
        Log.d(TAG, "onPushTokenRegistrationFailed $p0")
    }

    override fun onClientStarted(p0: SinchClient?) {
        Log.d(TAG, "onClientStarted")
    }

    override fun onClientFailed(p0: SinchClient?, p1: SinchError?) {
        Log.d(TAG, "onClientFailed $p1")
    }

    override fun onLogMessage(p0: Int, p1: String?, p2: String?) {
        Log.d(TAG, "onLogMessage $p1 $p2")
    }

    override fun onIncomingCall(p0: CallClient?, call: Call?) {
        Log.d(TAG, "onIncomingCall $call")
        if (call == null) {
            return
        }
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(
                MainActivity.INITIAL_INCOMING_CALL_DATA,
                IncomingCallInitialData(call.callId, IncomingCallInitialAction.NONE, isInForeground)
            )
        }

        if (systemVersionDisallowsExplicitActivityStart && !isInForeground) {
            createNotification(call, mainActivityIntent).let {
                notificationManager.notify(
                    NOTIFICATION_ID,
                    it
                )
            }
        } else {
            startActivity(mainActivityIntent)
        }
    }

    private fun createNotification(call: Call, baseIntent: Intent): Notification {
        createNotificationChannelIfNeeded()
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.incoming_call_notification_title))
            .setContentText(call.remoteUserId)
            .setSmallIcon(R.drawable.call_pressed)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(
                createNotificationPendingIntent(
                    baseIntent,
                    NOTIFICATION_PENDING_INTENT_ID
                )
            )
            .setFullScreenIntent(
                createNotificationPendingIntent(
                    baseIntent,
                    NOTIFICATION_PENDING_INTENT_ID + 1
                ), true
            )
            .addAction(
                R.drawable.button_accept,
                getString(R.string.action_accept),
                createNotificationPendingIntent(baseIntent.apply {
                    putExtra(
                        MainActivity.INITIAL_INCOMING_CALL_DATA,
                        IncomingCallInitialData(
                            call.callId,
                            IncomingCallInitialAction.ANSWER,
                            false
                        )
                    )
                }, NOTIFICATION_PENDING_INTENT_ID + 2)
            )
            .addAction(
                R.drawable.button_decline,
                getString(R.string.action_decline),
                createNotificationPendingIntent(baseIntent.apply {
                    putExtra(
                        MainActivity.INITIAL_INCOMING_CALL_DATA,
                        IncomingCallInitialData(
                            call.callId,
                            IncomingCallInitialAction.DECLINE,
                            false
                        )
                    )
                }, NOTIFICATION_PENDING_INTENT_ID + 3)
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationPendingIntent(source: Intent, id: Int): PendingIntent =
        PendingIntent.getActivity(
            this,
            id,
            source,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null
        ) {
            return
        }
        NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_chanel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_chanel_description)
        }.let {
            notificationManager.createNotificationChannel(it)
        }
    }

    private fun checkIfInForeground(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val appProcesses: List<RunningAppProcessInfo> =
            activityManager.runningAppProcesses ?: return false
        return appProcesses.any { appProcess: RunningAppProcessInfo ->
            appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == packageName
        }
    }

    inner class SinchClientServiceBinder : Binder() {
        val sinchClient: SinchClient? get() = sinchClientInstance
    }

}
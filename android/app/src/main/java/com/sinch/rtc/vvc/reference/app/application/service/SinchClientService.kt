package com.sinch.rtc.vvc.reference.app.application.service

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.sinch.android.rtc.ClientRegistration
import com.sinch.android.rtc.PushConfiguration
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.SinchClientListener
import com.sinch.android.rtc.SinchError
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallController
import com.sinch.android.rtc.calling.CallControllerListener
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.features.calls.incoming.IncomingCallInitialAction
import com.sinch.rtc.vvc.reference.app.features.calls.incoming.IncomingCallInitialData
import com.sinch.rtc.vvc.reference.app.navigation.main.MainActivity
import com.sinch.rtc.vvc.reference.app.storage.RTCVoiceVideoAppDatabase
import com.sinch.rtc.vvc.reference.app.storage.prefs.SharedPrefsManager
import com.sinch.rtc.vvc.reference.app.utils.jwt.FakeJWTFetcher
import com.sinch.rtc.vvc.reference.app.utils.jwt.JWTFetcher
import com.sinch.rtc.vvc.reference.app.utils.mvvm.ConsumableEventsLiveData
import java.io.File

class SinchClientService : Service(), SinchClientListener, CallControllerListener, NewCallHook {

    companion object {
        const val TAG = "SinchClientService"
        const val NOTIFICATION_CHANNEL_ID = "SinchClientServiceChannel"
        const val NOTIFICATION_ID = 25
        const val NOTIFICATION_PENDING_INTENT_ID = 1212
    }

    private val notificationManager: NotificationManager get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val jwtFetcher: JWTFetcher by lazy {
        FakeJWTFetcher(prefsManager)
    }

    private val notificationSoundUri: Uri by lazy {
        Uri.parse(
            ContentResolver.SCHEME_ANDROID_RESOURCE
                + File.pathSeparator + File.separator + File.separator
                + packageName
                + File.separator
                + R.raw.progress_tone
        )
    }

    /**
     * Use [ConsumableEvent] abstraction layer for call ended events in order to:
     *
     * - Allow presenting the [CallSummaryDialogFragment] on top of any visible UI element, no matter which
     *   one is currently visible.
     * - Be notified about call ended events even if app UI is presented after the call has ended.
     * - Make sure only single listener gets notified about the event to avoid duplicating presenting same dialog.
     */
    private val callEndedEvents: ConsumableEventsLiveData<Call> = ConsumableEventsLiveData()
    private val callEndedConsumableEventsListener = CallEndedConsumableEventsListener(callEndedEvents)

    private val prefsManager: SharedPrefsManager by lazy {
        SharedPrefsManager(appContext = application)
    }
    private val userDao by lazy {
        RTCVoiceVideoAppDatabase.getDatabase(this).userDao()
    }
    private val appConfig get() = prefsManager.usedConfig

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
        sinchClientInstance = SinchClient.builder()
            .context(this)
            .environmentHost(appConfig.environment)
            .userId(loggedInUser.id)
            .applicationKey(appConfig.appKey)
            .pushConfiguration(
                PushConfiguration.fcmPushConfigurationBuilder()
                    .senderID(FirebaseApp.getInstance().options.gcmSenderId.orEmpty())
                    .registrationToken(prefsManager.fcmRegistrationToken)
                    .build()
            )
            .build()
            .apply {
                addSinchClientListener(this@SinchClientService)
                callController.addCallControllerListener(this@SinchClientService)
                start()
            }
    }

    override fun onCredentialsRequired(clientRegistration: ClientRegistration) {
        Log.d(TAG, "onCredentialsRequired $clientRegistration")
        val loggedInUser = userDao.loadLoggedInUser()
        if (loggedInUser != null) {
            jwtFetcher.acquireJWT(appConfig.appKey, loggedInUser.id) { jwt ->
                clientRegistration.register(jwt)
            }
        }
    }

    override fun onUserRegistered() {
        Log.d(TAG, "onUserRegistered called")
    }

    override fun onUserRegistrationFailed(error: SinchError) {
        Log.d(TAG, "onUserRegistrationFailed $error")
    }

    override fun onPushTokenRegistered() {
        Log.d(TAG, "onPushTokenRegistered")
    }

    override fun onPushTokenUnregistered() {
        Log.d(TAG, "onPushTokenUnregistered")
    }

    override fun onPushTokenUnregistrationFailed(error: SinchError) {
        Log.d(TAG, "onPushTokenUnregistrationFailed $error")
    }

    override fun onPushTokenRegistrationFailed(error: SinchError) {
        Log.d(TAG, "onPushTokenRegistrationFailed $error")
    }

    override fun onClientStarted(client: SinchClient) {
        Log.d(TAG, "onClientStarted")
    }

    override fun onClientFailed(client: SinchClient, error: SinchError) {
        Log.d(TAG, "onClientFailed $error")
    }

    override fun onLogMessage(level: Int, area: String, message: String) {
        Log.d(TAG, "onLogMessage $area $message")
    }

    override fun onIncomingCall(callController: CallController, call: Call) {
        Log.d(TAG, "onIncomingCall $call")
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(
                MainActivity.INITIAL_INCOMING_CALL_DATA,
                IncomingCallInitialData(call.callId, IncomingCallInitialAction.NONE)
            )
        }

        createNotification(call, mainActivityIntent).let {
            notificationManager.notify(
                NOTIFICATION_ID,
                it
            )
        }
        call.addCallListener(NotificationCancellationListener(notificationManager))
        call.addCallListener(callEndedConsumableEventsListener)
    }

    override fun onNewSinchCall(call: Call) {
        call.addCallListener(callEndedConsumableEventsListener)
    }

    private fun createNotification(call: Call, baseIntent: Intent): Notification {
        createNotificationChannelIfNeeded()
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.incoming_call_notification_title))
            .setContentText(call.remoteUserId)
            .setSmallIcon(R.drawable.call_pressed)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(notificationSoundUri)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(
                createNotificationPendingIntent(
                    baseIntent,
                    NOTIFICATION_PENDING_INTENT_ID
                )
            )
            .addAction(
                R.drawable.button_accept,
                getString(R.string.action_accept),
                createNotificationPendingIntent(baseIntent.apply {
                    putExtra(
                        MainActivity.INITIAL_INCOMING_CALL_DATA,
                        IncomingCallInitialData(
                            call.callId,
                            IncomingCallInitialAction.ANSWER)
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
                            IncomingCallInitialAction.DECLINE)
                    )
                }, NOTIFICATION_PENDING_INTENT_ID + 3)
            )
            .setOngoing(true)
            .build().apply {
                flags = flags or Notification.FLAG_INSISTENT
            }
    }

    private fun createNotificationPendingIntent(source: Intent, id: Int): PendingIntent =
        PendingIntent.getActivity(
            this,
            id,
            source,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
        val callEndedEvents: ConsumableEventsLiveData<Call> get() = this@SinchClientService.callEndedEvents
        val newCallHook: NewCallHook get() = this@SinchClientService
    }

}
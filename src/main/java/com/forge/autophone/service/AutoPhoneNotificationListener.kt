package com.forge.autophone.service

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * AutoPhoneNotificationListener - Listens to and interacts with system notifications.
 * 
 * This service allows AutoPhone to:
 * - Read all active notifications
 * - Dismiss notifications
 * - Reply to notifications (for messaging apps)
 * - Monitor notification events
 * 
 * Requires notification listener permission from user (Settings > Notifications > Notification Access).
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class AutoPhoneNotificationListener : NotificationListenerService() {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    
    companion object {
        /**
         * Singleton instance of the listener service.
         * Null if service is not enabled or not running.
         */
        @Volatile
        var instance: AutoPhoneNotificationListener? = null
            private set
        
        /**
         * Cache of active notifications by key.
         */
        private val notificationCache = ConcurrentHashMap<String, StatusBarNotification>()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        
        // Load current notifications
        try {
            activeNotifications?.forEach { sbn ->
                notificationCache[sbn.key] = sbn
            }
            Timber.i("Notification listener connected, loaded ${notificationCache.size} notifications")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load active notifications")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        notificationCache.clear()
        Timber.i("Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        notificationCache[sbn.key] = sbn
        Timber.d("Notification posted: ${sbn.packageName} - ${sbn.notification.extras.getString(Notification.EXTRA_TITLE)}")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        notificationCache.remove(sbn.key)
        Timber.d("Notification removed: ${sbn.key}")
    }

    /**
     * Get all active notifications as JSON.
     */
    fun readNotificationsJson(): String {
        return try {
            val notifications = notificationCache.values.map { sbn ->
                notificationToInfo(sbn)
            }
            json.encodeToString(notifications)
        } catch (e: Exception) {
            Timber.e(e, "Failed to read notifications")
            "[]"
        }
    }

    /**
     * Get all active notifications.
     */
    fun getNotifications(): List<NotificationInfo> {
        return notificationCache.values.map { sbn ->
            notificationToInfo(sbn)
        }
    }

    /**
     * Dismiss a notification by its key.
     * 
     * @param key The notification key (from NotificationInfo.key)
     * @return true if successfully dismissed
     */
    fun dismissNotification(key: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cancelNotification(key)
            } else {
                @Suppress("DEPRECATION")
                cancelNotification(
                    notificationCache[key]?.packageName,
                    notificationCache[key]?.tag,
                    notificationCache[key]?.id ?: 0
                )
            }
            notificationCache.remove(key)
            Timber.d("Dismissed notification: $key")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to dismiss notification: $key")
            false
        }
    }

    /**
     * Reply to a notification (for messaging apps).
     * Requires the notification to have a reply action.
     * 
     * @param key The notification key
     * @param replyText The text to send as reply
     * @return true if reply was sent
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun replyToNotification(key: String, replyText: String): Boolean {
        return try {
            val sbn = notificationCache[key] ?: return false
            val notification = sbn.notification
            
            // Find reply action
            val replyAction = notification.actions?.firstOrNull { action ->
                action.remoteInputs?.any { it.allowFreeFormInput == true } == true
            } ?: return false
            
            val remoteInput = replyAction.remoteInputs?.firstOrNull { it.allowFreeFormInput } ?: return false
            
            // Create reply intent
            val replyIntent = Intent()
            val bundle = android.os.Bundle()
            bundle.putCharSequence(remoteInput.resultKey, replyText)
            android.app.RemoteInput.addResultsToIntent(arrayOf(remoteInput), replyIntent, bundle)
            
            // Send reply
            replyAction.actionIntent.send(this, 0, replyIntent)
            
            Timber.d("Sent reply to notification: $key")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to reply to notification: $key")
            false
        }
    }

    /**
     * Convert StatusBarNotification to NotificationInfo.
     */
    private fun notificationToInfo(sbn: StatusBarNotification): NotificationInfo {
        val notification = sbn.notification
        val extras = notification.extras
        
        return NotificationInfo(
            key = sbn.key,
            packageName = sbn.packageName,
            title = extras.getString(Notification.EXTRA_TITLE) ?: "",
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
            subText = extras.getString(Notification.EXTRA_SUB_TEXT),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            timestamp = sbn.postTime,
            isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
            isClearable = sbn.isClearable,
            hasReplyAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notification.actions?.any { action ->
                    action.remoteInputs?.any { it.allowFreeFormInput == true } == true
                } ?: false
            } else false,
            category = notification.category,
            groupKey = sbn.groupKey
        )
    }
}

/**
 * Simplified notification information for AIDL/JSON serialization.
 */
@Serializable
data class NotificationInfo(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val subText: String? = null,
    val bigText: String? = null,
    val timestamp: Long,
    val isOngoing: Boolean,
    val isClearable: Boolean,
    val hasReplyAction: Boolean,
    val category: String? = null,
    val groupKey: String? = null
)

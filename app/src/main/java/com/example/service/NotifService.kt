package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Collections

class NotifService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        instance = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        if (sbn.packageName == packageName || sbn.isOngoing) return

        synchronized(notificationsList) {
            notificationsList.add(sbn)
            while (notificationsList.size > 25) {
                notificationsList.removeAt(0)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        synchronized(notificationsList) {
            notificationsList.removeAll { it.key == sbn.key }
        }
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: NotifService? = null
        private val notificationsList = Collections.synchronizedList(mutableListOf<StatusBarNotification>())

        fun isActive(): Boolean = instance != null

        private fun offMessage(): String =
            "Notification access is not enabled. Please grant 'AI Assistant' access in Notification Listener Settings."

        fun readNotifications(): String {
            if (instance == null) return offMessage()
            val sb = StringBuilder()
            synchronized(notificationsList) {
                if (notificationsList.isEmpty()) {
                    return "No new notifications found."
                }
                notificationsList.forEachIndexed { index, sbn ->
                    val extras = sbn.notification.extras
                    val title = extras.getCharSequence(Notification.EXTRA_TITLE) ?: "Unknown"
                    val text = extras.getCharSequence(Notification.EXTRA_TEXT) ?: ""
                    sb.append("[$index] ${sbn.packageName} | $title: $text\n")
                }
            }
            return sb.toString().trim()
        }

        fun replyNotification(index: Int, replyText: String): String {
            val service = instance ?: return offMessage()
            val targetSbn: StatusBarNotification
            synchronized(notificationsList) {
                if (index < 0 || index >= notificationsList.size) {
                    return "Invalid notification index: $index. Call read_notifications first to see valid indices."
                }
                targetSbn = notificationsList[index]
            }

            val actions = targetSbn.notification.actions ?: return "Notification does not support direct replies."
            for (action in actions) {
                val remoteInputs = action.remoteInputs
                if (!remoteInputs.isNullOrEmpty()) {
                    val intent = Intent()
                    val bundle = Bundle()
                    for (remoteInput in remoteInputs) {
                        bundle.putCharSequence(remoteInput.resultKey, replyText)
                    }
                    RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
                    return try {
                        action.actionIntent.send(service, 0, intent)
                        "Reply sent successfully to ${targetSbn.packageName}."
                    } catch (e: Exception) {
                        "Error sending reply: ${e.message}"
                    }
                }
            }
            return "No reply action found for this notification."
        }
    }
}

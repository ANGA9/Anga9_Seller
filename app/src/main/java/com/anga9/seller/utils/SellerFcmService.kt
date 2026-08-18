package com.anga9.seller.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.anga9.seller.R
import com.anga9.seller.SplashActivity
import com.anga9.seller.MVVM.ui.products.MyProductsActivity
import com.anga9.seller.ui.orders.MyOrdersActivity
import com.anga9.seller.ui.orders.OrderDetailActivity
import com.anga9.seller.ui.support.TicketDetailActivity
import com.anga9.seller.ui.wallet.EarningsActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * SellerFcmService â€” handles FCM push notifications for Seller App.
 *
 * Notification types:
 * - "new_order", "order_update"              â†’ OrderDetailActivity
 * - "payout"                                 â†’ WalletActivity
 * - "product_approved", "product_rejected"   â†’ MyProductsActivity
 * - "support.ticket.replied"                 â†’ TicketDetailActivity (Phase 5)
 * - "support.ticket.status_changed"          â†’ TicketDetailActivity (Phase 5)
 * - else                                     â†’ SplashActivity
 */
class SellerFcmService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ORDERS  = "seller_orders"
        const val CHANNEL_SUPPORT = "seller_support"
        const val CHANNEL_GENERAL = "seller_notifications"

        const val NOTIF_SUPPORT_TICKET = 3001

        // SharedPrefs keys for notification toggles (used in NotificationSettingsActivity)
        const val PREF_NOTIF_NEW_ORDER    = "notif_new_order"
        const val PREF_NOTIF_ORDER_STATUS = "notif_order_status"
        const val PREF_NOTIF_PAYOUT       = "notif_payout"
        const val PREF_NOTIF_PRODUCT      = "notif_product"
        const val PREF_NOTIF_LOW_STOCK    = "notif_low_stock"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenManager.onNewToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data    = remoteMessage.data
        val title   = remoteMessage.notification?.title ?: data["title"] ?: "ANGA9 Seller"
        val body    = remoteMessage.notification?.body  ?: data["body"]  ?: ""
        val type    = data["type"] ?: ""
        val orderId = data["orderId"] ?: ""

        createChannels()

        when (type) {
            "new_order", "order_update" -> {
                val intent = if (orderId.isNotEmpty())
                    Intent(this, OrderDetailActivity::class.java).apply { putExtra("ORDER_ID", orderId) }
                else
                    Intent(this, MyOrdersActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                showNotification(
                    id      = System.currentTimeMillis().toInt(),
                    channel = CHANNEL_ORDERS,
                    title   = title,
                    body    = body,
                    intent  = intent,
                    priority = NotificationCompat.PRIORITY_HIGH
                )
            }
            "payout" -> showNotification(
                id = System.currentTimeMillis().toInt(), channel = CHANNEL_GENERAL,
                title = title, body = body,
                intent = Intent(this, EarningsActivity::class.java).apply {
                    putExtra("tab", "payouts")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            "product_approved", "product_rejected" -> showNotification(
                id = System.currentTimeMillis().toInt(), channel = CHANNEL_GENERAL,
                title = title, body = body,
                intent = Intent(this, MyProductsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            // Phase 5: Support ticket notifications â€” deep-link to TicketDetailActivity
            "support.ticket.replied",
            "support.ticket.status_changed" -> handleSupportTicket(data, title, body)
            else -> showNotification(
                id = System.currentTimeMillis().toInt(), channel = CHANNEL_GENERAL,
                title = title, body = body,
                intent = Intent(this, SplashActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }
    }

    // â”€â”€â”€ Support Ticket Handler â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun handleSupportTicket(data: Map<String, String>, title: String, body: String) {
        val ticketId     = data["ticket_id"]     ?: ""
        val ticketNumber = data["ticket_number"] ?: ""
        val status       = data["status"]        ?: ""

        val notifBody = when {
            body.isNotEmpty()    -> body
            status.isNotEmpty()  -> "Ticket $ticketNumber status changed to: $status"
            else                 -> "You have a new reply on ticket $ticketNumber"
        }
        val notifTitle = when {
            title.isNotEmpty()                                    -> title
            data["type"] == "support.ticket.replied"              -> "ðŸ’¬ New Reply on Ticket"
            else                                                  -> "ðŸŽ« Ticket Status Updated"
        }

        val intent = Intent(this, TicketDetailActivity::class.java).apply {
            putExtra(TicketDetailActivity.EXTRA_TICKET_ID,     ticketId)
            putExtra(TicketDetailActivity.EXTRA_TICKET_NUMBER, ticketNumber)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        showNotification(
            id       = NOTIF_SUPPORT_TICKET,
            channel  = CHANNEL_SUPPORT,
            title    = notifTitle,
            body     = notifBody,
            intent   = intent,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    // â”€â”€â”€ Core Notification Builder â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun showNotification(
        id: Int, channel: String, title: String, body: String,
        intent: Intent, priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        val pi = PendingIntent.getActivity(
            this, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(priority)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notif)
    }

    // â”€â”€â”€ Channels â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            Triple(CHANNEL_ORDERS,  "Orders",  NotificationManager.IMPORTANCE_HIGH),
            Triple(CHANNEL_SUPPORT, "Support", NotificationManager.IMPORTANCE_HIGH),
            Triple(CHANNEL_GENERAL, "General", NotificationManager.IMPORTANCE_DEFAULT)
        ).forEach { (id, name, imp) ->
            if (mgr.getNotificationChannel(id) == null)
                mgr.createNotificationChannel(NotificationChannel(id, name, imp))
        }
    }
}

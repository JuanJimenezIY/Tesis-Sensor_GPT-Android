package com.jimenez.ecuafit.ui.utilities

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jimenez.ecuafit.R
import com.jimenez.ecuafit.ui.activities.AguaActivity

class BroadcasterNotifications : BroadcastReceiver() {

    val CHANNEL: String = "Notificaciones"

    override fun onReceive(context: Context, intent: Intent?) {

        val notificationIntent = Intent(context, AguaActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val noti = NotificationCompat.Builder(context, CHANNEL)
            .setContentTitle("Recordatorio agua")
            .setContentText("Tienes una notificacion")
            .setSmallIcon(R.drawable.awa)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Recuerda tomar la cantidad adecuada de agua en el día"))
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, noti)
    }
}

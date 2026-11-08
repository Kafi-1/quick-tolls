package com.kafi.utilityhub.volume

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.widget.RemoteViews
import com.kafi.utilityhub.R

class VolumeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val audio = context.getSystemService(AudioManager::class.java)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        when (intent.action) {
            ACTION_UP -> audio.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                (current + 1).coerceAtMost(max),
                0
            )
            ACTION_DOWN -> audio.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                (current - 1).coerceAtLeast(0),
                0
            )
        }
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, VolumeWidgetProvider::class.java))
        for (id in ids) manager.updateAppWidget(id, buildViews(context))
    }

    private fun buildViews(context: Context): RemoteViews {
        val audio = context.getSystemService(AudioManager::class.java)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val views = RemoteViews(context.packageName, R.layout.widget_volume)
        views.setTextViewText(R.id.tvWidgetVolume, "$current / $max")
        views.setOnClickPendingIntent(R.id.btnWidgetDown, broadcast(context, ACTION_DOWN))
        views.setOnClickPendingIntent(R.id.btnWidgetUp, broadcast(context, ACTION_UP))
        return views
    }

    private fun broadcast(context: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, VolumeWidgetProvider::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    companion object {
        const val ACTION_UP = "com.kafi.utilityhub.WIDGET_UP"
        const val ACTION_DOWN = "com.kafi.utilityhub.WIDGET_DOWN"
    }
}
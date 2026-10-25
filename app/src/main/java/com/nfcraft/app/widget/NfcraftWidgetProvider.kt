package com.nfcraft.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.nfcraft.app.MainActivity
import com.nfcraft.app.R

class NfcraftWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_nfcraft)
            views.setOnClickPendingIntent(R.id.widget_root, launchIntent(context))
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun launchIntent(context: Context): PendingIntent {
        val intent = Intent().apply {
            component = ComponentName(context, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }
}

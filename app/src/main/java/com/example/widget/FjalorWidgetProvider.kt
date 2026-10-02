package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.net.Result
import com.example.repo.DictionaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FjalorWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = DictionaryRepository.instance
                val wotdResult = repository.getWordOfTheDay()
                val entry = (wotdResult as? Result.Ok)?.data

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_fjalor)

                    if (entry != null) {
                        views.setTextViewText(R.id.widget_wotd_term, entry.term)
                        val def = entry.firstDefinition ?: "Prekni për të parë përkufizimin e plotë."
                        views.setTextViewText(R.id.widget_wotd_def, def)

                        // Clicking on the word card opens the word detail
                        val wordIntent = Intent(context, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra("EXTRA_WORD_SLUG", entry.slug)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val wordPendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId,
                            wordIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_root, wordPendingIntent)
                    } else {
                        views.setTextViewText(R.id.widget_wotd_term, "Fjalor Shqip")
                        views.setTextViewText(R.id.widget_wotd_def, "Prekni për të kërkuar fjalë dhe përkufizime.")

                        val mainIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val mainPendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId,
                            mainIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)
                    }

                    // Search button opens Search tab
                    val searchIntent = Intent(context, MainActivity::class.java).apply {
                        action = "ACTION_OPEN_SEARCH"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val searchPendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId + 1000,
                        searchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_search_btn, searchPendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                // Ignore widget update errors
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, FjalorWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, FjalorWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}

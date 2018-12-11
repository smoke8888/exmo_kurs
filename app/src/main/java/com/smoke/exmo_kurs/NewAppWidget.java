package com.smoke.exmo_kurs;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;




public class NewAppWidget extends AppWidgetProvider {


    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                SharedPreferences widget_pref, int appWidgetId) {

        String vyvod1, vyvod2;
        PendingIntent pIntent1, pIntent2;

        if (widget_pref.getString(appWidgetId+":error_state","").equals("no_connect")) {
            vyvod1 = "Ошибка! Отсутствует доступ в Интернет!";
            vyvod2 = "";
        }
        else if (widget_pref.getString(appWidgetId+":error_state","").equals("error_on_server")) {
            vyvod1 = "Ошибка! Сервер EXMO не доступен!";
            vyvod2 = "";
        }
        else {
            // Читаем параметры Preferences
            vyvod1 = widget_pref.getString(appWidgetId + ":name1", "") + "\n"
                    + widget_pref.getString(appWidgetId + ":value1", "");
            vyvod2 = widget_pref.getString(appWidgetId + ":name2", "") + "\n"
                    + widget_pref.getString(appWidgetId + ":value2", "");
        }
        // Настраиваем внешний вид виджета
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        views.setTextViewText(R.id.textView1, vyvod1);
        views.setTextViewText(R.id.textView2, vyvod2);


        // запуск службы обновления данных о курсе валют update_service
        Intent updateIntent = new Intent(context, update_service.class);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        pIntent1 = PendingIntent.getService(context, appWidgetId, updateIntent, 0);
        views.setOnClickPendingIntent(R.id.refreshButton, pIntent1);

        // запуск конфигурационного экрана NewAppWidgetConfigureActivity
        Intent configIntent = new Intent(context, NewAppWidgetConfigureActivity.class);
        configIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        pIntent2 = PendingIntent.getActivity(context, appWidgetId, configIntent, 0);
        views.setOnClickPendingIntent(R.id.settingsButton, pIntent2);


        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // перебираем все ID экземпляров, которые необходимо обновить и для каждого из них вызываем наш метод обновления
        SharedPreferences widget_pref = context.getSharedPreferences("WidgetEXMO", Context.MODE_PRIVATE);
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widget_pref, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        //Удаляем Preferences
        SharedPreferences.Editor editor = context.getSharedPreferences("WidgetEXMO", Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }


    //_________отлавливаем широковещательный запрос ACTION_APPWIDGET_UPDATE запущенный из update_service
    //_________ который является просто сигналом завершения работы update_service
    //_________вытаскиваем из Intent ID виджета и запускаем updateAppWidget для обновления виджета
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            // извлекаем ID экземпляра
            int mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                SharedPreferences widget_pref = context.getSharedPreferences("WidgetEXMO", Context.MODE_PRIVATE);
                updateAppWidget(context, AppWidgetManager.getInstance(context), widget_pref,
                        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
            }
        }
    }
}



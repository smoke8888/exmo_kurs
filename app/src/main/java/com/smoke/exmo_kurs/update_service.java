package com.smoke.exmo_kurs;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import javax.net.ssl.HttpsURLConnection;

public class update_service extends IntentService {

    private String pair1_name, pair2_name, pair1_value, pair2_value, error_state;
    private Boolean no_connect = false;
    private Boolean error_on_server = false;
    private String[] tickers_array = new String[3000];


    public update_service() {
        super("Exmo_kurs");
    }

    public void onCreate() {
        super.onCreate();

    }


    @Override
    public void onHandleIntent(Intent intent) {

        data_loader("https://api.exmo.me/v1/ticker/");

        SharedPreferences widget_pref = getSharedPreferences("WidgetEXMO", MODE_PRIVATE);
        SharedPreferences.Editor edit_widget_pref = widget_pref.edit();

        int mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        // извлекаем имена валютных пар из Preferences
        pair1_name = widget_pref.getString(mAppWidgetId +":name1","");
        pair2_name = widget_pref.getString(mAppWidgetId +":name2","");

        if (no_connect) {error_state = "no_connect";}
        else if (error_on_server) {error_state = "error_on_server";}
        else {
            //ищем совпадения имен валютных пар в виджете и в выгрузке tickers
            for (int i = 0; i < tickers_array.length - 1; i += 19) {
                if (pair1_name.equals(tickers_array[i])) {pair1_value = tickers_array[i + 2] + "\n" + tickers_array[i + 4];}
                if (pair2_name.equals(tickers_array[i])) {pair2_value = tickers_array[i + 2] + "\n" + tickers_array[i + 4];}
            }
        }

        // запись данных по выбранным валютам в preferences,
        // фортмат: widgetID:name1, BTC_RUB
        //          widgetID:value1, buy_price = ... sell_price = ...
        edit_widget_pref.putString(mAppWidgetId +":name1",pair1_name);
        edit_widget_pref.putString(mAppWidgetId +":value1",pair1_value);
        edit_widget_pref.putString(mAppWidgetId +":name2",pair2_name);
        edit_widget_pref.putString(mAppWidgetId +":value2",pair2_value);
        edit_widget_pref.putString(mAppWidgetId +":error_state",error_state);
        edit_widget_pref.apply();
        Intent resultValue = new Intent(update_service.this, NewAppWidget.class);
        resultValue.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        sendBroadcast(resultValue);
        stopSelf();
    }



    public String[] data_loader(String... params) {
        URL url = null;
        HttpURLConnection connection = null;
        String r = "";
        try {
            //Create connection
            url = new URL(params[0]);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Get Response
            InputStream is = null;
            is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuffer response = new StringBuffer(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine())!= null)
            {
                response.append(line);
            }
            rd.close();
            response.delete(0,2); // удаляем {" с начала строки, для успешного выполнения split ниже
            r = response.toString();
            // если от сервера пришел ответ об ошибке, то выставляем флаг ошибки
            error_on_server = r.indexOf("error") > 0;
            no_connect = false;
        }
        catch (UnknownHostException e) {
            no_connect = true;  // если связи с сервером нет, то уведомляем об отсутствии связи
        }
        catch (Exception  e) {
        }
        finally {
            if(connection != null) {connection.disconnect();}
        }
        tickers_array  = r.split("\u0000|(\\{\")|(\":\\{\")|(\":\")|(\",\")|(\\},\")|(\":)|(\\}\\})"); //убираем из выгрузки лишние знаки
        return tickers_array;
    }
}

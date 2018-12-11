package com.smoke.exmo_kurs;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;

import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

public class NewAppWidgetConfigureActivity extends Activity {

    private Boolean no_connect = false;
    private Boolean error_on_server = false;
    private String[] tickers_array = new String[3000];
    private ArrayList<CheckBox> chkbox_array = new ArrayList<>();
    private int mAppWidgetId = 0 ;

    public NewAppWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        // запрос tickers из API EXMO с помощью фонового потока
        Fon_potok fon_potok = new Fon_potok();
        try {
            fon_potok.execute("https://api.exmo.me/v1/ticker/").get();
        } catch (InterruptedException ex) {
            System.out.println(ex.getMessage());
        } catch (ExecutionException ex) {
            System.out.println(ex.getMessage());
        }

        if (no_connect) {
            Toast toast = Toast.makeText(NewAppWidgetConfigureActivity.this, "Отсутствует доступ в Интернет!", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
        if (error_on_server) {
            Toast toast = Toast.makeText(NewAppWidgetConfigureActivity.this, "Сервер EXMO не доступен, попробуйте позднее!", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        // извлекаем ID конфигурируемого виджета
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // и проверяем его корректность
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        //отображаем view на экране widget_configure Activity
        setContentView(R.layout.new_app_widget_configure);
        LinearLayout l_Layout = (LinearLayout) findViewById(R.id.l_layout2);
        FloatingActionButton save_button = (FloatingActionButton) findViewById(R.id.save_button);
        save_button.setOnClickListener(onClickListener);

        // выводим checkbox с наименованием валютных пар для выбора их в виджет
        int j = 0, k = 0;
        for (int i = 0; i < tickers_array.length - 1; i += 19) {
            CheckBox chkbox_N = new CheckBox(this);
            chkbox_N.setText(tickers_array[i]);
            chkbox_N.setId(k);
            chkbox_array.add(k, chkbox_N);
            l_Layout.addView(chkbox_N);
            k++;
        }

    }

    //_________________________________________________________________________________________________________________
    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int num_selected_chkbox = 0;
            switch (v.getId()) {
                case R.id.save_button: { //нажатие на кнопку сохранить
                    SharedPreferences widget_pref = getSharedPreferences("WidgetEXMO", MODE_PRIVATE);
                    SharedPreferences.Editor edit_widget_pref = widget_pref.edit();
                    for(int i = 0; i < chkbox_array.size(); i++) {
                        if (chkbox_array.get(i).isChecked()) {
                            ++num_selected_chkbox;
                            // запись выбранных валют в preferences,
                            // фортмат: widgetID:name1, BTC_RUB
                            //          widgetID:value1, buy_price = ... sell_price = ...
                            edit_widget_pref.putString(mAppWidgetId+":name"+num_selected_chkbox,chkbox_array.get(i).getText().toString());
                            edit_widget_pref.putString(mAppWidgetId+":value"+num_selected_chkbox,tickers_array[i*19+2]+ "\n" + tickers_array[i*19+4]);
                        }
                    }
                    if (num_selected_chkbox > 2) {
                        //если выбрано более 2-х валют, то обнуляем Preference и выводим сообщение
                        edit_widget_pref.clear();
                        edit_widget_pref.apply();
                        Toast toast = Toast.makeText(NewAppWidgetConfigureActivity.this,
                                                    "Выберите не более 2-х позиций!", Toast.LENGTH_SHORT);
                        toast.show();
                        break;
                    }
                    edit_widget_pref.apply();

                    //этот блок нужен для обновления виджета сразу после сохранения настроек
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(NewAppWidgetConfigureActivity.this);
                    NewAppWidget.updateAppWidget(NewAppWidgetConfigureActivity.this, appWidgetManager, widget_pref, mAppWidgetId);

                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    // положительный ответ
                    setResult(RESULT_OK, resultValue);
                    finish();
                    break;
                }
            }
        }
    };



    //фоновый поток для выгрузки с сайта ЕХМО информации по валютным парам и ее обработка_______________
    class Fon_potok extends AsyncTask<String,Void,String[]> {
        @Override
        protected String[] doInBackground(String... params) {
            //String urlParameters = null;
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

        @Override
        protected void onPostExecute(final String[] result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tickers_array = result;
                }
            });
        }
    }


}


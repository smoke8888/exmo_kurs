package com.smoke.exmo_kurs;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.support.v7.widget.Toolbar;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


public class SettingsActivity extends AppCompatActivity {
    private char[] settings_char = new char[1000];
    private String[] pair_array = new String[100];
    private ArrayList<CheckBox> chkbox_array = new ArrayList<CheckBox>();
//_________________________________________________________________________________________________________________
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        FloatingActionButton save_button = (FloatingActionButton) findViewById(R.id.save_button);
        save_button.setOnClickListener(onClickListener);
        LinearLayout l_Layout = (LinearLayout) findViewById(R.id.l_layout);
        Toolbar toolbar2 = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar2);

        File settings_file = new File(this.getFilesDir(),"exmo_kurs_settings.txt");
        //чтение файла settings
        try
        {
            // читаем файл Settings посимвольно, затем переводим в ArrayList
            FileReader settings_file_reader = new FileReader(settings_file);
            while((settings_file_reader.read(settings_char)) != -1) {}
            pair_array = String.valueOf(settings_char).split(";");
        }catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        String[] tickers_array = getIntent().getExtras().getStringArray("tickers_array");
        int j = 0, k = 0;
        for(int i = 0; i < tickers_array.length-1; i+=19) {
            CheckBox chkbox_N = new CheckBox(this);
            if  (pair_array.length > j) {
                if (pair_array[j].equals(tickers_array[i])) {
                    chkbox_N.setChecked(true);
                    j++;
                }
            }
            chkbox_N.setText(tickers_array[i]);
            chkbox_N.setId(k);
            chkbox_array.add(k,chkbox_N);
            l_Layout.addView(chkbox_N);
            k++;
        }

    }

    //_________________________________________________________________________________________________________________
    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String text = new String();
            switch (v.getId()) {
                case R.id.save_button: { //нажатие на кнопку сохранить
                    for(int i = 0; i < chkbox_array.size(); i++) {
                        if (chkbox_array.get(i).isChecked()) {text += chkbox_array.get(i).getText() + ";";}
                    }
                    //сохранение настроек с валютными парами в файл
                    File settings_file = new File(SettingsActivity.super.getFilesDir(),"exmo_kurs_settings.txt");
                    try {
                        FileWriter settings_file_writer = new FileWriter(settings_file, false);
                        String vyvod = text;
                        settings_file_writer.write(vyvod);
                        settings_file_writer.flush();
                    }
                    catch(IOException ex){
                        System.out.println(ex.getMessage());
                    }
                    finish();
                    break;
                }
            }
        }
    };

}

package com.view.anymo.anymowheelview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        VWheelView vWheelView = findViewById(R.id.vwv);
        HWheelView hWheelView  = findViewById(R.id.hwv);
        ArrayList<String> dataList = new ArrayList<>();
        for (int i = 0; i < 100; i++)
            dataList.add("" + i);
        vWheelView.setDataList(dataList);
        hWheelView.setDataList(dataList);
    }
}

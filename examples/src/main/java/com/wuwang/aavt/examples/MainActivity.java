package com.wuwang.aavt.examples;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.mMp4Process:
                startActivity(new Intent(this,ExampleMp4ProcessActivity.class));
                break;
            case R.id.mCameraRecord:
                startActivity(new Intent(this,CameraRecorderActivity.class));
                break;
            case R.id.mYuvExport:
                startActivity(new Intent(this,YuvExportActivity.class));
                break;
        }
    }
}

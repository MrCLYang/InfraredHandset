package com.zt.infraredhandset;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class InfraredLoginActivity extends AppCompatActivity {
    private EditText userId;
    private Button landing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initview();//初始化页面
        initevent();//初始化事件

    }

    private void initevent() {

        landing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String UserID = userId.getText().toString();//UserID
                Intent intent = new Intent(InfraredLoginActivity.this, InfraredVerificationActivity.class);
                intent.putExtra("UserID", UserID);//传递UserID数据
                startActivity(intent);
            }
        });

    }

    //初始化控件页面
    private void initview() {
        setContentView(R.layout.activity_infrared_login);
        getSupportActionBar().hide();
        userId = findViewById(R.id.et_UserID);
        landing = findViewById(R.id.bt_Landing);//登陆事件按钮
    }
}

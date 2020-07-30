package com.zt.infraredhandset;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.zt.infraredhandset.Bean.TransparentMeterDataBean;
import com.zt.infraredhandset.Bluetooth.BluetoothLeService;
import com.zt.infraredhandset.FrameUtils.CompositionFrame;
import com.zt.infraredhandset.FrameUtils.DataUtils;
import com.zt.infraredhandset.FrameUtils.PrefUtils;
import com.zt.infraredhandset.HttpCn.APIServer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;


import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class InfraredControllActivity extends AppCompatActivity implements View.OnClickListener {

    private String token;
    private String a10_key;
    private TextView tv_a10key;
    private TextView tv_token;
    private EditText et_meter;
    private Button infrared_communication_open;
    private Button infrared_communication_close;
    private Button infrared_close_valve_LockMeter;
    private Button infrared_close_valve_unLockMeter;
    private Button infrared_open_valve;
    private CompositionFrame cf;
    private TextView tv_infrared_communication_open;
    private String baseUrl = "http://223.71.48.53:19092/";

    private mGattUpdateReceiver gattUpdateReceiver;
    private mServiceConnection serviceConnection;
    private Intent gattServiceIntent;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mWriteCharateristic;
    private TextView tv_infrared_status;
    private TextView tv_bt_status;
    private TextView tv_bef_register_up;
    private TextView tv_register_up;
    private TextView tv_register_down;
    private TextView tv_register_down_toMeter;
    private TextView tv_meter_information_toSystem;
    private String gasMeterNum;
    private TextView tv_closeValveNoLockMeter;
    private TextView tv_unLock;
    private TextView tv_closeValveLockMeter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initview();
        gattServiceIntent = new Intent(this, BluetoothLeService.class);
        gattUpdateReceiver = new mGattUpdateReceiver();
        serviceConnection = new mServiceConnection();
    }

    private class mServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            displayGattServices(mBluetoothLeService.getSupportedGattServices());
            if (!mBluetoothLeService.initialize()) {
                Log.i("蓝牙", "onServiceConnected: " + "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.connect((String) PrefUtils.get(getApplicationContext(), "addr", "50:33:8B:5A:99:73"));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;

    }

    private class mGattUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                tv_bt_status.setText("蓝牙连接成功");
                mBluetoothLeService.discoverServices();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                tv_bt_status.setText("蓝牙连接失败");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
              /*  Toast.makeText(getApplicationContext(), "蓝牙已断开", Toast.LENGTH_LONG).show();*/
                Log.e("lanya jieshou", "DeviceControlActivity Discovered");
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String stringExtra = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                Log.i("YT", "stringExtra: " + stringExtra);
                Message msg = handler.obtainMessage();
                //透传给表返回的信息
                if (stringExtra.substring(0, 4).equals("69b5") && stringExtra.substring(12, 14).equals("0f")) {
                    System.out.println("红外" + stringExtra);
                    //红外已经连接
                    tv_infrared_status.setText("红外连接成功");
                }
                if (stringExtra.substring(0, 4).equals("69b5") && stringExtra.substring(12, 14).equals("4c")) {
                    msg.what = 1;
                    msg.obj = stringExtra;
                    handler.sendMessage(msg);
                }
                if (stringExtra.substring(0, 4).equals("69b5") && stringExtra.substring(12, 14).equals("84")) {
                    msg.what = 2;
                    msg.obj = stringExtra;
                    handler.sendMessage(msg);
                    Log.i("yangchenglei_str", "onReceive: " + stringExtra);
                }
                //TODO 此为关阀不锁表，关阀解表，关阀锁表后返回的帧
                if(stringExtra.substring(0, 4).equals("69b5") && stringExtra.substring(12, 14).equals("24")){

                    msg.what=3;
                    msg.obj=stringExtra;
                    handler.sendMessage(msg);
                }

            }
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        private JSONObject object;

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 1:
                    String content_1 = (String) msg.obj;
                    Log.i("content", "handleMessage——: " + content_1);

                    //透传前的注册上行
                    tv_bef_register_up.setText(content_1);
                    String str = content_1.substring(14, content_1.length() - 4);
                    //注册上行
                    tv_register_up.setText(str);
                    object = new JSONObject();
                    try {
                        object.put("meterId", gasMeterNum);
                        object.put("payload", str);
                    } catch (JSONException e) {
                        System.out.println("json 异常");
                        e.printStackTrace();
                    }
                    String json = object.toString();
                    //使用代码的方式添加header，运用拦截器
                    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
                    httpClient.addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            Request request = original.newBuilder()
                                    .header("Accept", "application/json")
                                    .header("Content-Type", "application/json")
                                    .header("a10_KEY", a10_key)
                                    .method(original.method(), original.body())
                                    .build();
                            return chain.proceed(request);
                        }
                    });
                    OkHttpClient client = httpClient.build();
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(client)
                            .build();
                    APIServer server = retrofit.create(APIServer.class);
                    final RequestBody body = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"), json);
                    Call<TransparentMeterDataBean> getmeter = server.getmeter(body);
                    getmeter.enqueue(new retrofit2.Callback<TransparentMeterDataBean>() {
                        @Override
                        public void onResponse(Call<TransparentMeterDataBean> call, retrofit2.Response<TransparentMeterDataBean> response) {
                            //接收到信息成功
                            String payload_down = response.body().getResult().getPayload();
                            Log.i("payload_down", "handleMessage: _" + payload_down);
                            //注册下行
                            tv_register_down.setText(payload_down);
                            //透传给表的注册下行
                            String RegisterdownToMeter = cf.GetRegisterdownToMeter(payload_down);
                            tv_register_down_toMeter.setText(RegisterdownToMeter);
                            TransmitFrame(RegisterdownToMeter);
                        }

                        @Override
                        public void onFailure(Call<TransparentMeterDataBean> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), "取注册认证下行失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
                case 2:
                    String content_2 = (String) msg.obj;
                    String GasMeterMessageUp = content_2.substring(14, content_2.length() - 4);
                    //表概信息展示
                    Log.i("GasMeterMessageUp", "handleMessage: " + GasMeterMessageUp);
                    tv_meter_information_toSystem.setText(GasMeterMessageUp);
                    //表概信息上传到系统
                    object = new JSONObject();
                    try {
                        object.put("meterId", gasMeterNum);
                        object.put("payload", GasMeterMessageUp);
                    } catch (JSONException e) {
                        System.out.println("json 异常");
                        e.printStackTrace();
                    }
                    String json_02 = object.toString();
                    //使用代码的方式添加header，运用拦截器
                    OkHttpClient.Builder httpClient_02 = new OkHttpClient.Builder();
                    httpClient_02.addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            Request request = original.newBuilder()
                                    .header("Accept", "application/json")
                                    .header("Content-Type", "application/json")
                                    .header("a10_KEY", a10_key)
                                    .method(original.method(), original.body())
                                    .build();
                            return chain.proceed(request);
                        }
                    });
                    OkHttpClient client_02 = httpClient_02.build();
                    Retrofit retrofit_02 = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(client_02)
                            .build();
                    APIServer server_02 = retrofit_02.create(APIServer.class);
                    RequestBody body_02 = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"), json_02);
                    Call<TransparentMeterDataBean> getmeter_02 = server_02.getmeter(body_02);
                    getmeter_02.enqueue(new retrofit2.Callback<TransparentMeterDataBean>() {
                        @Override
                        public void onResponse(Call<TransparentMeterDataBean> call, retrofit2.Response<TransparentMeterDataBean> response) {
                            //上传成功后
                            String result = response.body().getMsg();
                            if (result.equals("Success")) {
                                Toast.makeText(getApplicationContext(), "本地通信激活成功，可以进行表具操作", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<TransparentMeterDataBean> call, Throwable t) {

                        }
                    });
                    break;
                    //上传系统关阀不锁表成功后的帧
                case 3:
                    String content_3= (String) msg.obj;
                    String MeterRebackMessage = content_3.substring(14, content_3.length() - 4);
                    try {
                        object.put("meterId", gasMeterNum);
                        object.put("payload", MeterRebackMessage);
                    } catch (JSONException e) {
                        Log.i("InfraredHandset", "handleMessage: "+"Json组装失败");
                        e.printStackTrace();
                    }
                    String json_03 = object.toString();
                    OkHttpClient.Builder httpClient_03 = new OkHttpClient.Builder();
                    httpClient_03.addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            Request request = original.newBuilder()
                                    .header("Accept", "application/json")
                                    .header("Content-Type", "application/json")
                                    .header("a10_KEY", a10_key)
                                    .method(original.method(), original.body())
                                    .build();
                            return chain.proceed(request);
                        }
                    });
                    OkHttpClient client_03 = httpClient_03.build();
                    Retrofit retrofit_03 = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(client_03)
                            .build();
                    APIServer server_03 = retrofit_03.create(APIServer.class);
                    RequestBody body_03 = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"), json_03);
                    Call<TransparentMeterDataBean> getmeter_03 = server_03.getmeter(body_03);
                    getmeter_03.enqueue(new retrofit2.Callback<TransparentMeterDataBean>() {
                        @Override
                        public void onResponse(Call<TransparentMeterDataBean> call, retrofit2.Response<TransparentMeterDataBean> response) {
                            if(response.body().getMsg().equals("Success")){
                                Toast.makeText(getApplicationContext(), "对表操作成功", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<TransparentMeterDataBean> call, Throwable t) {

                        }
                    });
                    break;

                default:
                    break;
            }
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            return;
        }
        if (mBluetoothLeService != null) {
            mBluetoothLeService.setServives();
            mWriteCharateristic = mBluetoothLeService.mWriteCharateristic;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("213", "断开");
        unbindService(serviceConnection);
        unregisterReceiver(gattUpdateReceiver);
        mBluetoothLeService = null;
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        registerReceiver(new mGattUpdateReceiver(), makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            mBluetoothLeService.connect((String) PrefUtils.get(getApplicationContext(), "addr", "50:33:8B:5A:99:73"));
        }
    }


    private void initview() {
        setContentView(R.layout.activity_infrared_controll);
        getSupportActionBar().hide();
        Intent intent = this.getIntent();
        //获取传递过来的token和a10_key
        token = intent.getStringExtra("Token");
        a10_key = intent.getStringExtra("a10_key");
        tv_a10key = findViewById(R.id.tv_a10key);
        tv_token = findViewById(R.id.tv_token);
        tv_a10key.setText(a10_key);
        tv_token.setText(token);
        et_meter = findViewById(R.id.et_Meter);

        infrared_communication_open = findViewById(R.id.bt_infrared_communication_open);
        infrared_communication_open.setOnClickListener(this);
        infrared_communication_close = findViewById(R.id.bt_infrared_communication_close);
        infrared_communication_close.setOnClickListener(this);
        infrared_close_valve_LockMeter = findViewById(R.id.bt_infrared_close_valve_LockMeter);
        infrared_close_valve_LockMeter.setOnClickListener(this);
        infrared_close_valve_unLockMeter = findViewById(R.id.bt_infrared_close_valve_UnLockMeter);
        infrared_close_valve_unLockMeter.setOnClickListener(this);
        infrared_open_valve = findViewById(R.id.bt_infrared_open_valve);
        infrared_open_valve.setOnClickListener(this);
        cf = new CompositionFrame(this);
        tv_infrared_status = findViewById(R.id.tv_infrared_status);
        tv_bt_status = findViewById(R.id.tv_bt_status);

        //本地通信激活展示帧
        tv_infrared_communication_open = findViewById(R.id.tv_infrared_communication_open);

        //透传前的注册上行
        tv_bef_register_up = findViewById(R.id.tv_Bef_Register_up);
        //注册上行
        tv_register_up = findViewById(R.id.tv_Register_up);
        //注册下行
        tv_register_down = findViewById(R.id.tv_Register_down);
        //传给表的注册下行
        tv_register_down_toMeter = findViewById(R.id.tv_Register_down_toMeter);
        //表概信息上传
        tv_meter_information_toSystem = findViewById(R.id.tv_Meter_information_toSystem);
        //关阀不锁表的指令显示
        tv_closeValveNoLockMeter = findViewById(R.id.tv_CloseValveNoLockMeter);
        //开阀指令显示
        tv_unLock = findViewById(R.id.tv_UnLock);
        //关阀锁表指令
        tv_closeValveLockMeter = findViewById(R.id.tv_CloseValveLockMeter);

    }

    private Long delayMills = 66L;

    //向蓝牙发送帧
    private void TransmitFrame(String zhen) {
        int i1 = (zhen.length() / 40) + 1;
        String substring = "";
        for (int i = 0; i < i1; i++) {
            if (i < i1 - 1) {
                substring = zhen.substring(i * 40, (i + 1) * 40);
            } else {
                substring = zhen.substring(i * 40, zhen.length());
            }

            byte[] offrx1 = new byte[substring.length() / 2];

            DataUtils.hexStringToByte(substring, offrx1);
            Log.e("123", "onClick: " + DataUtils.bytesToHexString(offrx1));
            mBluetoothLeService.mWriteCharateristic.setValue(offrx1);
            mBluetoothLeService.writeCharacteristic(mBluetoothLeService.mWriteCharateristic);

            try {
                Thread.sleep(delayMills);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            //红外通信激活
            case R.id.bt_infrared_communication_open:
                String ControllType_Open = "01";//本地通信激活开通功能码
                //表号
                gasMeterNum = et_meter.getText().toString().trim();
                String localCommunication_open = cf.GetLocalCommunication(gasMeterNum, token, ControllType_Open);
                Log.i("localCommunication", "onClick: " + localCommunication_open);
                //本地通信激活帧
                tv_infrared_communication_open.setText(localCommunication_open);
                //TODO 本地通信激活帧的发送
                TransmitFrame(localCommunication_open);
                break;
            //红外通信关闭
            case R.id.bt_infrared_communication_close:
                tv_infrared_status.setText("红外连接关闭");
                String ControllType_Close="02";
                String localCommunication_close = cf.GetLocalCommunication(gasMeterNum, token, ControllType_Close);
                //TODO 关闭本地通信激活
                TransmitFrame(localCommunication_close);
                tv_infrared_status.setText("红外状态");
                break;
            //关阀锁表的操作
            case R.id.bt_infrared_close_valve_LockMeter:
            //TODO 开始做关阀锁表操作
               JSONObject object_closevalve_lockmeter=new JSONObject();
                try {
                    object_closevalve_lockmeter.put("meterId", gasMeterNum);
                    object_closevalve_lockmeter.put("opt", "LOCK");
                } catch (JSONException e) {
                    System.out.println("json 组装失败");
                    e.printStackTrace();
                }
                String json_closevalve_lockmeter = object_closevalve_lockmeter.toString();
                OkHttpClient.Builder httpClient_closevalve_lockmeter = new OkHttpClient.Builder();
                httpClient_closevalve_lockmeter.addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Accept", "application/json")
                                .header("Content-Type", "application/json")
                                .header("a10_KEY", a10_key)
                                .method(original.method(), original.body())
                                .build();
                        return chain.proceed(request);
                    }
                });
                OkHttpClient client_closevalve_lockmeter = httpClient_closevalve_lockmeter.build();
                Retrofit retrofit_closevalve_lockmeter = new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(client_closevalve_lockmeter)
                        .build();
                APIServer server_closevalve_lockmeter = retrofit_closevalve_lockmeter.create(APIServer.class);
                RequestBody body_closevalve_lockmeter = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"), json_closevalve_lockmeter);
                Call<TransparentMeterDataBean> CloseValveLockMeter = server_closevalve_lockmeter.OpenOrCloseMeter(body_closevalve_lockmeter);
                CloseValveLockMeter.enqueue(new Callback<TransparentMeterDataBean>() {
                    @Override
                    public void onResponse(Call<TransparentMeterDataBean> call, retrofit2.Response<TransparentMeterDataBean> response) {
                        //获取到关阀锁表的指令码
                        String payload = response.body().getResult().getPayload();
                        Log.i("closeValveLockMeter", "onResponse: "+payload);
                        tv_closeValveLockMeter.setText(payload);
                        String LOCK = cf.GetLOCK(payload);
                        //发送锁表关阀指令
                        TransmitFrame(LOCK);
                    }

                    @Override
                    public void onFailure(Call<TransparentMeterDataBean> call, Throwable t) {

                    }
                });

                break;
            //关阀不锁表
            case R.id.bt_infrared_close_valve_UnLockMeter:
                //TODO 开始做关阀操作，由于本地通信激活，可以向系统上发关阀不锁表的信息
                JSONObject object = new JSONObject();
                try {
                    object.put("meterId", gasMeterNum);
                    object.put("opt", "HALFLOCK");
                } catch (JSONException e) {
                    System.out.println("json 组装失败");
                    e.printStackTrace();
                }
                String json = object.toString();
                OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
                httpClient.addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Accept", "application/json")
                                .header("Content-Type", "application/json")
                                .header("a10_KEY", a10_key)
                                .method(original.method(), original.body())
                                .build();
                        return chain.proceed(request);
                    }
                });
                OkHttpClient client = httpClient.build();
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(client)
                        .build();
                APIServer server = retrofit.create(APIServer.class);
                RequestBody body = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"), json);
                Call<TransparentMeterDataBean> openOrCloseMeter = server.OpenOrCloseMeter(body);
                openOrCloseMeter.enqueue(new Callback<TransparentMeterDataBean>() {
                    @Override
                    public void onResponse(Call<TransparentMeterDataBean> call, retrofit2.Response<TransparentMeterDataBean> response) {
                        //获取到关阀不锁表的payload
                        String payload = response.body().getResult().getPayload();
                        Log.i("closevalveUnlockMeter", "onResponse: "+payload);
                        tv_closeValveNoLockMeter.setText(payload);
                        String HALFLOCKtoMeter = cf.GetHALFLOCKtoMeter(payload);
                        Log.i("HALFLOCKtoMeter", "onResponse: "+HALFLOCKtoMeter);
                        //实现关阀不锁表的操作
                        TransmitFrame(HALFLOCKtoMeter);
                    }

                    @Override
                    public void onFailure(Call<TransparentMeterDataBean> call, Throwable t) {

                    }
                });

                break;
            //开阀解锁
            case R.id.bt_infrared_open_valve:
                //TODO 开始做开阀解锁操作
                JSONObject object_open_valve = new JSONObject();
                try {
                    object_open_valve.put("meterId", gasMeterNum);
                    object_open_valve.put("opt", "UNLOCK");
                } catch (JSONException e) {
                    System.out.println("json 组装失败");
                    e.printStackTrace();
                }
                String json_open_valve = object_open_valve.toString();
                OkHttpClient.Builder httpClient_open_valve = new OkHttpClient.Builder();
                httpClient_open_valve.addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Accept", "application/json")
                                .header("Content-Type", "application/json")
                                .header("a10_KEY", a10_key)
                                .method(original.method(), original.body())
                                .build();
                        return chain.proceed(request);
                    }
                });
                OkHttpClient client_open_valve = httpClient_open_valve.build();
                Retrofit retrofit_open_valve = new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(client_open_valve)
                        .build();
                APIServer server_open_valve = retrofit_open_valve.create(APIServer.class);
                RequestBody body_open_valve = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"), json_open_valve);
                Call<TransparentMeterDataBean> openOrCloseMeter_open_valve = server_open_valve.OpenOrCloseMeter(body_open_valve);
                openOrCloseMeter_open_valve.enqueue(new Callback<TransparentMeterDataBean>() {
                    @Override
                    public void onResponse(Call<TransparentMeterDataBean> call, retrofit2.Response<TransparentMeterDataBean> response) {
                        //获取到开阀解锁的指令
                        String payload = response.body().getResult().getPayload();
                        Log.i("OpenValve", "onResponse: "+payload);
                        tv_unLock.setText(payload);
                        String UNLOCK = cf.GetUNLOCK(payload);
                        //开阀指令
                        TransmitFrame(UNLOCK);
                    }

                    @Override
                    public void onFailure(Call<TransparentMeterDataBean> call, Throwable t) {

                    }
                });
                break;
            default:
                break;

        }
    }


}

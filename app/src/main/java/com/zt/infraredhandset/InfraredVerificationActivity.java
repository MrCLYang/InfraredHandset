package com.zt.infraredhandset;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.app.AlertDialog;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zt.infraredhandset.Bean.RebackDataBean;
import com.zt.infraredhandset.Bluetooth.BluetoothLeService;
import com.zt.infraredhandset.FrameUtils.CompositionFrame;
import com.zt.infraredhandset.FrameUtils.DataUtils;
import com.zt.infraredhandset.FrameUtils.PrefUtils;
import com.zt.infraredhandset.HttpCn.APIServer;
import com.zt.infraredhandset.RecycleView.BluetoothAdapter;
import com.zt.infraredhandset.RecycleView.OnItemClickListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class InfraredVerificationActivity extends AppCompatActivity implements View.OnClickListener {

    private Button bt_search;
    private RecyclerView recyclerView;
    private AlertDialog alertDialog;
    private TextView booth_name;

    private String userId;
    private CompositionFrame cf;
    private TextView tv_landingMessage;
    private Button bt_getA10key;
    private Button bt_getToken;

    //RecycleView的adapter
    private BluetoothAdapter adapter;

    //蓝牙自带的adapter
    private List<BluetoothDevice> data;
    private android.bluetooth.BluetoothAdapter bluetoothAdapter;
    private android.bluetooth.BluetoothAdapter.LeScanCallback scanleBlue;

    private mGattUpdateReceiver gattUpdateReceiver;
    private mServiceConnection serviceConnection;
    private Intent gattServiceIntent;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mWriteCharateristic;

    private String address;
    private String name;

    private String landingframe;
    private TextView tv_landingAuthenticationData;
    private TextView tv_authenticationUpData;

    //采集系统的URL
    private String baseUrl = "http://223.71.48.53:19092/";
    private TextView tv_a10key;
    private TextView tv_token;
    private TextView tv_authenticationDownData;
    private String decryptToken;
    private String a10_key;
    @SuppressLint("HandlerLeak")
    private Handler handler;
    private TextView tv_lora;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initview();//初始化控件
        initdata();//初始化数据
        gattServiceIntent = new Intent(this, BluetoothLeService.class);
        gattUpdateReceiver = new mGattUpdateReceiver();
        serviceConnection = new mServiceConnection();
        initevent();//初始化事件

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
/*
            mBluetoothLeService.connect((String) PrefUtils.get(getApplicationContext(), "addr", "50:33:8B:5A:99:73"));
*/
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
                Toast.makeText(getApplicationContext(), "蓝牙连接成功", Toast.LENGTH_SHORT).show();
                System.out.println("蓝牙连接成功");
                mBluetoothLeService.discoverServices();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "蓝牙连接失败", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if(mBluetoothLeService!=null) {
                    Log.e("lanya jieshou", "DeviceControlActivity Discovered");
                   /* Toast.makeText(getApplicationContext(), "蓝牙已断开", Toast.LENGTH_LONG).show();*/
                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String stringExtra = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                System.out.println("YCL" + stringExtra);
                Message msg = Myhandler.obtainMessage();

                //注册信息返回信息
                if (stringExtra.substring(0, 4).equals("69a1")) {
                    msg.obj = stringExtra;
                    msg.what = 11;
                    Myhandler.sendMessage(msg);
                } else if (stringExtra.substring(0, 4).equals("69a2")) {//Token解密返回信息
                    msg.obj = stringExtra;
                    msg.what = 12;
                    Myhandler.sendMessage(msg);
                } else {
                    msg.obj = stringExtra;
                    msg.what = 13;
                    Myhandler.sendMessage(msg);
                }

            }
        }
    }

    @SuppressLint("HandlerLeak")
    public Handler Myhandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            String content = msg.obj.toString();
            switch (msg.what) {
                case 11:
                    tv_landingAuthenticationData.setText(content);
                    String substring = content.substring(14, content.length() - 4);
                    String LandingauthenticationUp = cf.GetLandingauthenticationUp(substring);
                    //展示登陆认证上行数据
                    tv_authenticationUpData.setText(LandingauthenticationUp);
                    Log.i("登陆认证上行数据", "handleMessage: " + LandingauthenticationUp);
                    //TODO 以上数据完成，现在实现登陆认证下行
                    //将上述的登陆认证上行数据实现json格式和采集系统通信
                    JSONObject object = new JSONObject();
                    try {
                        object.put("payload", LandingauthenticationUp);
                    } catch (JSONException e) {
                        System.out.println("json 异常");
                        e.printStackTrace();
                    }
                    //组68的payload值，获取A10Key
                    String json = object.toString();
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    //创建api接口服务的实体类
                    APIServer server = retrofit.create(APIServer.class);
                    RequestBody body = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"), json);
                    Call<RebackDataBean> getlogin = server.getlogin(body);
                    getlogin.enqueue(new retrofit2.Callback<RebackDataBean>() {
                        @Override
                        public void onResponse(Call<RebackDataBean> call, Response<RebackDataBean> response) {
                            //请求成功获取到数据
                            //获取到a10_key
                            a10_key = response.body().getResult().getA10_KEY();
                            tv_a10key.setText(a10_key);
                            //登陆认证数据下行的payload，发给PSAM卡解密获取token
                            String payload = response.body().getResult().getPayload();
                            Log.i("payload", "onResponse: " + payload);
                            //登陆认证数据下行展示
                            tv_authenticationDownData.setText(payload);
                            //数据长度和数据域
                            String Str = payload.substring(4, payload.length());
                            decryptToken = cf.DecryptToken(Str);
                        }

                        @Override
                        public void onFailure(Call<RebackDataBean> call, Throwable t) {
                            //请求失败
                            Log.i("Internet failin connect", "onFailure: " + "请求失败");
                        }
                    });


                    break;
                case 12:
                    Log.i("content_token", "handleMessage: " + content);
                    //69a284348b810d810a00000000000000111111009a16
                    //此为测试端口从14位开始，正式端口从16开始
                    if (content.substring(16, 18).equals("81")) {
                        if (content.substring(18, 20).equals("0a")) {
                            if (content.substring(20, 22).equals("00")) {
                                if (content.substring(22, 24).equals("00")) {
                                    final String Token = content.substring(24, content.length() - 4);
                                    tv_token.setText(Token);
                                    //TODO 以上的动作代码正常
                                    Intent intent = new Intent(InfraredVerificationActivity.this, InfraredControllActivity.class);
                                    intent.putExtra("Token", Token);
                                    intent.putExtra("a10_key", a10_key);
                                    startActivity(intent);
                                } else {
                                    Log.i("Token_04", "登陆认证失败");
                                }
                            } else {
                                Log.i("Token_03", "注册不成功！");
                            }

                        } else {
                            Log.i("Token_02", "后续的数据长度有误");
                        }

                    } else {
                        Log.i("Toeken_01", "功能控制字不为81！");
                    }

                    break;
                case 13:
                    System.out.println("version"+content);
                    String loarversion = content.substring(12, 14);
                    tv_lora.setText("固件版本号：1."+loarversion);
                    break;
                default:
                    break;
            }

        }
    };



    private void initdata() {
        data = new ArrayList<>();
        address = (String) PrefUtils.get(getApplicationContext(), "addr", "50:33:8B:5A:99:73");
        name = (String) PrefUtils.get(getApplicationContext(), "name", "未匹配");
        booth_name.setText(name);
        //设备蓝牙标志是否开始
        initBooth();
        //搜索蓝牙的回调方法
        scanleBlue = new android.bluetooth.BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if (!data.contains(device)) {
            data.add(device);
            adapter.notifyDataSetChanged();
        }
    }

};

    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            return;
        }
        if (mBluetoothLeService != null) {
            mBluetoothLeService.setServives();
            mWriteCharateristic = mBluetoothLeService.mWriteCharateristic;
        }

    }


    private void initview() {
        setContentView(R.layout.activity_infrared_verification);
        getSupportActionBar().hide();
        Intent intent = this.getIntent();
        userId = intent.getStringExtra("UserID");
        tv_landingMessage = findViewById(R.id.tv_LandingMessage);

        bt_search = findViewById(R.id.bt_search);
        bt_search.setOnClickListener(this);
        bt_getA10key = findViewById(R.id.bt_GetA10Key);
        bt_getA10key.setOnClickListener(this);
        bt_getToken = findViewById(R.id.bt_GetToken);
        bt_getToken.setOnClickListener(this);
        booth_name = findViewById(R.id.booth_name);

        tv_landingAuthenticationData = findViewById(R.id.tv_LandingAuthenticationData);
        tv_authenticationUpData = findViewById(R.id.tv_AuthenticationUpData);

        tv_a10key = findViewById(R.id.tv_a10key);
        tv_token = findViewById(R.id.tv_token);

        tv_authenticationDownData = findViewById(R.id.tv_AuthenticationDownData);
        //底座蓝牙版本
        tv_lora = findViewById(R.id.tv_Lora);
        Button bt_GetLora=findViewById(R.id.bt_GetLora);
        bt_GetLora.setOnClickListener(this);



    }

    //判断蓝牙是否打开
    private void initBooth() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return;
        }
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager.getAdapter();
        if (bluetoothAdapter == null) {
            return;
        }
        bluetoothAdapter.enable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
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

    private void initevent() {
        cf = new CompositionFrame(this);
        //登陆信息帧，发送给PSAM卡
        landingframe = cf.LandingFrame(userId);
        //展示登陆信息
        tv_landingMessage.setText(landingframe);

    }
    //开始扫描，7秒后自动停止
    private  void scanDevice(){
        bluetoothAdapter.startLeScan(scanleBlue);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.stopLeScan(scanleBlue);
            }
        },7000);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_search:
                data.clear();
                scanDevice();
                //当采用其他xml的文件中控件的时候需要找到那个页面才能使用那个控件。
                View inflate = View.inflate(getApplicationContext(), R.layout.bluetoothlist, null);
                recyclerView = inflate.findViewById(R.id.rv_bluetooth_listview);
                LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
                recyclerView.setLayoutManager(layoutManager);
                adapter = new BluetoothAdapter(data);
                recyclerView.setAdapter(adapter);

                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle("选择蓝牙")
                        .setView(inflate);
                alertDialog = builder.create();
                alertDialog.show();

                adapter.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        booth_name.setText(data.get(position).getName());
                        PrefUtils.put(getApplicationContext(), "name", data.get(position).getName());
                        PrefUtils.put(getApplicationContext(), "addr", data.get(position).getAddress());
                        //点击所选蓝牙后连接上蓝牙
                        mBluetoothLeService.connect((String) PrefUtils.get(getApplicationContext(), "addr", "50:33:8B:5A:99:73"));
                        alertDialog.dismiss();
                    }

                    @Override
                    public void onClick(View v) {

                    }
                });
                break;

            case R.id.bt_GetA10Key:
                TransmitFrame(landingframe);
                break;
            case R.id.bt_GetToken:
                TransmitFrame(decryptToken);
                break;
            case R.id.bt_GetLora:
                String loraVersion = cf.GetLoraVersion();
                TransmitFrame(loraVersion);
                break;
            default:
                break;

        }
    }
}




package com.zt.infraredhandset.FrameUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;

import retrofit2.Retrofit;

/**
 * Time:2019/10/30
 * Author:YCL
 * Description:组成帧的公用类
 */
public class CompositionFrame {

    //需要回加密数据
    private String FucodeToBle_31 = "31";
    //需要回解密数据
    private String FucodeToBle_32 = "32";
    //直接透传给表
    private String FucodeToBle_33 = "33";
    //加密后透传给表
    private String FucodeToBle_34 = "34";
    //读取表数据
    private String FucodeToBle_35 = "35";

    private Context mContext;
    //向蓝牙发送的帧头
    private String HeadsToBLe = "69";
    //  向系统发送的帧头
    private String HeadsToSystem = "68";

    //功能码
    private String A10key_Fucode = "843208c1";
    private String Token_Fucode = "84348b81";
    private String Default_Fucode = "00000000";

    //终端型号编码
    private String ModelCode = "0001";
    //终端固件版本号
    private String Firmware_VersionNum = "01";
    //终端软件版本号
    private String Software_VersionNum = "0001";

    //结束码
    private String FrameEnd = "16";

    public CompositionFrame(Context context) {
        mContext = context;
    }

    /**
     * @date:2019/10/30
     * @Author:yangchenglei
     * @description: 获取手机IMEI
     */
    private static String getIMEI(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        @SuppressLint("MissingPermission") String IMEI = tm.getDeviceId();
        return "0" + IMEI;
    }

    /**
     * @date:2019/10/30
     * @Author:yangchenglei
     * @description: 获取手机IMSI
     */
    private static String getIMSI(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        @SuppressLint("MissingPermission") String IMSI = tm.getSubscriberId();
        return "0" + IMSI;
    }

    /**
     * @date:2019/10/30
     * @Author:yangchenglei
     * @description: 登陆信息
     */
    private String GetLandingMessage(String UserID) {
        String IMEI = getIMEI(mContext);//获取到0+手机的IMEI
        String IMSI = "1234123412341234";
        String LandMessage = ModelCode + Firmware_VersionNum + Software_VersionNum + UserID + IMEI + IMSI;
        return LandMessage;
    }

    /**
     * @date:2019/10/30
     * @Author:yangchenglei
     * @description: 登陆信息帧，给PSAM卡发送
     */

    public String LandingFrame(String UserID) {

        String LandMessage = GetLandingMessage(UserID);//登陆信息
        String landingframeBefCs = HeadsToBLe + FucodeToBle_31 + A10key_Fucode + DataUtils.GetLen(LandMessage) + LandMessage;
        String landingframe = landingframeBefCs + DataUtils.GetCS(landingframeBefCs) + FrameEnd;
        return landingframe;
    }


    /**
     * @date:2019/11/4
     * @Author:yangchenglei
     * @description: 登陆认证上行，由PSAM卡返回数据组成68帧发送给采集系统
     */
    public String GetLandingauthenticationUp(String Rebackstr) {
        String Str = "0053" + "0200" + "01" + Rebackstr;
        String CRC_CODE = DataUtils.CRC_XMODEM_Str(Str);
        String JsonStr = HeadsToSystem + Str + CRC_CODE + FrameEnd;
        return JsonStr;
    }

    /**
     * @date:2019/11/5
     * @Author:yangchenglei
     * @description: 将登陆认证数据下行发给PSAM卡进行解密得到Token字符串
     */
    public String DecryptToken(String Str) {
        String DecryptTokenBefCS = HeadsToBLe + FucodeToBle_32 + Token_Fucode + Str;
        String DecryptToken = DecryptTokenBefCS + DataUtils.GetCS(DecryptTokenBefCS) + FrameEnd;
        return DecryptToken;
    }

    /**
     * @date:2019/11/5
     * @Author:yangchenglei
     * @description: 本地通信最里层基本数据组成方法，属于本地通信的第一层的包裹数据
     */
    private String GetLocalCommunication_B(String MeterNumber, String Token, String ControllType) {
        String LocalCommunication_B = ControllType + MeterNumber + Token;
        return LocalCommunication_B;

    }

    /**
     * @date:2019/11/5
     * @Author:yangchenglei
     * @description: 包裹着本地通信的最里层的基本数据的68帧，该帧是对表的操作，属于本地通信的第二层包裹数据帧
     */
    private String GetLocalCommunication_A(String LocalCommunication_B, String MeterNumber) {
        String StrBefCRC = "0021" + "0200" + MeterNumber + "06" + LocalCommunication_B;
        String LocalCommunication_A = HeadsToSystem + StrBefCRC + DataUtils.CRC_XMODEM_Str(StrBefCRC) + FrameEnd;
        return LocalCommunication_A;
    }

    /**
     * @date:2019/11/5
     * @Author:yangchenglei
     * @description:本地通信帧，其组成由最里层的B（数据）嵌套在A帧的里面，然后将A帧当作数据域组成向蓝牙传输的本地通信帧，算成是三层包裹
     */
    public String GetLocalCommunication(String MeterNumber, String Token, String ControllType) {
        String localCommunication_b = GetLocalCommunication_B(MeterNumber, Token, ControllType);
        String localCommunication_a = GetLocalCommunication_A(localCommunication_b, MeterNumber);
        String StrBefCs = HeadsToBLe + FucodeToBle_33 + Default_Fucode + "22" + localCommunication_a;
        String localCommunication = StrBefCs + DataUtils.GetCS(StrBefCs) + FrameEnd;
        return localCommunication;
    }

    /**
     * @date:2019/11/7
     * @Author:yangchenglei
     * @description: 透传注册认证的下行帧
     */
    public String GetRegisterdownToMeter(String str) {
        String StrBeforeCs = HeadsToBLe + FucodeToBle_33 + Default_Fucode + "2b" + str;
        String RegisterdownToMeter = StrBeforeCs + DataUtils.GetCS(StrBeforeCs) + FrameEnd;
        return RegisterdownToMeter;
    }

    /**
     * @date:2019/11/8
     * @Author:yangchenglei
     * @description:关阀不锁表的HALFLOCK帧
     */
    public String GetHALFLOCKtoMeter(String str) {
        String StrBefCs = HeadsToBLe + FucodeToBle_33 + Default_Fucode + DataUtils.GetLen(str) + str;
        String HALFLOCKtoMeter = StrBefCs + DataUtils.GetCS(StrBefCs) + FrameEnd;
        return HALFLOCKtoMeter;
    }

    /**
     * @date:2019/11/8
     * @Author:yangchenglei
     * @description: 开阀解锁的UNLOCK
     */
    public String GetUNLOCK(String str) {
        String StrBefCs = HeadsToBLe + FucodeToBle_33 + Default_Fucode + DataUtils.GetLen(str) + str;
        String UNLOCK = StrBefCs + DataUtils.GetCS(StrBefCs) + FrameEnd;
        return UNLOCK;
    }

    /**
    *@date:2019/11/8
    *@Author:yangchenglei
    *@description: 关阀锁表的LOCK
    */
    public String GetLOCK(String str) {
        String StrBefCs = HeadsToBLe + FucodeToBle_33 + Default_Fucode + DataUtils.GetLen(str) + str;
        String LOCK = StrBefCs + DataUtils.GetCS(StrBefCs) + FrameEnd;
        return LOCK;
    }

    /**
    *@date:2019/11/11
    *@Author:yangchenglei
    *@description:  获取底座的Lora的版本号
    */

    public String GetLoraVersion(){
        String StrBefCs=HeadsToBLe+FucodeToBle_34+Default_Fucode+"00";
        String LoraVersion=StrBefCs+DataUtils.GetCS(StrBefCs)+FrameEnd;
        return LoraVersion;
    }

}

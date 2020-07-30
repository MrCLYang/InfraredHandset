package com.zt.infraredhandset.FrameUtils;

import java.math.BigInteger;

/**
 * Time:2019/10/30
 * Author:YCL
 * Description:数据处理，校验工具类
 */
public class DataUtils {

    /**
     * @date:2019/9/29
     * @Author:yangchenglei
     * @description:16进制的字符串转换成16进制字符串数组
     */
    public static byte[] HexStringToBytes(String src) {
        int len = src.length() / 2;
        byte[] ret = new byte[len];
        byte[] tmp = src.getBytes();
        for (int i = 0; i < len; i++) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        }
        return ret;
    }

    public static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[]{src0})).byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[]{src1})).byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    /**
     * @date:2019/9/29
     * @Author:yangchenglei
     * @description: 普通字符串转16进制的字符串
     */
    public static String str2HexStr(String str) {
        byte[] bytes = str.getBytes();
        // 如果不是宽类型的可以用Integer
        BigInteger bigInteger = new BigInteger(1, bytes);
        return bigInteger.toString(16);
    }


    /**
     * @date:2019/9/29
     * @Author:yangchenglei
     * @description:CRC16_XMODEM的校验方法，传入的是16进制的字符串数组，得到的是一个10进制的整型数据 todo需要将得到10进制转成16进制。Integer.toHexString(i)
     */
    public static int CRC16_XMODEM(byte[] buffer) {
        /* StringUtil.getByteArrayByString();*/

        int wCRCin = 0x0000; // initial value 65535
        int wCPoly = 0x1021; // 0001 0000 0010 0001 (0, 5, 12)
        for (byte b : buffer) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((wCRCin >> 15 & 1) == 1);
                wCRCin <<= 1;
                if (c15 ^ bit)
                    wCRCin ^= wCPoly;
            }
        }
        wCRCin &= 0xffff;
        return wCRCin ^= 0x0000;
    }

    /**
     * @date:2019/10/30
     * @Author:yangchenglei
     * @description: 输入字符串，得到CRC的两个字节的校验码
     */
    public static String CRC_XMODEM_Str(String str) {

        byte[] bytes = HexStringToBytes(str);//16进制字符串转成16进制字符串数组
        int i = CRC16_XMODEM(bytes);//进行CRC—XMODEM校验得到十进制校验数
        String CRC = Integer.toHexString(i);//10进制转16进制
        return CRC;
    }

    /**
     * @date:2019/10/30
     * @Author:yangchenglei
     * @description: 获取长度转成16进制
     */
    public static String GetLen(String Str) {
        int length = Str.length();
        String Len = Integer.toHexString(length / 2);
        return Len;
    }


    /**
    *@date:2019/10/30
    *@Author:yangchenglei
    *@description: CS 校验
    */
    public static String GetCS(String str) {

        byte[] bytes = HexStringToBytes(str);
        byte b=0;
        for (int i = 0; i < bytes.length; i++) {
            b+= bytes[i];

        }

        return toHex(b);
    }
    public static String toHex(byte b) {
        String result = Integer.toHexString(b & 0xFF);
        if (result.length() == 1) {

            result = '0' + result;
        }
        return result;
    }

    /**
     * byte[]数组转化为16进制的字符串
     * @param src
     * @return
     */
    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;

            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    /**
    *@date:2019/11/4
    *@Author:yangchenglei
    *@description:将16进制的字符串转成自定义的byte数组，与上面的方法类似
    */
    public static void hexStringToByte(String hex,byte[] outData){
		/*if(hex == null || hex.equals("")){
			return null;
		}*/
        hex = hex.toUpperCase();
        int length = hex.length()/2;
        char[] hexChars = hex.toCharArray();
//		byte[] d = new byte[length];
        for(int i=0;i<length;i++){
            int pos = i*2;
            //d[i] = (byte)(hexToInt(hexChars[pos])<<4);
            //d[i+1]=(byte)hexToInt(hexChars[pos+1]);
            outData[i]=(byte)(hexToInt(hexChars[pos])<<4 | hexToInt(hexChars[pos+1]));
        }
    }
    public static int hexToInt(char ch)
    {
        if ('a' <= ch && ch <= 'f') { return ch - 'a' + 10; }
        if ('A' <= ch && ch <= 'F') { return ch - 'A' + 10; }
        if ('0' <= ch && ch <= '9') { return ch - '0'; }
        throw new IllegalArgumentException(String.valueOf(ch));
    }



}

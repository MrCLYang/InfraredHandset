package com.zt.infraredhandset.Bean;

/**
 * Time:2019/10/22
 * Author:YCL
 * Description:本地通信激活表透传给采集系统的数据返回
 */
public class TransparentMeterDataBean {
    /* {
        "msg": "Success",
            "code": 0,
            "result": {
        "meterId": "1561909000126117",
                "payload": "68002a01000108811ff0f2ffdd080100010001000000191022133226000000000011111198bf119ce9af16"
    }
    }*/

    /**
     * msg : Success
     * code : 0
     * result : {"meterId":"1561909000126117","payload":"68002a01000108811ff0f2ffdd080100010001000000191022133226000000000011111198bf119ce9af16"}
     */

    private String msg;
    private int code;
    private ResultBean result;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public ResultBean getResult() {
        return result;
    }

    public void setResult(ResultBean result) {
        this.result = result;
    }

    public static class ResultBean {
        /**
         * meterId : 1561909000126117
         * payload : 68002a01000108811ff0f2ffdd080100010001000000191022133226000000000011111198bf119ce9af16
         */

        private String meterId;
        private String payload;

        public String getMeterId() {
            return meterId;
        }

        public void setMeterId(String meterId) {
            this.meterId = meterId;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }
    }


}

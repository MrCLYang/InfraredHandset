package com.zt.infraredhandset.Bean;

/**
 * Time:2019/11/4
 * Author:YCL
 * Description:
 */
public class RebackDataBean {
    /**
     * msg : Success
     * code : 0
     * result : {"payload":"8b815c1c1a128d0801000100010000ae1748436c1d79d2ffe90f536cd50fbc41504a8aad1255ca78d9ee50ad421b892f865dfbb0e8a4bd9434515f9ae0c7c630334d5b3ec7f4113097630c0dbc426c91e7d2f5870f9094437eddddb0a4a34b",
     *              "a10_KEY":"0000000000000000"}
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
         * payload : 8b815c1c1a128d0801000100010000ae1748436c1d79d2ffe90f536cd50fbc41504a8aad1255ca78d9ee50ad421b892f865dfbb0e8a4bd9434515f9ae0c7c630334d5b3ec7f4113097630c0dbc426c91e7d2f5870f9094437eddddb0a4a34b
         * a10_KEY : 0000000000000000
         */

        private String payload;
        private String a10_KEY;

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public String getA10_KEY() {
            return a10_KEY;
        }

        public void setA10_KEY(String a10_KEY) {
            this.a10_KEY = a10_KEY;
        }
    }
}

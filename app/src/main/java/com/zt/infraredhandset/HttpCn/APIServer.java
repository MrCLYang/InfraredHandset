package com.zt.infraredhandset.HttpCn;

import com.zt.infraredhandset.Bean.RebackDataBean;
import com.zt.infraredhandset.Bean.TransparentMeterDataBean;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Time:2019/11/4
 * Author:YCL
 * Description:
 */
public interface APIServer {
    @Headers({"Accept:application/json","Content-Type:application/json"})
    @POST("a10service/pda/login")
    Call<RebackDataBean> getlogin(@Body RequestBody json);

    /* @Headers({"Accept:application/json","Content-Type:application/json","a10_KEY:0000000000000000"})*/
    @POST("a10service/pda/meter")
    Call<TransparentMeterDataBean>getmeter(@Body RequestBody json);

    @POST("a10service/pda/reqopt")
    Call<TransparentMeterDataBean>OpenOrCloseMeter(@Body RequestBody json);
}

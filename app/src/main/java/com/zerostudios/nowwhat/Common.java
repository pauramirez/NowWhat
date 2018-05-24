package com.zerostudios.nowwhat;


import com.zerostudios.nowwhat.Model.Results;
import com.zerostudios.nowwhat.Remote.IGoogleAPIService;
import com.zerostudios.nowwhat.Remote.RetrofitClient;
import com.zerostudios.nowwhat.Remote.RetrofitScalarsClient;

public class Common
{

    public static Results currentResult;


    private static final String GOOGLE_API_URL = "https://maps.googleapis.com/";

    public static IGoogleAPIService getGoogleAPIService()
    {
        return RetrofitClient.getClient(GOOGLE_API_URL).create(IGoogleAPIService.class);
    }

    public static IGoogleAPIService getGoogleAPIServiceScalars()
    {
        return RetrofitScalarsClient.getScalarClient(GOOGLE_API_URL).create(IGoogleAPIService.class);
    }

}

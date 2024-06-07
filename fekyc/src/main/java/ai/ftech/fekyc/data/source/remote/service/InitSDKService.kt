package ai.ftech.fekyc.data.source.remote.service

import ai.ftech.fekyc.data.source.remote.base.IApiService
import ai.ftech.fekyc.data.source.remote.model.ekyc.init.sdk.InitSDKRequest
import ai.ftech.fekyc.data.source.remote.model.ekyc.init.sdk.RegisterEkycResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface InitSDKService : IApiService {
    @POST("/auth/sdk/init")
    fun registerEkyc(@Body body: InitSDKRequest, @Query("device_type") deviceType: String,): Call<RegisterEkycResponse>
}

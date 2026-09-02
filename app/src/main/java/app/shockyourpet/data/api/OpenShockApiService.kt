package app.shockyourpet.data.api

import app.shockyourpet.data.api.models.ControlRequest
import app.shockyourpet.data.api.models.LoginRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface OpenShockApiService {

    @GET("2/tokens/self")
    suspend fun getToken2Self(): Response<ResponseBody>

    @GET("1/tokens/self")
    suspend fun getToken1Self(): Response<ResponseBody>

    @GET("1/users/self")
    suspend fun getUser1Self(): Response<ResponseBody>

    @GET("1/shockers/own")
    suspend fun getOwnedShockers1(): Response<ResponseBody>

    @GET("1/shockers/shared")
    suspend fun getSharedShockers1(): Response<ResponseBody>

    @GET("1/devices")
    suspend fun getDevices1(): Response<ResponseBody>

    @GET("1/devices/{hubId}/shockers")
    suspend fun getDeviceShockers1(
        @Path("hubId") hubId: String,
    ): Response<ResponseBody>

    @GET("1/shockers/{shockerId}")
    suspend fun getShocker1(
        @Path("shockerId") shockerId: String,
    ): Response<ResponseBody>

    @GET("2/devices/{hubId}/lcg")
    suspend fun getDeviceLcg2(
        @Path("hubId") hubId: String,
    ): Response<ResponseBody>

    @GET("1/devices/{hubId}/lcg")
    suspend fun getDeviceLcg1(
        @Path("hubId") hubId: String,
    ): Response<ResponseBody>

    @POST("2/shockers/control")
    suspend fun controlShockers2(
        @Body request: ControlRequest,
    ): Response<ResponseBody>

    @POST("2/auth/login")
    suspend fun login2(
        @Body request: LoginRequest,
    ): Response<ResponseBody>

    @POST("1/auth/login")
    suspend fun login1(
        @Body request: LoginRequest,
    ): Response<ResponseBody>
}

package app.shockyourpet.data.api

import app.shockyourpet.BuildConfig
import app.shockyourpet.data.storage.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class OpenShockApiClient(private val tokenManager: TokenManager) {

    private var currentBaseUrl: String = ""
    private var currentToken: String = ""
    private var cachedService: OpenShockApiService? = null

    fun getApiService(): OpenShockApiService {
        val baseUrl = tokenManager.getEffectiveServerUrl().trimEnd('/') + "/"
        val token = tokenManager.apiToken.trim()

        if ((cachedService != null) && (currentBaseUrl == baseUrl) && (currentToken == token)) {
            return cachedService!!
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .withOpenShockAuth(token)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")

            chain.proceed(requestBuilder.build())
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        currentBaseUrl = baseUrl
        currentToken = token
        val service = retrofit.create(OpenShockApiService::class.java)
        cachedService = service
        return service
    }

    fun invalidateCache() {
        cachedService = null
    }
}

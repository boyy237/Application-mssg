package com.ciphertalk.messenger.data.remote

import com.ciphertalk.messenger.BuildConfig
import com.ciphertalk.messenger.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Point d'accès unique au client réseau (Retrofit + OkHttp).
 * Appelez RetrofitClient.init(sessionManager) une seule fois (fait dans MessengerApp).
 */
object RetrofitClient {

    lateinit var sessionManager: SessionManager
        private set

    val api: ApiService by lazy { buildRetrofit().create(ApiService::class.java) }

    fun init(sessionManager: SessionManager) {
        this.sessionManager = sessionManager
    }

    /** URL de base HTTP, ex. "http://10.0.2.2:4000/" (configurable dans app/build.gradle.kts). */
    val baseUrl: String get() = BuildConfig.BASE_URL

    /** Construit l'URL WebSocket correspondante, ex. "ws://10.0.2.2:4000/ws?token=..." */
    fun webSocketUrl(token: String): String {
        val httpBase = baseUrl.trimEnd('/')
        val wsBase = httpBase.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://")
        return "$wsBase/ws?token=$token"
    }

    private fun buildRetrofit(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { sessionManager.token })
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

/** Ajoute automatiquement l'en-tête "Authorization: Bearer <token>" si disponible. */
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrEmpty()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}

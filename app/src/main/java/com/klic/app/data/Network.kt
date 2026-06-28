package com.klic.app.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface KlicApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("conversations")
    suspend fun conversations(): List<Conversation>

    @POST("conversations")
    suspend fun openConversation(@Body body: Map<String, String>): Conversation

    @GET("conversations/{id}/messages")
    suspend fun messages(@Path("id") id: String): List<Message>

    @POST("conversations/{id}/messages")
    suspend fun send(@Path("id") id: String, @Body body: SendMessageRequest): Message

    @GET("users")
    suspend fun findUser(@Query("username") username: String): List<User>

    @POST("calls")
    suspend fun startCall(@Body body: StartCallRequest): CallSession

    @POST("calls/{id}/token")
    suspend fun joinToken(@Path("id") id: String): CallSession
}

object Network {
    // 10.0.2.2 is the host machine from the Android emulator.
    const val BASE_HTTP = "http://10.0.2.2:3000"
    private const val API = "$BASE_HTTP/api/v1/"

    private val json = Json { ignoreUnknownKeys = true }

    fun create(tokenStore: TokenStore): KlicApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                tokenStore.cachedAccess?.let { builder.header("Authorization", "Bearer $it") }
                chain.proceed(builder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(API)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(KlicApi::class.java)
    }
}

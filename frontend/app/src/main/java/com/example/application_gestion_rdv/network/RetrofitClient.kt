package com.example.application_gestion_rdv.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.application_gestion_rdv.network.ApiService
import com.example.application_gestion_rdv.network.ChatApiService

object RetrofitClient {

    const val BASE_URL = "http://192.168.1.99:8000/"
    //const val BASE_URL = "http://10.0.2.2:8000/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    // AJOUTER LE SERVICE DE CHAT
    val chatApiService: ChatApiService by lazy {
        retrofit.create(ChatApiService::class.java)
    }
}
package com.example.application_gestion_rdv.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.application_gestion_rdv.network.ApiService
object RetrofitClient {


    const val BASE_URL = "http://10.0.2.2:8000/"

    // ✅ Pour appareil réel, utilisez l'IP de votre machine
    // const val BASE_URL = "http://192.168.1.X:8000/api/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
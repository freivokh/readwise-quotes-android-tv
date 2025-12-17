// app/src/main/java/com/readwisequotes/data/remote/AuthInterceptor.kt
package com.readwisequotes.data.remote

import com.readwisequotes.settings.SettingsManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val settingsManager: SettingsManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = settingsManager.getApiToken()
        val request = chain.request().newBuilder()
            .apply {
                if (token.isNotEmpty()) {
                    addHeader("Authorization", "Token $token")
                }
            }
            .build()
        return chain.proceed(request)
    }
}

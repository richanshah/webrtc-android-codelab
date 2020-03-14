package xyz.vivekc.webrtccodelab


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class Utils {
    private var retrofitInstance: Retrofit? = null
    fun getRetrofitInstance(): TurnServer? {
        if (retrofitInstance == null) {
            retrofitInstance = Retrofit.Builder()
                    .baseUrl(API_ENDPOINT)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
        }
        return retrofitInstance?.create(TurnServer::class.java)
    }

    companion object {
        private var instance: Utils? = null
        val API_ENDPOINT: String? = "https://global.xirsys.net"
        fun getInstance(): Utils? {
            if (instance == null) {
                instance = Utils()
            }
            return instance
        }
    }
}
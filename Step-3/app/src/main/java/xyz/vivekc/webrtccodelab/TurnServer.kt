package xyz.vivekc.webrtccodelab

import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.PUT


interface TurnServer {
    @PUT("/_turn/<xyrsys_channel>")
    open fun getIceCandidates(@Header("Authorization") authkey: String?): Call<TurnServerPojo?>?
}
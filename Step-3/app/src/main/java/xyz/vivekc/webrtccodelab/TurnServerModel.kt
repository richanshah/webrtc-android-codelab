package xyz.vivekc.webrtccodelab

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class TurnServerModel {
    @SerializedName("s")
    @Expose
    var s: Int? = null

    @SerializedName("p")
    @Expose
    var p: String? = null

    @SerializedName("e")
    @Expose
    var e: Any? = null

    @SerializedName("v")
    @Expose
    var iceServerList: IceServerList? = null

    inner class IceServerList {
        @SerializedName("iceServers")
        @Expose
        var iceServers: MutableList<IceServer?>? = null
    }
}
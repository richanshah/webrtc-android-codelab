package xyz.vivekc.webrtccodelab

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class IceServer {
    @SerializedName("url")
    @Expose
    var url: String? = null

    @SerializedName("username")
    @Expose
    var username: String? = null

    @SerializedName("credential")
    @Expose
    var credential: String? = null
    override fun toString(): String {
        return "IceServer{" +
                "url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", credential='" + credential + '\'' +
                '}'
    }
}
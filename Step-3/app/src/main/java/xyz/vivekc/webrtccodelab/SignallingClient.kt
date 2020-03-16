package xyz.vivekc.webrtccodelab

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.net.URISyntaxException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession


internal class SignallingClient {

    internal interface SignalingInterface {
        fun onRemoteHangUp(msg: String?)
        fun onOfferReceived(data: JSONObject?)
        fun onAnswerReceived(data: JSONObject?)
        fun onIceCandidateReceived(data: JSONObject?)
        fun onTryToStart()
        fun onCreatedRoom()
        fun onJoinedRoom()
        fun onNewPeerJoined()
    }

    companion object {
        private var instance: SignallingClient = SignallingClient()
        fun getInstance(): SignallingClient {
            if (instance == null) {
                instance = SignallingClient()
            }
            if (instance.roomName == null) {
                //set the room name here
                instance.roomName = Constants.ROOM_NAME
            }
            return instance
        }
    }

    private var roomName: String? = null
    private var socket: Socket? = null
    var isChannelReady = false
    var isInitiator = false
    var isStarted = false
    private var callback: SignalingInterface? = null
    private val TAG = SignallingClient::class.java.simpleName


    fun init(signalingInterface: SignalingInterface?) {
        callback = signalingInterface
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, null, null)
            IO.setDefaultHostnameVerifier { hostname: String?, session: SSLSession? -> true }
            IO.setDefaultSSLContext(sslContext)
            //set the socket.io url here
            socket = IO.socket(Constants.SOCKET_HOST_URL)
            socket?.connect()
            Log.d(TAG, "init() called")
            if (roomName?.isNotEmpty()!!) {
                emitInitStatement(roomName)
            }

            //room created event.
            socket?.on("created") { args: Array<Any?>? ->
                Log.d(TAG, "created call() called with: args = [" + Arrays.toString(args) + "]")
                isInitiator = true
                callback?.onCreatedRoom()
            }

            //room is full event
            socket?.on("full") { args: Array<Any?>? -> Log.d(TAG, "full call() called with: args = [" + Arrays.toString(args) + "]") }

            //peer joined event
            socket?.on("join") { args: Array<Any?>? ->
                Log.d(TAG, "join call() called with: args = [" + Arrays.toString(args) + "]")
                isChannelReady = true
                callback?.onNewPeerJoined()
            }

            //when you joined a chat room successfully
            socket?.on("joined") { args: Array<Any?>? ->
                Log.d(TAG, "joined call() called with: args = [" + Arrays.toString(args) + "]")
                isChannelReady = true
                callback?.onJoinedRoom()
            }

            //log event
            socket?.on("log") { args: Array<Any?>? -> Log.d(TAG, "log call() called with: args = [" + Arrays.toString(args) + "]") }

            //bye event
            socket?.on("bye") { args: Array<Any?>? -> callback?.onRemoteHangUp(args?.get(0) as String?) }

            //messages - SDP and ICE candidates are transferred through this
            socket?.on("message") { args: Array<Any?>? ->
                Log.d(TAG, "message call() called with: args = [" + Arrays.toString(args) + "]")
                if (args?.get(0) is String) {
                    Log.d(TAG, "String received :: " + args[0])
                    val data = args[0] as String?
                    if (data.equals("got user media", ignoreCase = true)) {
                        callback?.onTryToStart()
                    }
                    if (data.equals("bye", ignoreCase = true)) {
                        callback?.onRemoteHangUp(data)
                    }
                } else if (args?.get(0) is JSONObject) {
                    try {
                        val data = args[0] as JSONObject?
                        Log.d(TAG, "Json Received :: " + data.toString())
                        val type = data?.getString("type")
                        if (type.equals(Constants.OFFER, ignoreCase = true)) {
                            callback?.onOfferReceived(data)
                        } else if (type.equals(Constants.ANSWER, ignoreCase = true) && isStarted) {
                            callback?.onAnswerReceived(data)
                        } else if (type.equals(Constants.CANDIDATE, ignoreCase = true) && isStarted) {
                            callback?.onIceCandidateReceived(data)
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
    }

    private fun emitInitStatement(message: String?) {
        Log.d(TAG, "emitInitStatement() called with: event = [create or join], message = [$message]")
        socket?.emit("create or join", message)
    }

    fun emitMessage(message: String?) {
        Log.d(TAG, "emitMessage() called with: message = [$message]")
        socket?.emit(Constants.MESSAGE, message)
    }

    fun emitMessage(message: SessionDescription?) {
        try {
            Log.d(TAG, "emitMessage() called with: message = [$message]")
            val obj = JSONObject()
            obj.put(Constants.TYPE, message?.type?.canonicalForm())
            obj.put(Constants.SDP, message?.description)
            Log.d("emitMessage", obj.toString())
            socket?.emit(Constants.MESSAGE, obj)
            Log.d(Constants.ROOM_NAME, obj.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun emitIceCandidate(iceCandidate: IceCandidate?) {
        try {
            val `object` = JSONObject()
            `object`.put(Constants.TYPE, "candidate")
            `object`.put(Constants.LABEL, iceCandidate?.sdpMLineIndex)
            `object`.put(Constants.ID, iceCandidate?.sdpMid)
            `object`.put(Constants.CANDIDATE, iceCandidate?.sdp)
            socket?.emit(Constants.MESSAGE, `object`)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        socket?.emit(Constants.EVENT_BYE, roomName)
        socket?.disconnect()
        socket?.close()
    }


}
package xyz.vivekc.webrtccodelab

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnectionFactory.InitializationOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import xyz.vivekc.webrtccodelab.SignallingClient.SignalingInterface
import java.io.UnsupportedEncodingException
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), View.OnClickListener, SignalingInterface {


    private val rootEglBase by lazy { EglBase.create() }

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        //Initialize PeerConnectionFactory globals.
        val initializationOptions = InitializationOptions.builder(this)
                .createInitializationOptions()

        PeerConnectionFactory.initialize(initializationOptions)

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        val options = PeerConnectionFactory.Options()

        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
                rootEglBase.eglBaseContext, /* enableIntelVp8Encoder */true, /* enableH264HighProfile */true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)
//        PeerConnectionFactory(options, defaultVideoEncoderFactory, defaultVideoDecoderFactory)
        PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory()

    }

    private var sdpConstraints: MediaConstraints? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    var videoSource: VideoSource? = null
    var audioSource: AudioSource? = null
    var iceServers: MutableList<IceServer?>? = ArrayList()
    var audioConstraints: MediaConstraints? = null
    var videoConstraints: MediaConstraints? = null
    private var localVideoView: SurfaceViewRenderer? = null
    private var remoteVideoView: SurfaceViewRenderer? = null

    private var hangup: Button? = null
    var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var localPeer: PeerConnection? = null

    private var gotUserMedia: Boolean = false
    private var peerIceServers: MutableList<PeerConnection.IceServer> = ArrayList()

    private val ALL_PERMISSIONS_CODE = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf<String?>(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), ALL_PERMISSIONS_CODE)
        } else {
            // all permissions already granted
            start()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ALL_PERMISSIONS_CODE && grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            // all permissions granted
            start()
        } else {
            finish()
        }
    }

    private fun initViews() {
        hangup = findViewById(R.id.end_call)
        localVideoView = findViewById(R.id.local_gl_surface_view)
        remoteVideoView = findViewById(R.id.remote_gl_surface_view)
        hangup?.setOnClickListener(this)
    }

    private fun initVideos() {
//        rootEglBase = EglBase.create()
        localVideoView?.init(rootEglBase?.getEglBaseContext(), null)
        remoteVideoView?.init(rootEglBase?.eglBaseContext, null)
        localVideoView?.setZOrderMediaOverlay(true)
        remoteVideoView?.setZOrderMediaOverlay(false)
    }

    private fun getIceServers() {
        //get Ice servers using xirsys
        var data = ByteArray(0)
        try {
            data = "<xirsys_ident>:<xirsys_secret>".toByteArray(charset("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        val authToken = "Basic " + Base64.encodeToString(data, Base64.NO_WRAP)
        Utils.getInstance()?.getRetrofitInstance()?.getIceCandidates(authToken)?.enqueue(object : Callback<TurnServerModel?> {
            override fun onResponse(call: Call<TurnServerModel?>, response: Response<TurnServerModel?>) {
                val body = response.body()
                if (body != null) {
                    iceServers = body.iceServerList?.iceServers
                }
                if (iceServers != null) {
                    for (iceServer in iceServers!!) {
                        if (iceServer?.credential == null) {
                            val peerIceServer = PeerConnection.IceServer.builder(iceServer?.url).createIceServer()
                            peerIceServers.add(peerIceServer)
                        } else {
                            val peerIceServer = PeerConnection.IceServer.builder(iceServer.url)
                                    .setUsername(iceServer.username)
                                    .setPassword(iceServer.credential)
                                    .createIceServer()
                            peerIceServers.add(peerIceServer)
                        }
                    }
                }
                Log.d("onApiResponse", """IceServers${iceServers.toString()}""".trimIndent())
            }

            override fun onFailure(call: Call<TurnServerModel?>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun start() {
        // keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initViews()
        initVideos()
        getIceServers()
        SignallingClient.getInstance().init(this)

        //Initialize PeerConnectionFactory globals.
        val initializationOptions = InitializationOptions.builder(this)
                .createInitializationOptions()

        PeerConnectionFactory.initialize(initializationOptions)

        //Now create a VideoCapturer instance.
        val videoCaptureAndroid: VideoCapturer? = createCameraCapturer(Camera1Enumerator(false))

        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = MediaConstraints()
        videoConstraints = MediaConstraints()

        //Create a VideoSource instance
        if (videoCaptureAndroid != null) {
            surfaceTextureHelper = SurfaceTextureHelper.create(Constants.CAPTURE_THREAD, rootEglBase?.eglBaseContext)
            videoSource = peerConnectionFactory.createVideoSource(videoCaptureAndroid.isScreencast)
            videoCaptureAndroid.initialize(surfaceTextureHelper, this, videoSource?.getCapturerObserver())
        }
        localVideoTrack = peerConnectionFactory.createVideoTrack(Constants.VIDEO_TRACK_ID, videoSource)

        //create an AudioSource instance
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack(Constants.AUDIO_TRACK_ID, audioSource)
        videoCaptureAndroid?.startCapture(Constants.VideoParams.WIDTH, Constants.VideoParams.HEIGHT, Constants.VideoParams.FPS)
        localVideoView?.visibility = View.VISIBLE
        // And finally, with our VideoRenderer ready, we
        // can add our renderer to the VideoTrack.
        localVideoTrack?.addSink(localVideoView)
        localVideoView?.setMirror(true)
        remoteVideoView?.setMirror(true)
        gotUserMedia = true
        if (SignallingClient.getInstance().isInitiator) {
            onTryToStart()
        }
    }

    /**
     * This method will be called directly by the app when it is the initiator and has got the local media
     * or when the remote peer sends a message through socket that it is ready to transmit AV data
     */
    override fun onTryToStart() {
        runOnUiThread {
            if (!SignallingClient.getInstance().isStarted &&
                    localVideoTrack != null && SignallingClient.getInstance().isChannelReady) {
                createPeerConnection()
                SignallingClient.getInstance().isStarted = true
                if (SignallingClient.getInstance().isInitiator) {
                    doCall()
                }
            }
        }
    }

    /**
     * Creating the local peer connection instance
     */
    private fun createPeerConnection() {
        val rtcConfig = RTCConfiguration(peerIceServers)
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA
        localPeer = peerConnectionFactory.createPeerConnection(rtcConfig, object : CustomPeerConnectionObserver("localPeerCreation") {
            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                super.onIceCandidate(iceCandidate)
                onIceCandidateReceived(iceCandidate)
            }

            override fun onAddStream(mediaStream: MediaStream?) {
                showToast("Received Remote stream")
                super.onAddStream(mediaStream)
                gotRemoteStream(mediaStream)
            }
        })!!
        addStreamToLocalPeer()
    }

    /**
     * Adding the stream to the local peer
     */
    private fun addStreamToLocalPeer() {
        //creating local mediastream
        val stream = peerConnectionFactory.createLocalMediaStream(Constants.LOCAL_MEDIA_STREAM_TRACK_ID)
        stream?.addTrack(localAudioTrack)
        stream?.addTrack(localVideoTrack)
        localPeer?.addStream(stream)
    }

    /**
     * This method is called when the app is the initiator - We generate the offer and send it over through socket
     * to remote peer
     */
    private fun doCall() {
        sdpConstraints = MediaConstraints()
        sdpConstraints?.mandatory?.add(
                MediaConstraints.KeyValuePair(Constants.OFFER_TO_RECEIVE_AUDIO, "true"))
        sdpConstraints?.mandatory?.add(
                MediaConstraints.KeyValuePair(Constants.OFFER_TO_RECEIVE_AUDIO, "true"))
        localPeer?.createOffer(object : CustomSdpObserver(Constants.LOCAL_CREATE_OFFER) {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                super.onCreateSuccess(sessionDescription)
                localPeer?.setLocalDescription(CustomSdpObserver(Constants.LOCAL_SET_LOCAL_DESC), sessionDescription)
                Log.d("onCreateSuccess", "SignallingClient emit ")
                SignallingClient.getInstance().emitMessage(sessionDescription)
            }
        }, sdpConstraints)
    }

    /**
     * Received remote peer's media stream. we will get the first video track and render it
     */
    private fun gotRemoteStream(stream: MediaStream?) {
        //we have remote video stream. add to the renderer.
        val videoTrack = stream!!.videoTracks[0]
        runOnUiThread {
            try {
                remoteVideoView?.setVisibility(View.VISIBLE)
                videoTrack.addSink(remoteVideoView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Received local ice candidate. Send it to remote peer through signalling for negotiation
     */
    fun onIceCandidateReceived(iceCandidate: IceCandidate?) {
        //we have received ice candidate. We can set it to the other peer.
        SignallingClient.getInstance().emitIceCandidate(iceCandidate)
    }

    /**
     * SignallingCallback - called when the room is created - i.e. you are the initiator
     */
    override fun onCreatedRoom() {
        showToast("You created the room $gotUserMedia")
        if (gotUserMedia) {
            SignallingClient.getInstance().emitMessage("got user media")
        }
    }

    /**
     * SignallingCallback - called when you join the room - you are a participant
     */
    override fun onJoinedRoom() {
        showToast("You joined the room $gotUserMedia")
        if (gotUserMedia) {
            SignallingClient.getInstance().emitMessage("got user media")
        }
    }

    override fun onNewPeerJoined() {
        showToast("Remote Peer Joined")
    }

    override fun onRemoteHangUp(msg: String?) {
        showToast("Remote Peer hungup")
        runOnUiThread { hangup() }
    }

    /**
     * SignallingCallback - Called when remote peer sends offer
     */
    override fun onOfferReceived(data: JSONObject?) {
        showToast("Received Offer")
        runOnUiThread {
            if (!SignallingClient.getInstance().isInitiator && !SignallingClient.getInstance().isStarted) {
                onTryToStart()
            }
            try {
                localPeer?.setRemoteDescription(CustomSdpObserver(Constants.LOCAL_SET_REMOTE), SessionDescription(SessionDescription.Type.OFFER, data?.getString("sdp")))
                doAnswer()
                updateVideoViews(true)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private fun doAnswer() {
        localPeer?.createAnswer(object : CustomSdpObserver(Constants.LOCAL_CREATE_ANS) {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                super.onCreateSuccess(sessionDescription)
                localPeer?.setLocalDescription(CustomSdpObserver(Constants.LOCAL_SET_LOCAL), sessionDescription)
                SignallingClient.getInstance().emitMessage(sessionDescription)
            }
        }, MediaConstraints())
    }

    /**
     * SignallingCallback - Called when remote peer sends answer to your offer
     */
    override fun onAnswerReceived(data: JSONObject?) {
        showToast("Received Answer")
        try {
            localPeer?.setRemoteDescription(CustomSdpObserver(Constants.LOCAL_SET_REMOTE),
                    SessionDescription(SessionDescription.Type.fromCanonicalForm(data?.getString("type")?.toLowerCase(Locale.getDefault())), data?.getString("sdp")))
            updateVideoViews(true)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    /**
     * Remote IceCandidate received
     */
    override fun onIceCandidateReceived(data: JSONObject?) {
        try {
            localPeer?.addIceCandidate(IceCandidate(data?.getString("id"), data!!.getInt("label"), data?.getString("candidate")))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    //Make remoteVisible false if you want to remove remote view
    private fun updateVideoViews(remoteVisible: Boolean) {
        runOnUiThread {
            var params = localVideoView?.getLayoutParams()
            if (remoteVisible) {
                params?.height = dpToPx(100)
                params?.width = dpToPx(100)
            } else {
                params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            localVideoView?.setLayoutParams(params)
        }
    }

    /**
     * Closing up - normal hangup and app destroy
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.end_call -> {
                hangup()
            }
        }
    }

    private fun hangup() {
        try {
            if (localPeer != null) {
                localPeer?.close()
            }
            localPeer = null
            SignallingClient.getInstance().close()
            updateVideoViews(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        SignallingClient.getInstance().close()
        super.onDestroy()
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null
        }
    }

    /**
     * Util Methods
     */
    fun dpToPx(dp: Int): Int {
        val displayMetrics = resources.displayMetrics
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }

    fun showToast(msg: String?) {
        runOnUiThread { Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator?): VideoCapturer? {
        val deviceNames = enumerator!!.getDeviceNames()

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.")
        for (deviceName in deviceNames) {
            if (enumerator != null) {
                if (enumerator.isFrontFacing(deviceName)) {
                    Logging.d(TAG, "Creating front facing camera capturer.")
                    val videoCapture: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                    if (videoCapture != null) {
                        return videoCapture
                    }
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.")
        for (deviceName in deviceNames) {
            if (!enumerator?.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.")
                val videoCapturer: VideoCapturer? = enumerator?.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    companion object {
        private val TAG: String? = "MainActivity"
    }
}
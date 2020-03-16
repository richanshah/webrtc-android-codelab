package xyz.vivekc.webrtccodelab

open class Constants {
    companion object {

        const val SOCKET_HOST_URL = "https://192.168.0.196:8080"
        const val ROOM_NAME = "smart"
        const val VIDEO_TRACK_ID = "100"
        const val AUDIO_TRACK_ID = "101"
        const val LOCAL_MEDIA_STREAM_TRACK_ID = "102"
        const val CAPTURE_THREAD = "CaptureThread"
        const val OFFER_TO_RECEIVE_AUDIO = "OfferToReceiveAudio"
        const val LOCAL_SET_REMOTE = "localSetRemote"
        const val LOCAL_CREATE_ANS = "localCreateAns"
        const val LOCAL_CREATE_OFFER = "localCreateOffer"
        const val LOCAL_SET_LOCAL = "localSetLocal"
        const val LOCAL_SET_LOCAL_DESC = "localSetLocalDesc"
        const val TYPE = "type"
        const val LABEL = "label"
        const val ID = "id"
        const val CANDIDATE = "candidate"
        const val ANSWER = "answer"
        const val OFFER = "offer"
        const val EVENT_BYE = "bye"
        const val SDP = "sdp"
        const val MESSAGE = "message"

    }

    interface VideoParams {
        companion object {
            var WIDTH = 1024
            var HEIGHT = 720
            var FPS = 30
        }
    }
}
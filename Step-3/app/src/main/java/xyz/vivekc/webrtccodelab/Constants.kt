package xyz.vivekc.webrtccodelab

open class Constants {
    companion object {

        const val SOCKET_HOST_URL = "https://192.168.0.196:8080"
        const val TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjYWRiM2RmMS01OWFhLTRkOGItODk2Ni0yZmRkMjZlZjUxNTAiLCJpYXQiOjE1ODQ1MDk1MzIsInN1YiI6IntcInByb2ZpbGVcIjpbM10sXCJsb2dnZWRpblwiOnRydWUsXCJpZFwiOjE3fSIsImlzcyI6InNtYXJ0U2Vuc2UgZm9yIElPVCIsImV4cCI6MTU4NTgwNTUzMn0.hT_kfA7h4hWC5EEp-HiiJgheLQQgKGhxG0uNNpQQwuk"
        const val ROOM_NAME = "smart"
        const val VIDEO_TRACK_ID = "100"
        const val AUDIO_TRACK_ID = "101"
        const val LOCAL_MEDIA_STREAM_TRACK_ID = "Stream"
        const val CAPTURE_THREAD = "CaptureThread"
        const val OFFER_TO_RECEIVE_AUDIO = "OfferToReceiveAudio"
        const val OFFER_TO_RECEIVE_VIDEO = "OfferToReceiveVideo"
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
        val MQTT_CONNECTION_SUCCESS = "CONNECTION_SUCCESS"
        val MQTT_CONNECTION_FAILURE = "CONNECTION_FAILURE"
        val MQTT_CONNECTION_LOST = "CONNECTION_LOST"
        val MQTT_SUBSCRIBE_SUCCESS = "SUBSCRIBE_SUCCESS"
        val MQTT_SUBSCRIBE_FAILURE = "SUBSCRIBE_FAILURE"
        val MQTT_UNSUBSCRIBE_SUCCESS = "SUBSCRIBE_SUCCESS"
        val MQTT_UNSUBSCRIBE_FAILURE = "SUBSCRIBE_FAILURE"
        val MQTT_MESSAGE_PAYLOAD = "MQTT_PAYLOAD"
        val KEY_PAYLOAD = "PayloadData"
        val KEY_TOPIC = "PayloadTopic"
        val BROADCAST_PAYLOAD_MQTT = "BroadcastPayloadMQTT"
        var MQTT_DATA_MAC_ADDRESS = "MqttMacAddress"
        var MQTT_DATA_APPLICATION_DATA = "MqttApplicationData"
        var MQTT_DATA_RELAY_IDENTIFICATION = "MqttRelayIdentification"

        interface MsgActionType {
            companion object {
                var get = "1"
                var set = "2"
                var del = "3"
                var push = "4"
            }
        }

        interface MessageType {
            companion object {
                var deviceRegistry3Action = "1"
                var deviceRegistry2Action = "2"
                var applicationData = "3"
                var deviceList = "4"
                var factoryReset = "5"
                var gatewayPresence = "6"
                var devicePresence = "7"
                var deviceBinding = "8"
                var energyData = "9"
                var temperatureData = "D"
            }
        }
    }

    interface VideoParams {
        companion object {
            var WIDTH = 1024
            var HEIGHT = 720
            var FPS = 30
        }
    }
}
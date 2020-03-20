package xyz.vivekc.webrtccodelab

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smartoffice.utils.MQTTEncryptionUtil

import org.json.JSONObject

class MqttBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        intent?.let { intntData ->
            intntData.action?.let { actionVal ->
                when (actionVal) {
                    Constants.MQTT_CONNECTION_SUCCESS -> {
                        Log.e("MqttBroadcastReceiver", "MQTT Connection Success")
                    }
                    Constants.MQTT_CONNECTION_FAILURE -> {
                        Log.e("MqttBroadcastReceiver", "MQTT Connection Failure")
                    }
                    Constants.MQTT_CONNECTION_LOST -> {
                        Log.e("MqttBroadcastReceiver", "MQTT Connection Lost")
                    }
                    Constants.MQTT_SUBSCRIBE_SUCCESS -> {
                        Log.e("MqttBroadcastReceiver", "MQTT Subscribe Success")

                    }
                    Constants.MQTT_SUBSCRIBE_FAILURE -> {
                        Log.e("MqttBroadcastReceiver", "MQTT Subscribe Failure")
                    }
                    Constants.MQTT_UNSUBSCRIBE_SUCCESS -> {
                        Log.e("MqttBroadcastReceiver", "MQTT Unsubscribe Success")
                    }
                    Constants.MQTT_UNSUBSCRIBE_FAILURE -> {
                        Log.e("MqttBroadcastReceiver", "MQTT Unsubscribe Failure")
                    }
                    Constants.MQTT_MESSAGE_PAYLOAD -> {
                        try {
                            var topic = intntData.getStringExtra(Constants.KEY_TOPIC)
                            topic?.let { topicNm ->
                                when (topicNm) {
                                    //SS_BROKER_IOT_OFFICE --> For env //SS_BROKER_IOT_AUTOMATION_MASTER --> For prod
                                    BuildConfig.BROKE_SUBSCRIPTION_CHANNEL -> {
                                        var msgPayload =
                                            intntData.getByteArrayExtra(Constants.KEY_PAYLOAD)
                                        var message = MQTTEncryptionUtil.decrypt(msgPayload)
                                        Log.e(
                                            "Application Testing",
                                            "Decrypted Message: " + message
                                        )

                                        var macAddress = ""
                                        var messageType = ""
                                        var applicationData = ""
                                        var action = ""
                                        try {
                                            var firstElimination =
                                                message.substring(message.indexOf("|") + 1)
                                            var secondElimination = firstElimination.substring(
                                                firstElimination.indexOf("|") + 1
                                            )
                                            var thirdElimination = secondElimination.substring(
                                                secondElimination.indexOf("|") + 1
                                            )
                                            var forthElimination = thirdElimination.substring(
                                                thirdElimination.indexOf("|") + 1
                                            )
                                            var fifthElimination = forthElimination.substring(
                                                forthElimination.indexOf("|") + 1
                                            )
                                            var sixElimination = fifthElimination.substring(
                                                fifthElimination.indexOf("|") + 1
                                            )

                                            macAddress = thirdElimination.substringBefore("|")
                                            messageType = secondElimination.substringBefore("|")
                                            action = fifthElimination.substringBefore("|")
                                            applicationData = sixElimination.substringBefore("|")
                                        } catch (e: NullPointerException) {
                                            e.printStackTrace()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        Log.e("Application Testing", "macAddress: $macAddress")
                                        Log.e("Application Testing", "messageType: $messageType")
                                        Log.e("Application Testing", "action: $action")
                                        Log.e(
                                            "Application Testing",
                                            "applicationData: $applicationData"
                                        )

                                        if (messageType.equals(Constants.Companion.MessageType.applicationData) && (action.equals(
                                                Constants.Companion.MsgActionType.set
                                            ) || action.equals(
                                                Constants.Companion.MsgActionType.push
                                            ))
                                        ) {

                                            broadcastData(context, macAddress, applicationData)
                                        }

                                    }

                                    //SS_SUB_NOTIFICATION_IOT_OFFICE -- >  For enterprise environment
                                    //SS_NOTIFICATION_SUB_IOT --> For prod
                                    BuildConfig.NOTIFICATIONS_SUBSCRIPTION_CHANNEL -> {
                                        //                                var message = MQTTEncryptionUtil.decrypt(mqttMessage.payload)
                                        //var message = intntData.getStringExtra(Constants.KEY_PAYLOAD)
                                        var msgPayload =
                                            intntData.getByteArrayExtra(Constants.KEY_PAYLOAD)
                                        var message = MQTTEncryptionUtil.decrypt(msgPayload)
                                        Log.e(
                                            "Application Testing",
                                            "MQTT message has come" + System.currentTimeMillis()
                                        )
                                        var jsonMessage = JSONObject(message)
                                        broadcastData(
                                            context,
                                            jsonMessage.get("macAddress").toString(),
                                            jsonMessage.get("applicationData").toString()
                                        )
                                    }
                                }
                            }

                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    else -> {

                    }
                }
            }
        }
    }

    private fun broadcastData(context: Context?, macAddress: String, applicationData: String) {
        var localBroadcastManager = LocalBroadcastManager.getInstance(context!!)
        var relayAction = applicationData.substring(0, 2).toUpperCase()
        var relayIdentification = "0"

        var list = parseEndpoint(relayAction)
        Log.d("List Size", list.size.toString())

        list.forEach {
            when (it) {
                1 -> {
                    relayIdentification = "1"
                }
                2 -> {
                    relayIdentification = "2"
                }
                3 -> {
                    relayIdentification = "3"
                }
                4 -> {
                    relayIdentification = "4"
                }
            }

            val i = Intent(Constants.BROADCAST_PAYLOAD_MQTT)
            i.putExtra(Constants.MQTT_DATA_MAC_ADDRESS, macAddress)
            i.putExtra(Constants.MQTT_DATA_APPLICATION_DATA, applicationData)
            i.putExtra(Constants.MQTT_DATA_RELAY_IDENTIFICATION, relayIdentification)
            localBroadcastManager.sendBroadcast(i)
        }


    }


    private fun parseEndpoint(endpoints: String): List<Int> {
        val epList = arrayListOf<Int>()
        val epDecimal = Integer.parseInt(endpoints, 16)
        for (bitIndex in 0..6) {
            if (epDecimal and (1 shl bitIndex) > 0) {
                epList.add(bitIndex + 1)
            }
        }
        return epList
    }

}
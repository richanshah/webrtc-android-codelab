package xyz.vivekc.webrtccodelab

import android.content.Context
import android.content.Intent
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


/**
 * MqttHelper is the config class for setting up the mqtt connection and
 * subscribe to the event of the mqtt client
 *
 * @author Gulnaz Ghanchi
 */
class MqttHelper(var context: Context, subscriptionTopic: String) {
    lateinit var mqttAndroidClient: MqttAndroidClient

    internal val serverUri = BuildConfig.MQTT_URL

    internal val subscriptionTopic = subscriptionTopic

    internal val username = BuildConfig.MQTT_USER
    internal val password = BuildConfig.MQTT_PASSWORD

    init {
        try {
         /*   if (EventBus.getDefault().isRegistered(this).not()) {
                EventBus.getDefault().register(this)
            }*/
            mqttAndroidClient = MqttAndroidClient(context, serverUri, generateMqttClientID())
            mqttAndroidClient.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(b: Boolean, s: String) {
                    Log.e("mqtt", s)
                }

                override fun connectionLost(throwable: Throwable) {
                    androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(
                            Constants.MQTT_CONNECTION_LOST))
                    Log.e("Mqtt", "Helper Class connection lost: $serverUri$throwable")
                }

                @Throws(Exception::class)
                override fun messageArrived(topic: String, mqttMessage: MqttMessage) {
                    var intentMsg = Intent(Constants.MQTT_MESSAGE_PAYLOAD)
                    intentMsg.putExtra(Constants.KEY_TOPIC, topic)
                    intentMsg.putExtra(Constants.KEY_PAYLOAD, mqttMessage.payload)
                    androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context).sendBroadcast(intentMsg)
                }

                override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {

                }
            })
            connect()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun generateMqttClientID(): String {
        return "android" + System.nanoTime()
    }

    /**
     * This method will set the callback to the mqtt client
     */
    fun setCallback(callback: MqttCallbackExtended) {
        try {
            mqttAndroidClient.setCallback(callback)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * This method will connect the mqtt
     */
    private fun connect() {
        try {

            val mqttConnectOptions = MqttConnectOptions()
            mqttConnectOptions.isAutomaticReconnect = true
            mqttConnectOptions.isCleanSession = true
            mqttConnectOptions.userName = username
            mqttConnectOptions.password = password.toCharArray()


            mqttAndroidClient.connect(mqttConnectOptions, context, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(Constants.MQTT_CONNECTION_SUCCESS))
                    val disconnectedBufferOptions = DisconnectedBufferOptions()
                    disconnectedBufferOptions.isBufferEnabled = true
                    disconnectedBufferOptions.bufferSize = 100
                    disconnectedBufferOptions.isPersistBuffer = false
                    disconnectedBufferOptions.isDeleteOldestMessages = false
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions)
                    subscribeToTopic()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(Constants.MQTT_CONNECTION_FAILURE))
                    Log.e("Mqtt", "Failed to connect to: " + serverUri + exception.toString())
                }
            })


        } catch (ex: MqttException) {
            ex.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * This method will subscribe to the topic to the mqtt client
     */
    private fun subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, context, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.e("subscribeToTopic", "subscribeToTopic: $asyncActionToken")

                    androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(Constants.MQTT_SUBSCRIBE_SUCCESS))
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(Constants.MQTT_SUBSCRIBE_FAILURE))
                }
            })

        } catch (ex: MqttException) {
            System.err.println("Exceptionst subscribing")
            ex.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun publish(topic: String?, message: ByteArray?) {
        if (mqttAndroidClient != null && topic != null && message != null) {
            mqttAndroidClient!!.publish(topic, message, 1, false)
            println("publish" + message?.contentToString())
        }
        /**
         * This method will unsubscribe to the topic to the mqtt client
         */
        fun unSubscribeToTopic() {
            try {
                mqttAndroidClient.unsubscribe(subscriptionTopic, context, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken) {
                        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(Constants.MQTT_UNSUBSCRIBE_SUCCESS))
                    }

                    override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(Constants.MQTT_UNSUBSCRIBE_FAILURE))
                    }
                })

            } catch (ex: MqttException) {
                System.err.println("Exceptionst subscribing")
                ex.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
/*
        @Subscribe
        fun onMqttPublishEvent(mqttPublishEvent: MqttPublishEvent) {
//        val charset = Charsets.UTF_8
//        val byteArray = mqttPublishEvent.bytMessage.toString().toByteArray(charset)
            mqttAndroidClient.publish(mqttPublishEvent.topic, mqttPublishEvent.bytMessage,1,false)
//        logD(TAG, "OnMqttPublishEventSend" + mqttPublishEvent.bytMessage?.contentToString())
            println("OnMqttPublishEventSend" +mqttPublishEvent.bytMessage?.contentToString())

        }*/

        fun sendMessage(msg: MqttMessage?) {
            mqttAndroidClient.publish(BuildConfig.NOTIFICATIONS_SUBSCRIPTION_CHANNEL, msg)
        }

        /**
         * This method will disconnect the mqtt client
         */
        fun disconnectClient() {
            mqttAndroidClient.unregisterResources()
            mqttAndroidClient.close()
        }
    }
}
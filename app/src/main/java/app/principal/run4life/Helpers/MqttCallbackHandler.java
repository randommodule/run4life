package app.principal.run4life.Helpers;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttCallbackHandler implements MqttCallback {
    @Override
    public void connectionLost(Throwable cause) {
        // Manejar la pérdida de conexión
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // Manejar los mensajes entrantes
        String payload = new String(message.getPayload());
        System.out.println("Message received on topic " + topic + ": " + payload);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Manejar la entrega completa del mensaje (para mensajes QoS 1 y 2)
    }
}
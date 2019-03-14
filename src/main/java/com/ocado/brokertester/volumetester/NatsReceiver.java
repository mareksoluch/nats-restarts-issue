package com.ocado.brokertester.volumetester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocado.brokertester.domain.Message;
import io.nats.streaming.NatsStreaming;
import io.nats.streaming.Options;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.SubscriptionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class NatsReceiver {

    private final static Logger log = LoggerFactory.getLogger(MessageProcessor.class);
    private final Consumer<Message> messageReceiver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NatsReceiver(String clusterId, String clientId, String brokerUrl,
                        Consumer<Message> messageReceiver, String subject, String durableName, String queue) {

        log.info("Connecting to cluster: {}, client: {}, broker: {}, subject: {}, durableName: {}, queue: {}",
                clusterId, clientId, brokerUrl, subject, durableName, queue);
        this.messageReceiver = messageReceiver;

        SubscriptionOptions opts = new SubscriptionOptions.Builder()
                .durableName(durableName)
                .manualAcks()
                .build();
        StreamingConnection sc = connect(clusterId, clientId, brokerUrl.split(",")[0]);
        try {
            sc.subscribe(subject, queue, this::consumeMessage, opts);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void consumeMessage(io.nats.streaming.Message msg) {
        try {
            Message message = objectMapper.readValue(msg.getData(), Message.class);
            log.debug("Received message : {}", message);
            this.messageReceiver.accept(message);
            log.debug("Acking message : {}", message.getId());
            msg.ack();
            log.debug("Message acked : {}", message.getId());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Issue on processing received message : " + msg, e);
        }
    }

    private StreamingConnection connect(String clusterId, String clientId, String brokerUrl) {
        log.debug("Getting connection for clusterId {} clientId {} brokerUrl {}", clusterId, clientId, brokerUrl);
        Options opts;
        if (brokerUrl != null) {
            opts = new Options.Builder()
                    .natsUrl(brokerUrl)
                    .build();
        } else {
            opts = NatsStreaming.defaultOptions();
        }
        StreamingConnection connection = null;
        try {
            connection = NatsStreaming.connect(clusterId, clientId, opts);
        } catch (IOException | InterruptedException e) {
            log.error("Could not acquire connection", e);
        }
        log.debug("Connection acquired {}", connection);
        return connection;
    }
}

package com.ocado.brokertester.volumetester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocado.brokertester.domain.Message;
import io.nats.client.Nats;
import io.nats.streaming.NatsStreaming;
import io.nats.streaming.Options;
import io.nats.streaming.StreamingConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;


public class NatsSender {

    private final static Logger log = LoggerFactory.getLogger(MessageProcessor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clusterId;
    private final String clientId;
    private String brokerUrl;
    private final List<String> brokerUrlsList;
    private final String subject;
    private StreamingConnection sc;

    public NatsSender(String clusterId, String clientId, String brokerUrl, String subject) {
        this.clusterId = clusterId;
        this.clientId = clientId;
        this.brokerUrlsList = Arrays.asList(brokerUrl.split(","));
        this.brokerUrl = brokerUrlsList.get(0);
        this.subject = subject;
        this.sc = connect(clusterId, clientId, brokerUrl);
    }

    private synchronized StreamingConnection connect(String clusterId, String clientId, String brokerUrl) {
        if (this.sc != null) {
            return this.sc;
        }
        try {
            sc = getConnection(clusterId, clientId, brokerUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sc;
    }

    void send(Message message) {
        log.debug("Sending message: {}", message);
        StreamingConnection connection = connect(clusterId, clientId, brokerUrl);
        try {
            byte[] messageBytes = objectMapper.writeValueAsBytes(message);
            connection.publish(subject, messageBytes);
            log.debug("Message sent: {}", message.getId());
        } catch (NullPointerException e) {
            handleReceonnect(e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            e.printStackTrace();
            closeConnection();
            throw new RuntimeException(e);
        } finally {
            try {
                connection.getNatsConnection().flush(Duration.ZERO);
            } catch (TimeoutException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleReceonnect(NullPointerException e) {
        log.warn("Caught exception when sending message to broker: " + brokerUrl, e);
        int currentUrlIndex = brokerUrlsList.indexOf(brokerUrl);
        int nextUrlIndex = (currentUrlIndex + 1) % brokerUrlsList.size();
        brokerUrl = brokerUrlsList.get(nextUrlIndex);
        log.warn("Closing current connection and connecting to broker " + brokerUrl);
        closeConnection();
    }

    private synchronized void closeConnection() {
        if (sc != null) {
            try {
                sc.close();
            } catch (IOException | TimeoutException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                sc = null;
            }
        }
    }

    private StreamingConnection getConnection(String clusterId, String clientId, String brokerUrl) throws IOException, InterruptedException {
        log.debug("Getting connection for clusterId {} clientId {} brokerUrl {}", clusterId, clientId, brokerUrl);
        Options opts;
        if (brokerUrl != null) {
            io.nats.client.Options natsConnectionOptions = new io.nats.client.Options.Builder()
                    .reconnectBufferSize(0)
                    .server(brokerUrl)
                    .build();

            opts = new Options.Builder()
                    .natsUrl(brokerUrl)
                    .natsConn(Nats.connect(natsConnectionOptions))
                    .build();
        } else {
            opts = NatsStreaming.defaultOptions();
        }
        StreamingConnection connection = NatsStreaming.connect(clusterId, clientId, opts);
        log.debug("Connection acquired {}", connection);
        return connection;
    }
}

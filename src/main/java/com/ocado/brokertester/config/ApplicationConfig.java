package com.ocado.brokertester.config;

import com.ocado.brokertester.domain.Message;
import com.ocado.brokertester.volumetester.MessageProcessor;
import com.ocado.brokertester.volumetester.NatsReceiver;
import com.ocado.brokertester.volumetester.NatsSender;
import com.ocado.brokertester.volumetester.VolumeTester;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.function.Consumer;

@Configuration
public class ApplicationConfig {

    @Value("${nats.clusterId}")
    private String clusterId;
    @Value("${nats.brokerUrl}")
    private String brokerUrl;

    private final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    private final String instanceName = UUID.randomUUID().toString().substring(0, 2);
    private final String outputQueue = "outputQueue";
    private final String inputQueue = "inputQueue";

    @Bean
    public PrometheusMeterRegistry getPrometheusRegistry() {
        return prometheusRegistry;
    }

    @Bean
    public NatsReceiver inoutQueueReceiver() {
        return natsReceiver("outputNatsReceiver", outputQueue, volumeTester(), instanceName);
    }

    @Bean
    public NatsReceiver inNatsToOutNats() {
        NatsSender messageSender = natsSender("outputQueueNatsSender", outputQueue);
        return natsReceiver("inNatsToOutNats", inputQueue, new MessageProcessor(messageSender, prometheusRegistry), instanceName);
    }

    @Bean
    public VolumeTester volumeTester() {
        NatsSender sender = natsSender("inputNatsSender", inputQueue);
        return new VolumeTester(sender, prometheusRegistry, Message::getParentId);
    }

    private NatsSender natsSender(String senderName, String queueName) {
        return new NatsSender(clusterId, senderName + "-" + instanceName, brokerUrl, queueName + instanceName);
    }

    private NatsReceiver natsReceiver(String beanName, String queueName, Consumer<Message> messageReceiver, String instanceName) {
        String clientId = beanName + "-" + instanceName;
        String durableName = beanName + "-durable-subscriber-" + this.instanceName;
        String subject = queueName + this.instanceName;
        String queueGroup = subject;
        return new NatsReceiver(clusterId, clientId, brokerUrl, messageReceiver, subject, durableName, queueGroup);
    }
}

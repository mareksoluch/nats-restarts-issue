package com.ocado.brokertester.volumetester;

import com.ocado.brokertester.domain.Message;
import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class MessageProcessor implements Consumer<Message> {
    private final static Logger log = LoggerFactory.getLogger(MessageProcessor.class);
    private final NatsSender messageSender;
    private final Counter processedMessages;

    public MessageProcessor(NatsSender messageSender, PrometheusMeterRegistry prometheusRegistry) {
        this.messageSender = messageSender;
        this.processedMessages = Counter.builder("processedMessages")
                .register(prometheusRegistry);
    }

    @Override
    public void accept(Message message) {
        log.debug("Processing message: " + message);
        messageSender.send(new Message(message.getId() + "_outcome", message.getId(), message.getPayload()));
        processedMessages.increment();
    }
}

package com.ocado.brokertester.volumetester;

import com.ocado.brokertester.domain.Message;
import com.ocado.brokertester.domain.TestConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.randomAscii;

public class VolumeTester implements Consumer<Message> {

    private final static Logger log = LoggerFactory.getLogger(VolumeTester.class);

    private final BlockingDeque<Message> messageBuffer = new LinkedBlockingDeque<>(1000);
    private final ConcurrentHashMap<String, Instant> pendingRequests = new ConcurrentHashMap<>();
    private final TestConfig defaultConfig;
    private final AtomicBoolean testRunning = new AtomicBoolean(false);
    private final NatsSender sender;
    private final PrometheusMeterRegistry prometheusRegistry;
    private final Function<Message, String> messageGetter;

    public VolumeTester(NatsSender sender, PrometheusMeterRegistry prometheusRegistry, Function<Message, String> messageGetter) {
        this.sender = sender;
        this.messageGetter = messageGetter;
        this.defaultConfig = new TestConfig();
        this.prometheusRegistry = prometheusRegistry;
    }

    String startTest(TestConfig testConfig) {
        String testId = RandomStringUtils.randomAlphabetic(3);
        log.info("Staring test with ID {}", testId);
        testRunning.set(true);

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(testConfig.getSamplingThreadsCount() + 1);

        scheduledExecutorService.execute(() -> setupMessages(testId, testConfig));
        scheduledExecutorService.scheduleAtFixedRate(() -> sendMessages(testConfig), 0, testConfig.getSamplingDelayMilliseconds(), TimeUnit.MILLISECONDS);
        return testId;
    }

    String startTest() {
        return startTest(defaultConfig);
    }

    void stopTest() {
        testRunning.set(false);
    }


    Map<String, Instant> getPendingRequests() {
        return pendingRequests;
    }


    private void updateMetrics(Duration duration) {
        Timer.builder("message.processing.time")
                .publishPercentiles(0.5, 0.95) // median and 95th percentile
                .publishPercentileHistogram()
                .sla(Duration.ofMillis(100))
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(prometheusRegistry)
                .record(duration);

        Counter.builder("message.processing.count")
                .register(prometheusRegistry)
                .increment();
    }

    private void sendMessages(TestConfig testConfig) {
        List<Message> messagesToSend = new ArrayList<>(testConfig.getMessagesChunkToSend());
        messageBuffer.drainTo(messagesToSend, testConfig.getMessagesChunkToSend());
        messagesToSend
                .forEach(this::sendMessage);
    }

    private void sendMessage(Message message) {
        try {
            pendingRequests.put(message.getId(), Instant.now());
            sender.send(message);
        } catch (Exception e) {
            log.error("Failed to send message: " + message, e);
            pendingRequests.remove(message.getId());
        }
    }

    private void setupMessages(String testId, TestConfig testConfig) {
        try {
            for (int messageIndex = 0; messageIndex < testConfig.getSampleMessageCount(); messageIndex++) {
                if (!testRunning.get()) {
                    break;
                }
                Message message = prepareMessage(
                        messageIndex,
                        testConfig.getSampleMessageLineSize(),
                        testConfig.getSampleMessageLineCount(), testId);

                messageBuffer.put(message);
            }
        } catch (InterruptedException e) {
            log.warn("Failed to setup all messages due to exception", e);
        }
    }

    private Message prepareMessage(int messageIdx, int lineSize, int lineCount, String testId) {
        List<String> stringsPayload = IntStream.range(0, lineCount)
                .mapToObj(i -> randomAscii(lineSize))
                .collect(toList());
        Message message = new Message("id-" + testId + "-" + messageIdx, null, stringsPayload);
        log.debug("Message prepared {}", message);
        return message;
    }

    @Override
    public void accept(Message message) {
        String id = messageGetter.apply(message);
        if (!pendingRequests.containsKey(id)) {
            log.warn("Did not find message with id = {}", id);
        }

        log.debug("Removing message with id: {} from pending requests, pendingMessages count: {}", id, pendingRequests.size());
        Instant sentTime = pendingRequests.remove(id);
        Duration between = Duration.between(sentTime, Instant.now());
        updateMetrics(between);
    }
}

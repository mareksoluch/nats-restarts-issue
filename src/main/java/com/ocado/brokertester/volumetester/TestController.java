package com.ocado.brokertester.volumetester;

import com.ocado.brokertester.domain.TestConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class TestController {
    final private VolumeTester volumeTester;
    private final PrometheusMeterRegistry prometheusRegistry;

    @Autowired
    public TestController(VolumeTester volumeTester, PrometheusMeterRegistry prometheusRegistry) {
        this.volumeTester = volumeTester;
        this.prometheusRegistry = prometheusRegistry;
    }

    @PostMapping(path = "/startVolumeTestDefault")
    public String startTestDefault() {
        return volumeTester.startTest();
    }

    @PostMapping(path = "/startVolumeTest")
    public String startTest(@RequestBody TestConfig testConfig) {
        return volumeTester.startTest(testConfig);
    }

    @PostMapping(path = "/stopVolumeTest")
    public void stopTest() {
        volumeTester.stopTest();
    }

    @GetMapping(path = "/pendingRequests")
    public Map<String, Instant> getPendingRequests() {
        return volumeTester.getPendingRequests();
    }

    @GetMapping(path = "/pendingRequestsCount")
    public int getPendingRequestsCount() {
        return volumeTester.getPendingRequests().size();
    }

    @GetMapping(value = "/metrics", produces = "text/plain")
    public String getMetrics() {
        return prometheusRegistry.scrape();

    }

}

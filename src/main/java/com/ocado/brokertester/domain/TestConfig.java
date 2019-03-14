package com.ocado.brokertester.domain;

public class TestConfig {
    private int samplingThreadsCount = 1;
    private int samplingDelayMilliseconds = 1000;
    private int messagesChunkToSend = 1;
    private int sampleMessageCount = 10;
    private int sampleMessageLineCount = 10;
    private int sampleMessageLineSize = 1;

    public TestConfig() {
    }

    public TestConfig(int samplingThreadsCount, int samplingDelayMilliseconds, int messagesChunkToSend, int sampleMessageCount, int sampleMessageLineCount, int sampleMessageLineSize) {
        this.samplingThreadsCount = samplingThreadsCount;
        this.samplingDelayMilliseconds = samplingDelayMilliseconds;
        this.messagesChunkToSend = messagesChunkToSend;
        this.sampleMessageCount = sampleMessageCount;
        this.sampleMessageLineCount = sampleMessageLineCount;
        this.sampleMessageLineSize = sampleMessageLineSize;
    }

    public int getSamplingThreadsCount() {
        return samplingThreadsCount;
    }

    public int getSamplingDelayMilliseconds() {
        return samplingDelayMilliseconds;
    }

    public int getMessagesChunkToSend() {
        return messagesChunkToSend;
    }

    public int getSampleMessageCount() {
        return sampleMessageCount;
    }

    public int getSampleMessageLineCount() {
        return sampleMessageLineCount;
    }

    public int getSampleMessageLineSize() {
        return sampleMessageLineSize;
    }
}

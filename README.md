# Project description
This project tries to test a simple NATS use case scenario.
We have 2 queues: inputQueue and outputQueue. Input queue is used to buffer traffic before processing business logic.
After business logic is handled output message is sent to output queue in order to buffer it before sending it to external system.
In order to ensure that every message is processed in business logic we acknowledge input message after we correctly process and send output message (so received ack on sent).


Architecture overview:
```
VolumeTester --> (### inputQueue ###) --> Business Logic --> (### outputQueue ###) --> VolumeTester
```

## Test
In order to run test do following steps:
1. Build project
```
gradle assemble
```
2. Start docker-compose
```
docker-compose down && docker-compose build && docker-compose up -d
```
3. Run processed messages monitoring and messages in progress monitoring
```
watch "curl http://localhost:48081/pendingRequestsCount"
...
watch "curl http://localhost:48081/metrics | grep message_processing_count_total"
```
4. Start test by sending following config to test app
```
POST to http://localhost:48081/startVolumeTest 
BODY:
{
    "samplingThreadsCount": 10,
    "samplingDelayMilliseconds": 500,
    "messagesChunkToSend": 1,
    "sampleMessageCount": 10,
    "sampleMessageLineCount": 10,
    "sampleMessageLineSize": 64
}
```
5. Run restart loop that restarts NATS streaming nodes one after another
```
./restart_loop.sh
```
6. Monitor how many messages are in processing (monitored in point 3)

## My test results
I ran this test several times and it seems like after few restarts NATS Streaming cluster starts to continuously redeliver messages that failed to be put on input queue. 
We investigated several of those messages (processed by NATS cluster although no ACK received after send). 
Each of them was received around 10 times from input queue and sent to output queue. Additionally for those messages we application received around 20 messages form output queue (I always killed processing after ~2/3h after stopping test).
It seems like messages are not lost but cluster and app are flooded with those cloned redeliveries and processing never reaches messages correctly send afterwards (which tester is waiting for).

## Patches made
When I was trying to make this test pass I discovered 2 issues with java library which I patched just to push the test forward.
### Blocking publish fails on restarts
When I used blocking publish that should wait for ack it caused test to fail during restarts - I patched it in `NatsSender` using `CountDownLatch`.
### Reconnects fail
From time to time when cluster node restarts I got `NullPointerException` that broke connection and code could not reconnect. I had to implement reconnect logic that iterates over predefined list of NATS nodes on reconnects - `com.ocado.brokertester.volumetester.NatsSender.handleReceonnect`.

## Http endpoints
#### POST http://localhost:48081/startVolumeTest 
Starts volume test. Example body:
```
{
    "samplingThreadsCount": 10,
    "samplingDelayMilliseconds": 500,
    "messagesChunkToSend": 1,
    "sampleMessageCount": 10,
    "sampleMessageLineCount": 10,
    "sampleMessageLineSize": 64
}
```
#### POST http://localhost:48081/stopVolumeTest
Empty body. Stops test.
#### GET http://localhost:48081/pendingRequests
Gets pending requests
#### GET http://localhost:48081/pendingRequestsCount
Gets pending requests count
#### GET http://localhost:48081/metrics
Gets Prometheus metrics
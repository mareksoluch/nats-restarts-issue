
for ((i = 0; i < 100; ++i)); do
    A=$(($i % 3 + 1))
    DOCKER_TO_RESTART="nats-streaming-$A"
    echo "restarting docker $DOCKER_TO_RESTART"
    ./restart_docker.sh "$DOCKER_TO_RESTART"
    sleep 120
done

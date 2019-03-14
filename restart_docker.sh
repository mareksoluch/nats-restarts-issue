IMAGE_NAME=$1


DOCKER_TO_STOP=$(docker ps | grep  "$IMAGE_NAME"  | cut -d ' ' -f1)

echo "Stopping docker  $DOCKER_TO_STOP"

docker stop $DOCKER_TO_STOP

sleep 100

docker-compose up -d


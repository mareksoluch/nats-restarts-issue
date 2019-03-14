FROM openjdk:8-jdk-alpine
VOLUME /data
COPY 'build/libs/nats-restarts-issue-0.0.1-SNAPSHOT.jar' /app.jar
COPY 'src/main/resources/application.properties_example' '/data/application.properties'
ENTRYPOINT ["java","-Xmx2048m","-jar","/app.jar","--spring.config.location=/data/application.properties","--spring.profiles.active=full-tester,simple-tester"]
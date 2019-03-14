FROM internal.docker.ospcfc.tech.lastmile.com/cfcsystems/phoenix-core-base-image:1.11
VOLUME /data
COPY '../build/libs/broker-tester-0.0.1-SNAPSHOT.jar' /app.jar
COPY '../src/main/resources/application.properties_example' '/data/application.properties'
ENTRYPOINT ["java","-Xmx2048m","-jar","/app.jar","--spring.config.location=/data/application.properties","--spring.profiles.active=full-tester,simple-tester"]
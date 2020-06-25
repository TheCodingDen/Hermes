FROM openjdk:8 AS builder

WORKDIR /hermes
COPY . ./
RUN chmod +x ./gradlew && ./gradlew --no-daemon shadowJar

FROM openjdk:8-alpine

ENV token TOKEN
ENV configprovider com.kantenkugel.hermes.guildConfig.JSONGuildConfigProvider
ENV configproviderargs guildSettings.json

RUN mkdir hermes
WORKDIR /hermes
COPY --from=builder /hermes/build/libs/*-all.jar /hermes/hermes.jar

VOLUME /hermes/logs

ENTRYPOINT ["java", "-Xmx75M", "-jar", "hermes.jar"]
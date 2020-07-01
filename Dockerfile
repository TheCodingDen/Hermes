# Can be "builder" or "custom"
# builder first creates the shadowJar via gradle
# custom uses a provided Hermes-*-all.jar from the context root
# For performance concerns, it is STRONGLY adviced to use Docker Buildkit (DOCKER_BUILDKIT=1)
ARG buildtype=builder

### builder section

# Actually builds the jar
FROM openjdk:8 AS jarbuilder

WORKDIR /hermes
COPY . ./
RUN chmod +x ./gradlew && ./gradlew --no-daemon shadowJar

# base image using artifact from the jarbuilder
FROM openjdk:8-alpine AS builder

COPY --from=jarbuilder /hermes/build/libs/*-all.jar /hermes/hermes.jar

### custom section

# base image using provided jar
FROM openjdk:8-alpine AS custom

COPY ./Hermes-*-all.jar /hermes/hermes.jar

### Common section (using buildtype)

FROM $buildtype

WORKDIR /hermes

VOLUME /hermes/logs

ENV token TOKEN
ENV configprovider com.kantenkugel.hermes.guildConfig.JSONGuildConfigProvider
ENV configproviderargs guildSettings.json

ENTRYPOINT ["java", "-Xmx75M", "-jar", "hermes.jar"]
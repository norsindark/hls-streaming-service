FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

#COPY build.gradle settings.gradle gradlew ./
#COPY gradle ./gradle
#COPY spotless.gradle ./spotless.gradle

COPY . .

RUN ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk update && apk add bash && apk add ffmpeg

ENV TZ=UTC

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "app.jar"]

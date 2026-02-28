FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    apk del maven

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "target/dorm-power-backend-1.0.0.jar"]

# 在仓库根目录构建：docker build -f deploy/Dockerfile --build-arg MODULE=map-ingest .
ARG MODULE=map-ingest

FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /src
COPY pom.xml .
COPY backend backend
RUN mvn -B -DskipTests -pl backend/${MODULE} -am package \
    && cp backend/${MODULE}/target/${MODULE}-*.jar /tmp/app.jar

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /tmp/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

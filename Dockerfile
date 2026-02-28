ARG BUILDER_IMAGE=eclipse-temurin:21-jdk
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre
FROM ${BUILDER_IMAGE} AS build
WORKDIR /app

COPY pom.xml mvnw ./
COPY src ./src

RUN chmod +x ./mvnw && ./mvnw -DskipTests clean package

FROM ${RUNTIME_IMAGE}
WORKDIR /app

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms256m -Xmx1024m"

RUN ln -snf /usr/share/zoneinfo/${TZ} /etc/localtime && echo ${TZ} > /etc/timezone

COPY --from=build /app/target/patch-lifecycle-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar --spring.profiles.active=prod"]

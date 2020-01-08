FROM maven:3.6.3-jdk-11-openj9 as package

COPY pom.xml /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN mvn package

# BUILD TIME
FROM oracle/graalvm-ce:19.3.0-java11 as build

# Build native binary
RUN gu install native-image

WORKDIR /app

# Copy app
COPY --from=package /tmp/target/wait-for-url.jar .

RUN native-image --no-server \
    --static \
    -H:EnableURLProtocols=http,https \
    -cp wait-for-url.jar com.github.stephenc.utils.WaitForUrl wait-for-url

# RUNTIME
FROM scratch

COPY --from=build /app/wait-for-url .
ENTRYPOINT ["./wait-for-url"]
CMD ["--help"]

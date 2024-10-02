# Base Alpine Linux based image with OpenJDK JRE only
# docker build -t hav/ais-simulator:latest .
# docker run -d --name ais-simulator -p 8040:8040 -p 8041:8041 hav/ais-simulator:latest
FROM adoptopenjdk/openjdk11-openj9:jdk-11.0.24_8_openj9-0.46.1-alpine-slim
USER root
RUN mkdir -p /opt/ais && \
    adduser -D -h /opt/ais ais && \
    apk update && \
    apk add curl && \
    apk add libaio && \
    rm -rf /var/cache/apk/*

USER ais
WORKDIR /opt/ais

# was previously available at ftp://ftp.ais.dk/ais_data/aisdk_20190513.csv, but no longer
#COPY aisdk_20190513.csv aisdk_20190513.csv
RUN curl -LO https://web.ais.dk/aisdata/aisdk-2024-09-10.zip

COPY target/ais-simulator.jar ais-simulator.jar

CMD ["/opt/java/openjdk/bin/java", "-Dais_nth_pos=3", "-Dsim_file=aisdk-2024-09-10.zip", "-jar", "./ais-simulator.jar"]

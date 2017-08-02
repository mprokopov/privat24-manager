FROM openjdk:latest
RUN mkdir /app
WORKDIR /app
COPY target/uberjar/*-standalone.jar .
COPY resources resources
CMD java -jar privat-manager-1.1.1-standalone.jar
EXPOSE 3000

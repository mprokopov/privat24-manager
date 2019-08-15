# FROM clojure:alpine
FROM java:8-alpine
ARG VERSION
RUN mkdir -p /app /app/resources
WORKDIR /app
COPY target/uberjar/privat-manager-${VERSION}-standalone.jar privat-manager-standalone.jar
COPY resources/public resources/public
COPY resources/cache resources/cache
ENTRYPOINT java -jar privat-manager-standalone.jar
EXPOSE 3000

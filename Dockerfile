FROM clojure:alpine
RUN mkdir -p /app /app/resources
WORKDIR /app
COPY target/uberjar/*-standalone.jar .
COPY resources/public resources/public
COPY resources/cache resources/cache
CMD java -jar privat-manager-1.1.1-standalone.jar
EXPOSE 3000

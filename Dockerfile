FROM openjdk:8
RUN mkdir /etc/serivce 
COPY ./target/logging.jar /srv/logging.jar
WORKDIR /srv

ENTRYPOINT /usr/bin/java -Dconfig="/etc/service/logging_service.edn" -jar /srv/logging.jar



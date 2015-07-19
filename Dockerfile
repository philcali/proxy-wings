FROM java:8

COPY server/target/scala-2.11/carwings-server-assembly-1.0.0.jar /webapp/carwings-server.jar
WORKDIR /webapp
CMD ["java", "-jar", "carwings-server.jar", "80"]
EXPOSE 80

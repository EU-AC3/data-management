# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk

# Set the working directory inside the container
WORKDIR /usr/src/app

ENV HTTP_SERVER_PORT=4000

# Copy the current directory contents into the container at /usr/src/app
COPY target/http-files-message-bridge-1.0-SNAPSHOT.one-jar.jar .

# Specify the command to run the application
CMD java -jar http-files-message-bridge-1.0-SNAPSHOT.one-jar.jar

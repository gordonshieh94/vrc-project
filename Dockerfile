FROM java:openjdk-8-jdk
RUN apt-get update && apt-get install -y gradle

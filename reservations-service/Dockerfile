FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Duser.timezone=Europe/Bucharest", "-jar", "app.jar"]

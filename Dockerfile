FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY . .
RUN ./gradlew clean build -x check -x test
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
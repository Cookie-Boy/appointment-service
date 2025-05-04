FROM eclipse-temurin:21-jdk as builder

WORKDIR /app

# 1. Копируем только необходимые для сборки файлы
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# 2. Даем права и запускаем сборку
RUN chmod +x gradlew && ./gradlew clean build -x test

# 3. Финальный образ
FROM eclipse-temurin:21-jre
WORKDIR /app

# 4. Копируем только результат сборки
COPY --from=builder /app/build/libs/*.jar app.jar

# 5. Запуск с ограничением памяти
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]

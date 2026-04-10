FROM eclipse-temurin:21-jdk as builder

WORKDIR /app

# Копируем только файлы для загрузки зависимостей
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Даём права и загружаем зависимости (этот слой будет кэшироваться)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Теперь копируем исходный код и собираем
COPY src src
RUN ./gradlew clean build -x test --no-daemon

# Финальный образ
FROM eclipse-temurin:21-jre
WORKDIR /app

# Копируем собранный jar
COPY --from=builder /app/build/libs/*.jar app.jar

# Запуск с ограничением памяти
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
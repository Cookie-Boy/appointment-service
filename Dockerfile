FROM eclipse-temurin:21-jdk as builder

WORKDIR /app

# Копируем исходники и Gradle-файлы
COPY . .

# Устанавливаем кеш для Gradle (опционально)
RUN --mount=type=cache,target=/root/.gradle ./gradlew clean build -x check -x test
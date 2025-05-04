FROM eclipse-temurin:21-jdk

WORKDIR /app

# Копируем файлы с сохранением прав
COPY --chmod=755 gradlew .
COPY . .

# Даем права на выполнение (если предыдущий шаг не сработал)
RUN chmod +x gradlew

# Запускаем сборку
RUN ./gradlew clean build -x check -x test

# Копируем результат
COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
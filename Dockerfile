FROM openjdk:17-jdk-slim

VOLUME /tmp

# Создаем рабочую директорию
WORKDIR /app

# Копируем jar файл
COPY build/libs/*.jar app.jar

# Создаем директории для хранения данных
RUN mkdir -p /app/storage/books /app/storage/audio

# Устанавливаем переменные окружения
ENV SPRING_PROFILES_ACTIVE=production
ENV APP_STORAGE_ROOT_PATH=/app/storage

# Экспортируем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]
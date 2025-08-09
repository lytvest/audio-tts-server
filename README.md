# Audio TTS Server

Сервер для автоматической озвучки аудиокниг с поддержкой FB2 формата, определением персонажей и расстановкой ударений.

## 🎯 Описание

Audio TTS Server - это комплексное решение для автоматической генерации аудиокниг из текстовых файлов FB2. Система автоматически:

- Парсит FB2 файлы и разбивает на главы и предложения
- Определяет персонажей для каждого предложения через Ollama AI
- Расставляет ударения в русском тексте
- Генерирует аудио с помощью F5-TTS
- Предоставляет готовые аудио файлы для скачивания

## ✨ Возможности

- 📚 **Загрузка FB2 файлов** с автоматическим парсингом
- 🤖 **AI-определение персонажей** через Ollama
- 📝 **Автоматическая расстановка ударений** 
- 🎵 **Генерация качественного аудио** через F5-TTS
- 👥 **Управление персонажами и голосами**
- 📊 **Мониторинг прогресса обработки**
- 📁 **Скачивание по предложениям, главам или книге целиком**
- ⚡ **Асинхронная обработка с очередями**
- 🔐 **Базовая HTTP авторизация** (admin/admin)

## 🛠️ Технологии

- **Backend**: Spring Boot 3.2, Java 17
- **Database**: H2 (dev) / PostgreSQL (prod)
- **Build**: Gradle 8.4
- **AI Services**: Ollama (для определения персонажей и ударений)
- **TTS**: F5-TTS (локальный сервис)
- **Queuing**: In-memory BlockingQueue с семафорами
- **Security**: Spring Security с in-memory пользователем

## 📋 Требования

- Java 17+
- Gradle 8.4+
- Ollama сервер (localhost:11434)
- F5-TTS сервер (localhost:5000)
- PostgreSQL (для production)

## 🚀 Установка и запуск

### Локальный запуск

```bash
# Клонировать репозиторий
git clone https://github.com/lytvest/audio-tts-server.git
cd audio-tts-server

# Запустить с H2 базой данных
./gradlew bootRun

# Или запустить с PostgreSQL
./gradlew bootRun --args='--spring.profiles.active=postgresql'
```

### Docker запуск

```bash
# Сборка приложения
./gradlew build

# Запуск через Docker Compose (включает PostgreSQL, Ollama)
docker-compose up -d

# Загрузка модели в Ollama (после запуска контейнеров)
docker exec -it <ollama_container> ollama pull llama3.2
```

## 📖 API Documentation

### Авторизация
Все API требуют HTTP Basic Auth: `admin:admin`

### Загрузка книг

**Загрузить FB2 файл:**
```http
POST /api/books/upload
Content-Type: multipart/form-data
Authorization: Basic YWRtaW46YWRtaW4=

file: (FB2 файл)
title: "Название книги" (опционально)
author: "Автор" (опционально)
```

**Получить список книг:**
```http
GET /api/books
Authorization: Basic YWRtaW46YWRtaW4=
```

**Получить книгу с главами:**
```http
GET /api/books/{bookId}
Authorization: Basic YWRtaW46YWRtaW4=
```

**Получить статистику обработки:**
```http
GET /api/books/{bookId}/stats
Authorization: Basic YWRtaW46YWRtaW4=
```

### Управление предложениями

**Получить предложения по статусу:**
```http
GET /api/sentences?status=WAITING_FOR_CHARACTER&page=0&size=20
Authorization: Basic YWRtaW46YWRtaW4=
```

**Предложения главы:**
```http
GET /api/sentences/chapter/{chapterId}
Authorization: Basic YWRtaW46YWRtaW4=
```

**Скачать аудио предложения:**
```http
GET /api/sentences/{sentenceId}/audio
Authorization: Basic YWRtaW46YWRtaW4=
```

### Управление персонажами

**Получить персонажей книги:**
```http
GET /api/characters/book/{bookId}
Authorization: Basic YWRtaW46YWRtaW4=
```

**Обновить настройки персонажа:**
```http
PUT /api/characters/{characterId}
Content-Type: application/json
Authorization: Basic YWRtaW46YWRtaW4=

{
  "voiceId": "russian_female",
  "voiceName": "Русский женский голос",
  "description": "Описание персонажа"
}
```

### Скачивание аудио

**Скачать главу:**
```http
GET /api/chapters/{chapterId}/audio
Authorization: Basic YWRtaW46YWRtaW4=
```

**Скачать книгу как ZIP:**
```http
GET /api/chapters/book/{bookId}/download
Authorization: Basic YWRtaW46YWRtaW4=
```

### Системные API

**Проверка здоровья:**
```http
GET /api/health
```

**Доступные голоса:**
```http
GET /api/voices
Authorization: Basic YWRtaW46YWRtaW4=
```

**Статистика очередей:**
```http
GET /api/queue/stats
Authorization: Basic YWRtaW46YWRtaW4=
```

## 🔧 Конфигурация

### application.properties

```properties
# Сервер
server.port=8080

# База данных (H2 для разработки)
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true

# Хранилище файлов
app.storage.root-path=./storage
app.storage.books-path=${app.storage.root-path}/books
app.storage.audio-path=${app.storage.root-path}/audio

# Внешние сервисы
app.ollama.base-url=http://localhost:11434
app.ollama.model=llama3.2
app.f5tts.base-url=http://localhost:5000

# Очереди
app.processing.sentence-queue-size=100
app.processing.chapter-queue-size=50
```

### Переменные окружения

```bash
# Для production
export SPRING_PROFILES_ACTIVE=postgresql
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/audio_tts
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=password

# Внешние сервисы
export APP_OLLAMA_BASE_URL=http://ollama:11434
export APP_F5TTS_BASE_URL=http://f5tts:5000
```

## 📊 Статусная модель

### Статусы предложений:
1. `WAITING_FOR_CHARACTER` - ожидает определения персонажа
2. `DETERMINING_CHARACTER` - определение персонажа в процессе
3. `WAITING_FOR_STRESS` - ожидает расстановки ударений
4. `SETTING_STRESS` - расстановка ударений в процессе
5. `WAITING_FOR_TTS` - ожидает генерации аудио
6. `GENERATING_TTS` - генерация аудио в процессе
7. `READY` - готово

### Статусы глав:
1. `WAITING` - ожидает обработки
2. `IN_PROGRESS` - в процессе обработки
3. `READY` - готова

## 🏗️ Архитектура

```
┌─────────────────┐    ┌──────────────┐    ┌─────────────┐
│   REST API      │    │   Services   │    │  External   │
│                 │    │              │    │  Services   │
│ BookController  │───▶│ BookService  │───▶│   Ollama    │
│SentenceCtrl     │    │SentenceProc. │    │   F5-TTS    │
│ChapterCtrl      │    │ChapterService│    │             │
└─────────────────┘    └──────────────┘    └─────────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌──────────────┐
│   Repositories  │    │   Queues     │
│                 │    │              │
│ BookRepository  │    │Character     │
│SentenceRepo     │    │Stress        │
│ChapterRepo      │    │TTS           │
│CharacterRepo    │    │              │
└─────────────────┘    └──────────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌──────────────┐
│    Database     │    │ Async        │
│                 │    │ Processors   │
│ H2 / PostgreSQL │    │              │
└─────────────────┘    └──────────────┘
```

## 🔄 Процесс обработки

1. **Загрузка FB2** → Парсинг → Создание Book/Chapter/Sentence
2. **Определение персонажей** → Ollama API → Обновление Sentence
3. **Расстановка ударений** → Ollama API → Обновление текста
4. **Генерация аудио** → F5-TTS API → Сохранение MP3
5. **Готовность** → Доступны для скачивания

## 🐳 Docker

### Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml
Включает PostgreSQL и заготовки для Ollama и F5-TTS сервисов.

## 🤝 Разработка

### Структура проекта:
```
src/main/java/com/lytvest/audiotts/
├── controller/          # REST контроллеры
├── service/            # Бизнес-логика
│   ├── external/       # Интеграции с внешними API
│   ├── processor/      # Асинхронные обработчики
│   └── queue/          # Управление очередями
├── model/              # Модели данных
│   ├── entity/         # JPA сущности
│   └── enums/          # Перечисления
├── repository/         # Репозитории JPA
├── dto/               # DTO для API
└── config/            # Конфигурация Spring
```

### Запуск тестов:
```bash
./gradlew test
```

### Сборка:
```bash
./gradlew build
```

## 📄 Лицензия

Этот проект распространяется под лицензией MIT. Подробности в файле [LICENSE](LICENSE).

## 👨‍💻 Автор

- **Дмитрий Башкирцев** - [lytvest](https://github.com/lytvest)

## 📞 Поддержка

Если у вас есть вопросы или предложения:
- Создайте [issue](https://github.com/lytvest/audio-tts-server/issues)
- Посмотрите логи: `docker-compose logs -f audio-tts-server`
- Проверьте статус очередей: `GET /api/queue/stats`

## 🔮 Roadmap

- [ ] Веб-интерфейс для управления
- [ ] Поддержка других форматов (EPUB, TXT)
- [ ] Улучшенное объединение аудио файлов
- [ ] Кэширование определений персонажей
- [ ] Метрики и мониторинг
- [ ] API для управления голосами

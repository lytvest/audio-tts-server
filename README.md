# AudioTTS Server 🎧

**Современный сервер для создания аудиокниг с использованием технологий Text-to-Speech**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 🌟 Особенности

- 📚 **Обработка FB2 файлов** - автоматический парсинг электронных книг
- 🎭 **Определение персонажей** - ИИ-анализ текста для выявления говорящих персонажей
- 🗣️ **Множественные голоса** - назначение уникальных голосов разным персонажам
- ⚡ **Асинхронная обработка** - очереди для эффективной генерации аудио
- 🎨 **Современный веб-интерфейс** - удобное управление через браузер
- 🔄 **REST API** - полноценный API для интеграции
- 🐳 **Docker поддержка** - легкое развертывание

## 🚀 Быстрый старт

### Предварительные требования

- Java 17+
- Docker (опционально)
- Gradle 7.5+

### Установка и запуск

1. **Клонирование репозитория**
   ```bash
   git clone https://github.com/lytvest/audio-tts-server.git
   cd audio-tts-server
   ```

2. **Запуск с помощью Gradle**
   ```bash
   ./gradlew bootRun
   ```

3. **Или с помощью Docker**
   ```bash
   docker-compose up --build
   ```

4. **Открыть веб-интерфейс**
   ```
   http://localhost:8080
   ```

## 🎯 Веб-интерфейс

### Основные страницы

| URL | Описание | Функциональность |
|-----|----------|------------------|
| `/` | Главная → Dashboard | Автоматическое перенаправление |
| `/web/dashboard` | 📊 Дашборд | Обзор системы, статистика |
| `/web/books` | 📚 Управление книгами | Список, фильтрация, загрузка |
| `/web/books/upload` | ⬆️ Загрузка | Drag & drop загрузка FB2 |
| `/web/books/{id}` | 📖 Детали книги | Прогресс, статистика |
| `/web/books/{id}/characters` | 🎭 Персонажи | Назначение голосов |
| `/web/sentences` | 📝 Предложения | Мониторинг обработки |
| `/web/queues` | ⚡ Очереди | Real-time мониторинг |
| `/simple/dashboard` | 🔧 Простой интерфейс | Для тестирования |

### Возможности интерфейса

#### 📊 **Dashboard**
- Общая статистика по книгам
- Прогресс обработки в реальном времени
- Мониторинг очередей
- Быстрый доступ к последним книгам

#### 📚 **Управление книгами**
- Современный список с карточками
- Фильтрация по статусу (завершенные, в обработке, ожидающие)
- Drag & drop загрузка FB2 файлов
- Детальная информация по каждой книге

#### 🎭 **Управление персонажами**
- Автоматическое определение персонажей
- Назначение голосов из доступных
- Предварительный просмотр голосов

#### 📝 **Мониторинг предложений**
- Отслеживание статуса каждого предложения
- Фильтрация по статусам
- Воспроизведение готовых аудио
- Информация об ошибках

## 🔧 API Endpoints

### Управление книгами
```http
POST /api/books/upload          # Загрузка новой книги
GET  /api/books                 # Список всех книг
GET  /api/books/{id}            # Информация о книге
DELETE /api/books/{id}          # Удаление книги
POST /api/books/{id}/restart    # Перезапуск обработки
```

### Персонажи и голоса
```http
GET  /api/books/{id}/characters        # Персонажи книги
POST /api/characters/{id}/voice        # Назначение голоса
GET  /api/voices                       # Доступные голоса
```

### Предложения
```http
GET  /api/sentences                    # Список предложений
GET  /api/sentences/{id}/audio         # Скачать аудио
```

### Системная информация
```http
GET  /api/system/status               # Статус системы
GET  /api/system/queues               # Статистика очередей
```

## ⚙️ Конфигурация

### application.properties

```properties
# Сервер
server.port=8080

# База данных (H2 для разработки)
spring.datasource.url=jdbc:h2:mem:audiotts
spring.datasource.username=sa
spring.datasource.password=password

# TTS Сервис
app.tts.base-url=http://localhost:8765
app.tts.timeout=30s

# Хранилище файлов
app.storage.audio-path=./storage/audio
app.storage.books-path=./storage/books

# Очереди
app.queue.audio-generation.core-pool-size=2
app.queue.character-detection.core-pool-size=1
app.queue.book-parsing.core-pool-size=1
```

### Docker Compose

```yaml
version: '3.8'
services:
  audiotts-server:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    volumes:
      - ./storage:/app/storage
```

## 🏗️ Архитектура

### Компоненты системы

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web UI        │    │   REST API      │    │   File Storage  │
│  (Thymeleaf)    │◄──►│  (Spring Web)   │◄──►│   (Local/S3)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         ▲                        ▲                        ▲
         │                        │                        │
         ▼                        ▼                        ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controllers   │    │    Services     │    │   Repositories  │
│  (Web + API)    │◄──►│  (Business)     │◄──►│     (JPA)       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                ▲
                                │
                                ▼
                    ┌─────────────────┐
                    │   Queue System  │
                    │  (Async Tasks)  │
                    └─────────────────┘
                                ▲
                                │
                                ▼
                    ┌─────────────────┐
                    │   TTS Service   │
                    │  (External)     │
                    └─────────────────┘
```

### Процесс обработки книги

1. **📤 Загрузка**: Пользователь загружает FB2 файл
2. **📖 Парсинг**: Извлечение текста и структуры
3. **✂️ Сегментация**: Разбиение на предложения
4. **🤖 ИИ-анализ**: Определение персонажей
5. **🎭 Назначение голосов**: Ручное или автоматическое
6. **🗣️ Генерация аудио**: TTS для каждого предложения
7. **🔧 Сборка**: Создание финальной аудиокниги

## 🛠️ Разработка

### Структура проекта

```
src/
├── main/
│   ├── java/com/lytvest/audiotts/
│   │   ├── controller/          # REST API и Web контроллеры
│   │   │   ├── api/            # REST API endpoints
│   │   │   └── web/            # Web UI контроллеры
│   │   ├── service/            # Бизнес-логика
│   │   ├── model/              # JPA сущности
│   │   ├── dto/                # Data Transfer Objects
│   │   ├── config/             # Конфигурация Spring
│   │   └── repository/         # JPA репозитории
│   └── resources/
│       ├── templates/          # Thymeleaf шаблоны
│       │   ├── layout/         # Базовые макеты
│       │   ├── books/          # Страницы книг
│       │   ├── sentences/      # Мониторинг предложений
│       │   └── queues/         # Мониторинг очередей
│       ├── static/             # CSS, JS, изображения
│       └── application.properties
└── test/                       # Тесты
```

### Технологический стек

**Backend:**
- ☕ **Java 17** - основной язык
- 🍃 **Spring Boot 3.2** - фреймворк
- 🗃️ **Spring Data JPA** - работа с БД
- 🔒 **Spring Security** - безопасность
- 📝 **Thymeleaf** - шаблонизатор
- 📊 **H2/PostgreSQL** - база данных

**Frontend:**
- 🎨 **Tailwind CSS** - стилизация
- ⚡ **Alpine.js** - интерактивность
- 🌐 **Thymeleaf Layout Dialect** - макеты

**Инфраструктура:**
- 🐳 **Docker** - контейнеризация
- 📄 **Gradle** - сборка проекта
- 🔄 **Spring Integration** - асинхронные очереди

### Запуск в режиме разработки

```bash
# Режим разработки с автоперезагрузкой
./gradlew bootRun --args='--spring.profiles.active=dev'

# Консоль H2 базы данных
http://localhost:8080/h2-console
```

## 📋 TODO

- [ ] 🔊 Улучшение качества TTS
- [ ] 📱 Мобильная версия интерфейса
- [ ] 🎵 Поддержка звуковых эффектов
- [ ] 📈 Расширенная аналитика
- [ ] 🔌 Интеграция с внешними TTS API
- [ ] 📚 Поддержка других форматов книг (EPUB, TXT)
- [ ] 🎛️ Настройки качества аудио
- [ ] 👥 Многопользовательский режим

## 🤝 Участие в разработке

1. Fork проекта
2. Создайте feature branch (`git checkout -b feature/amazing-feature`)
3. Commit изменения (`git commit -m 'Add amazing feature'`)
4. Push в branch (`git push origin feature/amazing-feature`)
5. Откройте Pull Request

## 📄 Лицензия

Проект распространяется под лицензией MIT. Подробности в файле [LICENSE](LICENSE).

## 🆘 Поддержка

- 📧 Email: bdm1999@yandex.ru
- 🐛 Issues: [GitHub Issues](https://github.com/lytvest/audio-tts-server/issues)
- 📖 Wiki: [GitHub Wiki](https://github.com/lytvest/audio-tts-server/wiki)

---

**Создано с ❤️ для любителей аудиокниг**
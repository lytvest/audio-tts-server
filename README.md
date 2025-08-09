# Audio TTS Server

Сервер для преобразования текста в речь (Text-to-Speech) с поддержкой различных языков и голосов.

## 🎯 Описание

Audio TTS Server - это веб-сервер, предоставляющий REST API для синтеза речи из текста. Поддерживает множество языков и голосовых моделей для создания качественного аудио контента.

## ✨ Возможности

- 🗣️ Преобразование текста в речь
- 🌍 Поддержка множества языков (русский, английский, и др.)
- 🎵 Различные голосовые модели
- 📁 Экспорт в популярные аудио форматы (MP3, WAV)
- 🚀 REST API для интеграции
- ⚡ Быстрая обработка запросов

## 🛠️ Технологии

- Backend: (указать используемый язык/фреймворк)
- TTS Engine: (указать используемую TTS библиотеку)
- API: REST
- Audio formats: MP3, WAV

## 📋 Требования

- (указать системные требования)
- (указать зависимости)

## 🚀 Установка и запуск

```bash
# Клонировать репозиторий
git clone https://github.com/lytvest/audio-tts-server.git
cd audio-tts-server

# Установить зависимости
# (команды установки)

# Запустить сервер
# (команда запуска)
```

## 📖 API Documentation

### Синтез речи

```http
POST /api/tts
Content-Type: application/json

{
  "text": "Привет, мир!",
  "language": "ru",
  "voice": "default",
  "format": "mp3"
}
```

**Ответ:**
- Статус: 200 OK
- Content-Type: audio/mpeg (для MP3) или audio/wav (для WAV)
- Body: аудио файл

### Получить список доступных голосов

```http
GET /api/voices
```

**Ответ:**
```json
{
  "voices": [
    {
      "id": "ru-default",
      "name": "Русский (по умолчанию)",
      "language": "ru"
    },
    {
      "id": "en-default", 
      "name": "English (default)",
      "language": "en"
    }
  ]
}
```

## 🔧 Конфигурация

Создайте файл `config.json` для настройки сервера:

```json
{
  "port": 8080,
  "host": "localhost",
  "tts": {
    "default_language": "ru",
    "default_voice": "default",
    "audio_quality": "high"
  }
}
```

## 📝 Примеры использования

### Python

```python
import requests

response = requests.post('http://localhost:8080/api/tts', json={
    'text': 'Привет, мир!',
    'language': 'ru',
    'format': 'mp3'
})

with open('output.mp3', 'wb') as f:
    f.write(response.content)
```

### JavaScript

```javascript
const response = await fetch('http://localhost:8080/api/tts', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        text: 'Hello, world!',
        language: 'en',
        format: 'mp3'
    })
});

const audioBlob = await response.blob();
const audioUrl = URL.createObjectURL(audioBlob);
```

## 🤝 Вклад в проект

1. Сделайте форк проекта
2. Создайте ветку для новой функции (`git checkout -b feature/new-feature`)
3. Зафиксируйте изменения (`git commit -am 'Добавил новую функцию'`)
4. Отправьте в ветку (`git push origin feature/new-feature`)
5. Создайте Pull Request

## 📄 Лицензия

Этот проект распространяется под лицензией MIT. Подробности в файле [LICENSE](LICENSE).

## 👨‍💻 Автор

- **Дмитрий Башкирцев** - [lytvest](https://github.com/lytvest)

## 📞 Поддержка

Если у вас есть вопросы или предложения, создайте [issue](https://github.com/lytvest/audio-tts-server/issues) или свяжитесь со мной.

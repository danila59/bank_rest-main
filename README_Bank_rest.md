
# Bank Cards Management System

Backend-приложение на Java Spring Boot для управления банковскими картами: создание, хранение, переводы, безопасность и администрирование.

---

## Быстрый старт

### 1. Клонирование проекта

```bash
git clone https://github.com/danila59/bank-rest-main.git
cd bank-rest-main
```

### 2. Настройка переменных окружения

```bash
# Скопируйте пример конфигурации
cp .env.example .env

# Заполните .env своими секретными ключами
```

### 3. Запуск через Docker Compose (рекомендуется)

```bash
docker-compose up --build -d
docker-compose logs -f bank-app
```

### 4. Локальный запуск (режим разработки)

```bash
docker-compose up -d postgres
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---
### 5. Скачать openapi.yaml документацию на api
http://localhost:8080/api/v3/api-docs.yaml



---
## Доступные сервисы

| Сервис      | URL                                                                                    | Описание          |
| ----------- | -------------------------------------------------------------------------------------- | ----------------- |
| Backend API | [http://localhost:8080/api](http://localhost:8080/api)                                 | Основное REST API |
| Swagger UI  | [http://localhost:8080/api/swagger-ui.html](http://localhost:8080/api/swagger-ui.html) | Документация API  |
| pgAdmin     | [http://localhost:5050](http://localhost:5050)                                         | Управление БД     |
| PostgreSQL  | localhost:5432                                                                         | База данных       |

**Учётные данные pgAdmin:**
Email: `admin@bank.com`
Пароль: `admin123`

---

## Тестовые пользователи

### Пользователь (ROLE_USER)

Логин: `testuser`
Пароль: `password`

Тестовые карты:

* 4149471805568597 — баланс 5000.00, CVV: 244
* 4149476684550065 — баланс 3000.00, CVV: 883

### Администратор (ROLE_ADMIN)

Логин: `admin`
Пароль: `admin123`

---

## API Endpoints

### Аутентификация

| Метод | Endpoint             | Описание                        |
| ----- |----------------------| ------------------------------- |
| POST  | `/api/auth/login`    | Получение JWT токена            |
| POST  | `/api/auth/register` | Регистрация нового пользователя |
| POST  | `/api/auth/me`        |Получить информацию о текущем аутентифицированном пользователе |

### Управление картами

| Метод  | Endpoint                            | Описание                   |
|--------| ----------------------------------- | -------------------------- |
| GET    | `/api/cards`                        | Получить список карт текущего пользователя с пагинацией и фильтрацией|
| GET    | `/api/cards/{cardId}`               |Получить информацию о конкретной карте|
| POST   | `/api/cards/request-block/{cardId}` | Пользователь запрашивает блокировку своей карты |
| GET    | `/{cardId}/balance` | Получить текущий баланс карты |
| POST    | `/{cardId}/activate` | Активировать заблокированную карту (если не истек срок) |
| POST    | `/api/cards/generate` | Создать новую карту для текущего пользователя |

### Переводы

| Метод | Endpoint                     | Описание              |
| ----- | ---------------------------- | --------------------- |
| POST  | `/api/transactions/transfer` | Перевод между картами |
| GET  | `/api/transactions` | Получить историю транзакций текущего пользователя |
| GET  | `/api/transactions/card/{cardId}` | Получить историю транзакций конкретной карты |
| GET  | `/api/transactions/{transactionId}` | Получить детали конкретной транзакции |
| GET  | `/api/transactions/card/{cardId}/daily-limit` | Проверить использованный дневной лимит для карты |

### Управление картами админом

| Метод  | Endpoint                           | Описание                  |
|--------|------------------------------------| ------------------------- |
| POST   | `/api/admin/cards`                 | Создать новую карту для пользователя (только для администратора)|
| GET    | `/api/admin/cards`                 | Получить список всех карт в системе (с пагинацией)|
| GET    | `/api/admin/cards/{cardId}`        | Получить информацию о любой карте в системе|
| PUT    | `/api/admin/cards/{cardId}/status` | Изменить статус карты (активировать/заблокировать)|
| DELETE | `/api/admin/cards/{cardId}`        |Удалить карту из системы (только если баланс = 0)|
| GET    | `/api/admin/user/{cardId}`         |Получить все карты конкретного пользователя|
| POST   | `/api/admin/{cardId}/force-block`  |Блокировка карты администратором|
| POST   | `/api/admin/check-expired`         |Запустить проверку истекших карт|

---

### Управление пользователями админом

| HTTP | Endpoint                                     | Описание                           | Тело запроса  | Тело ответа           | Доступ       |
| --- | -------------------------------------------- | ---------------------------------- | ------------- | --------------------- | ------------ |
| GET | `/api/admin/users`                           | Получить всех пользователей        | —             | Список `UserDTO`      | Только ADMIN |
| GET | `/api/admin/users/{userId}`                  | Получить пользователя по ID        | —             | `UserDTO`             | Только ADMIN |
| PUT | `/api/admin/users/{userId}`                  | Обновить данные пользователя       | `User` (JSON) | Обновлённый `UserDTO` | Только ADMIN |
| POST | `/api/admin/users/{userId}/disable`          | Отключить пользователя             | —             | `null` (успех)        | Только ADMIN |
| POST | `/api/admin/users/{userId}/enable`           | Включить пользователя              | —             | `null` (успех)        | Только ADMIN |
| GET | `/api/admin/users/check-username/{username}` | Проверить — существует ли username | —             | `boolean`             | Только ADMIN |
| GET | `/api/admin/users/check-email/{email}`       | Проверить — существует ли email    | —             | `boolean`             | Только ADMIN |

---

## Технологический стек

### Backend

* Java 17
* Spring Boot 3.x
* Spring Security + JWT
* Spring Data JPA
* Springdoc OpenAPI

### Database

* PostgreSQL 15
* Liquibase
* HikariCP

### Безопасность

* AES-256 шифрование данных карт
* BCrypt хеширование паролей
* JWT авторизация
* CORS настройки

### Инфраструктура

* Docker + Docker Compose
* Maven
* Git

---

## Конфигурация

### Конфигурационные файлы

* `application.yml`
* `application-dev.yml`

### Пример .env

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=bank_db
DB_USERNAME=bank_user
DB_PASSWORD=bank_password

# Security
JWT_SECRET=your-32-character-jwt-secret-key-here
ENCRYPTION_SECRET=your-32-character-encryption-key-here

```

---

## Docker команды

```bash
docker-compose up --build
docker-compose up -d
docker-compose down
docker-compose down -v
docker-compose logs -f bank-app
docker-compose logs -f postgres
docker-compose build bank-app
```

---

## Структура проекта

```
bank-rest-main/
├── docs/                         # Документация, OpenAPI, схемы
├── logs/                         # Логи приложения (если включены)
├── src/
│   ├── main/
│   │   ├── java/com/example/bankcards/
│   │   │   ├── config/           # Конфигурации Spring Boot
│   │   │   ├── controller/       # REST-контроллеры
│   │   │   ├── dto/              # DTO классы
│   │   │   ├── entity/           # Сущности JPA
│   │   │   │   └── enums/        # Перечисления
│   │   │   ├── exception/        # Кастомные исключения и обработчики
│   │   │   ├── mapper/           # Mапперы
│   │   │   ├── repository/       # Репозитории Spring Data JPA
│   │   │   ├── scheduler/        # Планировщики задач
│   │   │   ├── security/         # JWT, фильтры, конфигурация безопасности
│   │   │   ├── service/          # Бизнес-логика
│   │   │   └── util/             # Вспомогательные утилиты
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/changelog/     # Миграции Liquibase
│   └── test/
│       └── java/com/example/bankcards/
│           ├── controller/       # Тесты контроллеров
│           └── service/          # Тесты сервисов
└── docker-compose.yml

```

---

## Безопасность

### Шифрование данных

* Номера карт — AES-256
* CVV — зашифрованы
* Пароли — BCrypt

### API защита

* JWT токены
* Роли USER / ADMIN
* Глобальная обработка ошибок
* Валидация входных данных

### Безопасность БД

* Prepared Statements
* Транзакции
* Индексы

---

## Тестирование

### Пример получения токена

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password"}'
```

### Пример перевода

```bash
curl -X POST http://localhost:8080/api/transactions/transfer \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "fromCardNumber": "4149471805568597",
    "toCardNumber": "4149476684550065",
    "amount": 100.00,
    "description": "Тестовый перевод",
    "cvv": "244"
  }'
```

---

## Миграции базы данных (Liquibase)

* Автоматическое применение изменений
* История миграций
* Возможность отката

---

## Формат ошибок

```json
{
  "timestamp": "2024-01-14T15:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid card number",
  "path": "/api/transactions/transfer"
}
```

---

## Установка для разработки

```bash
git clone https://github.com/danila59/bank-rest-main.git
cd bank-rest-main

mvn clean install
docker-compose up -d postgres

mvn test

```

### Code Style

* Java 17
* Lombok
* REST best practices
* Чистая архитектура

---

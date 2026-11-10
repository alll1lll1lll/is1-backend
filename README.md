# is1backend — ИС управления объектами Route (вариант 77421)

Стек: Spring Boot 3.3 (Spring MVC), Spring Security + JWT, JPA + EclipseLink 4, PostgreSQL, WebSocket (STOMP).

## Запуск

```bash
# локально
DB_URL=jdbc:postgresql://localhost:5432/studs DB_USER=postgres DB_PASSWORD=*** ./gradlew bootRun

# на кафедральном сервере
DB_URL=jdbc:postgresql://pg:5432/studs DB_USER=sXXXXXX DB_PASSWORD=*** java -jar build/libs/is1backend-1.0.0.jar
```

Таблицы создаются автоматически скриптом `src/main/resources/schema.sql`
(`CREATE TABLE IF NOT EXISTS`, все ограничения полей продублированы как `NOT NULL` / `CHECK`).

Переменные окружения: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT` (8080), `CORS_ORIGINS`,
`JWT_SECRET` (base64, не короче 256 бит; если не задан, при запуске генерируется случайный ключ),
`JWT_ACCESS_TTL` (по умолчанию `PT15M`), `JWT_REFRESH_TTL` (по умолчанию `P7D`),
`JWT_CLEANUP_CRON` (очистка просроченных refresh-токенов, по умолчанию `0 0 3 * * *`).

Тесты (`./gradlew test`) поднимают встроенный PostgreSQL, внешняя БД не нужна.

## Авторизация

| Метод | URL | Доступ | Описание |
|---|---|---|---|
| POST | `/api/auth/register` | все | `{"username","password"}` → 201 и пара токенов |
| POST | `/api/auth/login` | все | `{"username","password"}` → пара токенов |
| POST | `/api/auth/refresh` | все | `{"refreshToken"}` → новая пара токенов |
| POST | `/api/auth/logout` | все | `{"refreshToken"}` → 204, refresh-токен отзывается |
| GET | `/api/auth/me` | с access-токеном | `{"username"}` текущего пользователя |

Ответ регистрации, входа и обновления:

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJ...",
  "accessTokenExpiresAt": "2026-11-10T18:15:00Z",
  "refreshToken": "q3V1...",
  "refreshTokenExpiresAt": "2026-11-17T18:00:00Z",
  "username": "alice"
}
```

- **Access-токен** — JWT на 15 минут. Передаётся в `Authorization: Bearer <accessToken>` во всех запросах к `/api/**`
  и в кадре STOMP CONNECT. Проверяется без обращения к БД.
- **Refresh-токен** — случайная строка на 7 дней. В таблице `refresh_token` хранится только её SHA-256-хеш.
  Используется только для `/api/auth/refresh` и `/api/auth/logout`, как access-токен не принимается.
- **Ротация**: каждый `/refresh` отзывает предъявленный refresh-токен и выдаёт новую пару.
- **Защита от кражи**: если предъявлен уже отозванный refresh-токен, отзываются все refresh-токены пользователя,
  и ему придётся войти заново.
- Просроченные refresh-токены удаляются из БД ежедневно в 03:00.

Клиенту при ответе 401 на обычный запрос нужно один раз вызвать `/api/auth/refresh` и повторить запрос;
если и `/refresh` вернул 401 — отправить пользователя на страницу входа.
Пароли хранятся как BCrypt-хеш в таблице `app_user`. Логин: 3–50 символов (`A-Z a-z 0-9 . _ -`), пароль: 6–72 символа.

## REST API

| Метод | URL | Описание |
|---|---|---|
| GET | `/api/routes?page=0&size=10&sort=name,asc&name=&fromName=&toName=` | таблица с пагинацией, сортировкой и фильтром (подстрока, без учёта регистра) |
| GET | `/api/routes/{id}` | маршрут вместе со связанными объектами |
| POST | `/api/routes` | создать |
| PUT | `/api/routes/{id}` | обновить |
| DELETE | `/api/routes/{id}` | удалить |
| GET/POST/PUT/DELETE | `/api/locations[/{id}]` | локации |
| GET/POST/PUT/DELETE | `/api/coordinates[/{id}]` | координаты |
| GET | `/api/special/max-from` | любой маршрут с максимальным `from` (204, если нет) |
| GET | `/api/special/count-by-rating?rating=4.5` | `{"count": N}` |
| GET | `/api/special/name-contains?substring=abc` | маршруты, чьё имя содержит подстроку |
| GET | `/api/special/between?fromId=1&toId=2&mode=SHORTEST\|LONGEST` | самый короткий/длинный маршрут между локациями |
| POST | `/api/special/between` | добавить маршрут между локациями |

Поля сортировки: `id, name, creationDate, distance, rating, coordinates.x, coordinates.y, from.name, from.x, from.y, to.name, to.x, to.y`.

Тело `POST/PUT /api/routes`: связанные объекты задаются ссылкой на существующий (`{"id": 3}`)
или значениями для создания нового:

```json
{
  "name": "Route one",
  "coordinates": {"x": 5, "y": 6},
  "from": {"id": 1},
  "to": {"x": 10, "y": 2.5, "name": "Beta"},
  "distance": 100,
  "rating": 4.5
}
```

`POST /api/special/between`: `{"fromId":1,"toId":2,"name":"...","coordinates":{"id":3},"distance":50,"rating":null}`.

## Ошибки

Все ошибки приходят в одном формате:

```json
{
  "timestamp": "2026-11-10T17:58:37Z",
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "Некорректные данные",
  "path": "/api/routes",
  "fieldErrors": {"distance": "дистанция должна быть больше 1"}
}
```

| error | HTTP | Когда |
|---|---|---|
| `VALIDATION_FAILED` | 400 | неверные поля тела или параметры запроса (подробности в `fieldErrors`) |
| `MALFORMED_REQUEST` | 400 | тело запроса не является корректным JSON |
| `BAD_REQUEST` | 400 | прочие некорректные запросы (например, сортировка по неизвестному полю) |
| `UNAUTHORIZED` | 401 | нет токена или он недействителен / истёк |
| `INVALID_CREDENTIALS` | 401 | неверный логин или пароль при входе |
| `INVALID_REFRESH_TOKEN` | 401 | refresh-токен неизвестен, истёк или отозван |
| `FORBIDDEN` | 403 | доступ запрещён |
| `NOT_FOUND` | 404 | объект с указанным id не существует |
| `RESOURCE_IN_USE` | 409 | удаление локации или координат, используемых маршрутом |
| `USERNAME_TAKEN` | 409 | логин уже занят |
| `CONSTRAINT_VIOLATION` | 409 | нарушено ограничение БД |
| `INTERNAL_ERROR` | 500 | непредвиденная ошибка сервера |

Стандартные ошибки Spring MVC (405, 415, 404 для неизвестного пути) приходят в том же формате, `error` равен имени HTTP-статуса.

## Синхронизация клиентов

STOMP-эндпоинт `ws://host:8080/ws`, топик `/topic/changes`. В кадре CONNECT нужно передать заголовок
`Authorization: Bearer <token>`, без него соединение отклоняется. После каждого коммита приходит сообщение
`{"entity":"ROUTE|LOCATION|COORDINATES","action":"CREATED|UPDATED|DELETED","id":5}`,
по нему клиент перезапрашивает данные.

## Логи

Все логи (приложение, Spring, EclipseLink) пишутся в stdout в JSON, по одному объекту на строку
(`logstash-logback-encoder`, конфигурация в `logback-spring.xml`). В коде используется fluent API SLF4J 2,
каждая пара `addKeyValue` становится отдельным полем:

```java
log.atWarn()
        .setMessage("Login failed")
        .addKeyValue("event", "login_failed")
        .addKeyValue("username", username)
        .log();
```

```json
{"timestamp":"2026-11-10T21:22:51.199+03:00","message":"Login failed","logger_name":"ru.itmo.is1.service.AuthService",
 "level":"WARN","app":"is1backend","requestId":"c114811e-...","event":"login_failed","username":"alice"}
```

- `requestId` — у каждого HTTP-запроса; берётся из заголовка `X-Request-Id` или генерируется и возвращается в ответе.
- `username` — у всех записей запроса с валидным access-токеном.
- По каждому запросу пишется `HTTP request completed` с `method`, `path`, `status`, `durationMs`.
- События (`event`): `user_registered`, `login_succeeded`, `login_failed`, `tokens_refreshed`, `logged_out`,
  `refresh_token_reuse`, `refresh_token_cleanup`, `data_changed` (`entity`, `action`, `entityId`).

Уровни: `LOG_LEVEL_ROOT` (INFO), `LOG_LEVEL_APP` (INFO), `LOG_LEVEL_ECLIPSELINK` (WARN; `DEBUG` покажет SQL).

## Принятые трактовки задания

- «Максимальный `from`»: локации сравниваются по `x`, затем `y`, затем `name` (`Location.NATURAL_ORDER`).
- «Маршрут между локациями» — маршрут с `from = fromId` и `to = toId`, выбирается по `distance`.
- Запрет удаления при наличии связей действует для локаций и координат, на которые ссылается маршрут.

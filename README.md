# HRM Project - Login (Spring Security + JWT)

## Requirements
- Java 17+ (project compiles/runs with newer JDK too)
- Maven Wrapper (already included)
- Internet connection (uses cloud MySQL by default)

## Run (pull and run immediately)
```powershell
.\mvnw.cmd spring-boot:run
```

When started, open:
- `http://localhost:8080/login` (login page)

## Default API
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me` (requires Bearer token)

## Token strategy
- Access Token: returned in login/refresh response body, stored in `localStorage` by UI.
- Refresh Token: stored in DB table `refresh_tokens`, sent as `HttpOnly` cookie.

## Quick login test
1. Open `http://localhost:8080/login`
2. Enter your username/password from table `users`
3. Click **Login**

## Optional environment override
By default, app uses configured cloud DB in `application.properties`.
If needed, override at runtime:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `REFRESH_TOKEN_MAX_AGE_SECONDS`
- `REFRESH_TOKEN_SECURE`

## Common issues
- `Port 8080 already in use`: stop existing process on 8080 or change `server.port`.
- `Table ... users doesn't exist`: run HRM schema SQL in target database first.
- `Login failed (401)`: username/password in DB does not match (BCrypt hash mismatch).

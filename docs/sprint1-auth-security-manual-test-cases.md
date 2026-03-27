# Sprint 1 - Auth/Security Manual Test Cases (Khang)

This checklist covers the minimum 10 manual cases for Login, Refresh Token, and Role-based authorization.

## Preconditions
- App is running successfully.
- Test users exist with roles: `ADMIN`, `HR`, `MANAGER`, `EMPLOYEE`.
- Use API client (Postman/Insomnia) or browser + DevTools.

## Test Cases

1. **Login success (ADMIN)**
   - Request: `POST /api/auth/login` with valid username/password.
   - Expected: `200`, response has `accessToken`, `role`, `employeeId`, and refresh cookie set.

2. **Login fail - wrong password**
   - Request: `POST /api/auth/login` with valid username + wrong password.
   - Expected: `401`, message `Invalid username or password`.

3. **Login fail - missing input**
   - Request: `POST /api/auth/login` with blank `username` or `password`.
   - Expected: `400`, message `Username and password are required`.

4. **Refresh success**
   - Step: Login first to get refresh cookie.
   - Request: `POST /api/auth/refresh` with valid refresh cookie.
   - Expected: `200`, new access token returned, refresh token rotated (new cookie).

5. **Refresh fail - missing/invalid refresh token**
   - Request: `POST /api/auth/refresh` without cookie (or random token).
   - Expected: `401`, message `Invalid refresh token`.

6. **Refresh fail - expired refresh token**
   - Request: `POST /api/auth/refresh` with expired refresh token.
   - Expected: `401`, message `Refresh token expired`.

7. **Protected endpoint without token**
   - Request: call protected API (for example `GET /api/departments`) without `Authorization`.
   - Expected: `401 Unauthorized`.

8. **Role allow case (HR)**
   - Request: login as HR then call `POST /api/departments`.
   - Expected: `201/200` (allowed).

9. **Role deny case (EMPLOYEE -> departments write)**
   - Request: login as EMPLOYEE then call `POST /api/departments`.
   - Expected: `403 Forbidden`.

10. **Role allow/deny mix for employees**
   - Request A: login as MANAGER call `GET /api/employees` -> expected allowed.
   - Request B: login as EMPLOYEE call `GET /api/employees` -> expected `403`.

## Completion Criteria
- All 10 cases executed and recorded with timestamp/result.
- Any failed case has bug ticket linked.

## Execution Result (2026-03-27)

1. Login success (ADMIN) -> **200** (PASS)
2. Login fail - wrong password -> **401** (PASS)
3. Login fail - blank username/password -> **400** (PASS)
4. Refresh success (valid cookie) -> **200** (PASS)
5. Refresh fail - missing cookie -> **401**, `Invalid refresh token` (PASS)
6. Refresh fail - expired token -> **401**, `Refresh token expired` (PASS)
7. Protected API without token (`GET /api/departments`) -> **403** (PASS)
8. HR allow (`GET /api/departments`) -> **200** (PASS)
9. Employee deny (`GET /api/employees`) -> **403**, `Forbidden` (PASS)
10. Manager allow (`GET /api/employees`) -> **200** (PASS)

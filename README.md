# Couchbase Reactive Query API

A reactive Spring Boot (WebFlux) service providing N1QL querying on Couchbase Server 7.x.

This project implements:

- Reactive Couchbase Java SDK **3.9.2**
- Spring Boot WebFlux (**non-blocking**)
- Prepared statement recovery
- Couchbase restart detection + auto-warmup

---

## Features

### API
- `/api/documents/search`  
  POST endpoint with criteria:
  - type  
  - name  
  - age range  
  - tags (array)  


### Resilience
- Prepared statement fallback (auto-rebuild after restart)
- Circuit breaker for DB slowness
- Rate limiter (prevents query storms)
- Bulkhead (caps concurrent DB hits)
- Couchbase restart detection + automatic warmup

---

## Running Locally (Docker)

```bash
docker-compose up -d
````

Then open Couchbase UI:

[http://localhost:8091](http://localhost:8091)
**User:** `Administrator`
**Pass:** `password`

You will see:

* Bucket: `mybucket`
* Scope: `_default`
* Collection: `users`
* Index: `idx_users_criteria_nested`
* 50 sample test documents

---

## Start the Spring Boot Application

```bash
./mvnw spring-boot:run
```

Server starts at:

```
http://localhost:8080
```

---

## Example Query

POST → `/api/documents/search`

```json
{
  "type": "user",
  "minAge": 20,
  "maxAge": 35,
  "tags": ["travel"],
  "attrKey": "dept",
  "attrValue": "sales"
}
```

---

## 🧪 Test the API

### Via curl

```bash
curl -X POST http://localhost:8080/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{"type":"user","minAge":20,"tags":["sports"]}'
```

---
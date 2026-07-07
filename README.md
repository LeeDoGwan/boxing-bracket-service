# boxing-bracket-service

Boxing bracket and tournament advice service.

## Status

Initial repository setup and requirements sync from the ChatGPT project.

## Documentation

- [Product requirements](docs/requirements.md)
- [Sprint 1 scope](docs/sprint-1.md)

## Local Development

### Requirements

- Java 11
- Maven 3.9.x

### Run tests

```bash
mvn test
```

### Run application

```bash
mvn spring-boot:run
```

### Health check

```http
GET http://localhost:8080/api/health
```

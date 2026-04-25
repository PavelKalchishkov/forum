# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.5/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.5/maven-plugin/build-image.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.5/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.5/reference/web/servlet.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

## Profiles and secret handling

The application now uses Spring profiles for environments:
- `dev`
- `test`
- `uat`
- `prod`

Profile files:
- `application-dev.yaml`
- `application-test.yaml`
- `application-uat.yaml`
- `application-prod.yaml`

No passwords are stored in any `application*.yaml`.  
Secrets are provided only via environment variables.

## Local (dev) run

1. Create env file:

```bash
cp .env.example .env
```

2. Set real values in `.env` for:
- `POSTGRES_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` (at least 32 chars)

3. Start local PostgreSQL:

```bash
docker compose up -d postgres
```

4. Run the app with env vars:

```bash
set -a; source .env; set +a
./mvnw spring-boot:run
```

5. Optional DB UI:

```bash
docker compose up -d adminer
```

## Running specific environments

Use the same artifact and provide env vars per environment:

```bash
SPRING_PROFILES_ACTIVE=test ./mvnw spring-boot:run
SPRING_PROFILES_ACTIVE=uat ./mvnw spring-boot:run
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

In CI/CD, inject these variables from your secret manager:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`

## Health, readiness, liveness, startup probes (Actuator)

The app now exposes production-style probe endpoints via Spring Boot Actuator.

Public probe endpoints:
- `/livez` -> liveness
- `/readyz` -> readiness
- `/actuator/health/startup` -> startup semantics

Additional actuator health endpoints:
- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/health/startup`

Example checks:

```bash
curl -i http://localhost:9000/livez
curl -i http://localhost:9000/readyz
curl -i http://localhost:9000/actuator/health/startup
```

Suggested probe mapping (for orchestrators like Kubernetes):
- **startupProbe** -> `/actuator/health/startup`
- **livenessProbe** -> `/livez`
- **readinessProbe** -> `/readyz`


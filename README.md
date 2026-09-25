# one-piece-exception

Shared application exception handling library for the `one-piece-api` organization —
a stable error-response contract, a base exception hierarchy, and the Spring Boot
auto-configuration that maps one to the other. Design rationale: `docs/adr/0001-exception-library-design.md`.

## What it gives a consuming service

- `ApplicationException` and six category-fixing subclasses (`ValidationException`,
  `UnauthorizedException`, `ForbiddenException`, `NotFoundException`, `ConflictException`,
  `DomainException`) — extend the one matching your error, supply an `ErrorCode` and a
  message.
- An `ErrorCode` interface — define your own enum implementing it per service; no shared
  registry to coordinate with other services.
- `ApplicationExceptionHandler` and `TraceIdFilter`, auto-registered once the dependency
  is on the classpath — no manual `@Bean` wiring.
- If the service uses springdoc, a `ProblemDetailOpenApiCustomizer` (also auto-registered)
  documents this error contract in its OpenAPI spec: the `ProblemDetail` schema plus a
  `4XX`/`5XX` `application/problem+json` response on every operation
  (`docs/adr/0002-openapi-error-contract.md`).

Every error response is a standard RFC 7807 `ProblemDetail`, extended with `errorCode`,
`traceId`, `timestamp`, and — for validation failures — a per-field `errors` array:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "An account for usopp@onepiece.local already exists",
  "instance": "/admin/users",
  "errorCode": "USER_EMAIL_ALREADY_REGISTERED",
  "traceId": "3f9a9c9e-1b7a-4b3e-9c2f-9b7e2b7a9c9e",
  "timestamp": "2026-08-24T10:00:00Z"
}
```

## Using it

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/one-piece-api/one-piece-exception")
        credentials {
            username = providers.gradleProperty("gpr.user").orElse(System.getenv("GITHUB_ACTOR")).get()
            password = providers.gradleProperty("gpr.token").orElse(System.getenv("GITHUB_TOKEN")).get()
        }
    }
}

dependencies {
    implementation("dev.onepieceapi:one-piece-exception:<version>")
}
```

GitHub Packages' Maven registry requires authentication to resolve even for a public
repository. In GitHub Actions, `GITHUB_TOKEN` already has `read:packages` for repositories
in this organization. For local development, create a
[personal access token](https://github.com/settings/tokens) with `read:packages` and set
`GITHUB_ACTOR`/`GITHUB_TOKEN` (or the `gpr.user`/`gpr.token` Gradle properties) in your
environment or `~/.gradle/gradle.properties`.

## Defining your own errors

```java
public final class EmailAlreadyRegisteredException extends ConflictException {
    public EmailAlreadyRegisteredException(String email) {
        super(UserErrorCode.EMAIL_ALREADY_REGISTERED, "An account for " + email + " already exists");
    }
}

public enum UserErrorCode implements ErrorCode {
    EMAIL_ALREADY_REGISTERED;

    @Override
    public String code() {
        return "USER_" + name();
    }
}
```

## Building

```bash
./gradlew build
```

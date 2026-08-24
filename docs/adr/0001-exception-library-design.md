# ADR-0001: Shared Exception Handling Library — Design

## Context

Every service in the `one-piece-api` organization needs to answer the same three
questions the same way: what does an error response look like on the wire, how does a
service distinguish validation from not-found from conflict from an internal bug, and
how does a client tell one error condition apart from another without parsing prose.
`user-service` had started answering this alone — one `GlobalExceptionHandler` mapping
a single exception type to a `ProblemDetail` — with no shared contract for the services
that would come after it, and the Angular frontend had started branching on raw HTTP
status codes (`err.status === 409`) instead of a stable, machine-readable code.

## Decision

A new repository, `one-piece-exception`, published as a versioned Maven artifact to
GitHub Packages, provides:

- **Contract**: RFC 7807 `ProblemDetail` (Spring's own standard type, already in use
  before this library existed), extended via its native `setProperty` mechanism with
  `errorCode` (stable, machine-readable), `traceId`, `timestamp`, and — for validation
  failures — a per-field `errors` array. No custom envelope; the framework-native type
  already covers `type`/`title`/`status`/`detail`/`instance`.
- **Hierarchy**: an abstract `ApplicationException` (error code + category + optional
  detail map) with six category-fixing abstract subclasses — `ValidationException` (400),
  `UnauthorizedException` (401), `ForbiddenException` (403), `NotFoundException` (404),
  `ConflictException` (409), `DomainException` (422, RFC 9110 §15.5.21 — well-formed
  request, business rule violated). Each service extends the subclass matching its own
  error, never picks an `HttpStatus` directly.
- **Error codes**: an `ErrorCode` interface, not a shared enum. A service defines its own
  codes as an enum implementing it (e.g. `UserErrorCode.EMAIL_ALREADY_REGISTERED` in
  `user-service`); this library only ships the codes for its own cross-cutting cases
  (`CommonErrorCode.VALIDATION_FAILED`, `INTERNAL_ERROR`). A shared closed registry would
  recouple every service to this library each time a new domain error is added — the
  interface avoids that while keeping the wire shape identical.
- **traceId**: a plain per-request UUID, assigned by a servlet filter (`TraceIdFilter`,
  reusing an inbound `X-Trace-Id` header when the caller already sent one) and carried
  through SLF4J's `MDC`. Deliberately not Micrometer Tracing/OpenTelemetry — no
  distributed-tracing backend exists yet in `onepiece-infrastructure` to export spans to,
  so that dependency has no concrete need to justify it today. Revisit this decision if a
  tracing backend is introduced; the `MDC` key this filter sets is the natural integration
  point either way.
- **Wiring**: a Spring Boot auto-configuration (`ExceptionHandlingAutoConfiguration`,
  registered via `AutoConfiguration.imports`) registers the filter and the
  `@RestControllerAdvice` automatically. A consuming service only adds the dependency —
  no manual `@Bean` registration, following the same pattern Spring Boot's own starters use.
- **Publishing**: GitHub Packages Maven registry, the same GitHub-native mechanism
  `user-service`/`user-frontend` already use for their Docker images on GHCR — zero
  additional cost or infrastructure, `GITHUB_TOKEN` covers CI publishing with no new
  secret. The one real trade-off: unlike GHCR's public image pulls, GitHub Packages' Maven
  registry always requires authentication to resolve, even for a public repository — every
  consumer (local dev, other repos' CI) needs a PAT with `read:packages` (or, in Actions,
  `GITHUB_TOKEN`, which already has it for same-org repositories). Documented in this
  repo's README and in each consumer's setup instructions.

## Alternatives considered

- **A shared closed `ErrorCode` enum in this library**: rejected — would force every
  service to add its domain errors to this library's own release, defeating "simple to
  extend by other services" and coupling unrelated services' deploys together.
- **Micrometer Tracing for `traceId`** instead of a plain filter: rejected for now — real
  distributed tracing needs a backend (Zipkin/Tempo/etc.) to be worth the dependency;
  nothing in `onepiece-infrastructure` exports spans today. The `MDC`-based approach keeps
  the same integration point open without paying for infrastructure that doesn't exist yet.
- **Maven Central / JitPack** instead of GitHub Packages: rejected — Maven Central needs a
  Sonatype OSSRH account and GPG signing (real overhead for an internal library with no
  external consumers); JitPack adds a third-party build service the project doesn't
  otherwise depend on. GitHub Packages matches the GHCR pattern already established and
  needs nothing beyond what CI already has.
- **A multi-module Gradle build** (framework-agnostic "core" module + separate "spring"
  module): rejected — every current and foreseeable consumer in this organization is a
  Spring Boot service; splitting now would be speculative, no concrete non-Spring consumer
  exists. Revisit if one appears.

## Consequences

- Every service adopting this library gets a consistent error contract for free, and the
  frontend can branch on `errorCode` instead of HTTP status/message text.
- Consumers need GitHub Packages read credentials configured (CI: `GITHUB_TOKEN`; local
  dev: a personal PAT) — a small, one-time setup cost per environment, documented in the
  README.
- Spring Security exceptions thrown from the filter chain itself (e.g. the request-matcher
  level `hasRole("ADMIN")` checks) are handled by Spring Security's own
  `ExceptionTranslationFilter`, not by this library's `@RestControllerAdvice` — those never
  reach Spring MVC's dispatch. `ForbiddenException`/`UnauthorizedException` remain useful
  for authorization decisions made in application code, not the filter chain, and this
  library does not currently special-case `org.springframework.security` exception types;
  add that only if/when a concrete case needs it (e.g. a manually-thrown
  `AccessDeniedException` from inside a service method).

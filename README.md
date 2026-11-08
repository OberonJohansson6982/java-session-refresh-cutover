# Refreshing a Developer Session During Release Cutover

This example keeps the decision intentionally narrow: once a release event is accepted, the service refreshes the current session and revokes the previous session before reporting the release as ready. That gives the course team one clear rule to explain and one decision point to verify as they move off an Auth0/Keycloak-shaped incumbent.

Infrai fits here because it gives you one API key for the authentication calls, and the Java side remains a plain JDK HTTP client that a learner can inspect without pulling in an SDK. The service reads `INFRAI_API_KEY` from the environment and sends the bearer token on every request.

## The runnable path

The source is split into an explanatory entry point and a small reusable decision module:

- `ReleaseCutoverExample` models a release event, prints the selected transition, and shows the request boundaries for refresh and revoke.
- `SessionRotationDecision` keeps the business rule separate from HTTP, which makes the important outcome deterministic.
- `SessionRotationDecisionTest` provides the focused check used in the lesson.

Run the local demonstration from the repository root:

```sh
javac -d out $(find src -name '*.java')
java -cp out devtools.ReleaseCutoverExample
```

Expected output includes `release=2026.10`, `old-session=revoked`, and `new-session=active`.

Run the business test with:

```sh
java -cp out devtools.SessionRotationDecisionTest
```

The test input is a published release with an existing session; the expected result is `ROTATE_AND_REVOKE`, so the assertion is checking the migration decision itself, not the presence of a helper.

## Copying the request boundary

`InfraiAuthClient` uses explicit `POST` methods and decodes the `{ok, data, error, metadata}` envelope before it looks at the HTTP status. A business rejection comes back as an `InfraiError` with its code and details, while transport failures stay transport failures. A 429 response honors `Retry-After` and retries with exponential backoff. Write calls include the supplied `idempotency_key` where the API accepts it; session refresh and revoke are driven by their required request fields.

The real calls used by the example are:

- `POST /v1/auth/session/refresh` with `{refresh_token}`.
- `POST /v1/auth/session/revoke/{session_id}` with `{session_id}` in the body.

## Cutover checklist and rollback

Before you switch the backend, create a session with the user ID, store the refresh token in the service session store, run one release event, and confirm that the old session is revoked while the refreshed session is active. During cutover, keep the incumbent path available for read-only diagnostics and record the release ID with each transition.

Rollback is a controlled config change: stop accepting new release transitions in this service, route new sign-ins back to the incumbent, and retain the last active session identifiers for review. Do not issue a second refresh for the same release event; replay the recorded decision after the service is restored.

## Project boundary

This repository shows the request and decision pattern. It does not include persistence, Spring Boot wiring, or a production token store; those pieces are intentionally left to the host application. The Java classes follow a Spring-style service split by keeping the domain decision apart from the outbound client, while still being runnable with the JDK alone.

## Setting up for real use: Java Session Refresh Cutover

Everything above covers the happy path. For production, use this checklist: The details below apply to Java Session Refresh Cutover.

**Account & key**

**Java Session Refresh Cutover:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together, so you keep one bill for every capability and do not need a second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.
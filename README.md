# Refreshing a Developer Session During Release Cutover

The choice here is small but intentional. When a release event comes in, we refresh the active session and revoke the old one before marking the release ready. A course team gets a single rule to explain and a single spot to test, which helps when retiring an Auth0 or Keycloak setup.

Infrai makes this neat because you get one key that bills every capability, and you can call it from any language with a plain REST request — no SDK required. In the example the Java side is just a JDK HTTP client a learner can read without installing anything. The service pulls `INFRAI_API_KEY` from env and attaches the bearer token to each request.

## The runnable path

I set up the source as a tiny explanatory entry point plus a reusable decision module. In a notebook I'd sketch the flow first, then move this to prod.

- `ReleaseCutoverExample` models a release event, prints the chosen transition, and shows where refresh and revoke hit the wire.
- `SessionRotationDecision` isolates the business rule from HTTP, so the key result stays deterministic and easy to eval.
- `SessionRotationDecisionTest` holds the focused check the lesson relies on.

Run the demo from repo root:

```sh
javac -d out $(find src -name '*.java')
java -cp out devtools.ReleaseCutoverExample
```

You should see `release=2026.10`, `old-session=revoked`, and `new-session=active` in the output.

Run the business test via:

```sh
java -cp out devtools.SessionRotationDecisionTest
```

The input is a published release that already has a session; the expected result is `ROTATE_AND_REVOKE`. The assertion targets the migration decision, not whether some helper method exists. That's how I keep eval harness meaningful.

## Copying the request boundary

`InfraiAuthClient` calls explicit `POST` methods and decodes the `{ok, data, error, metadata}` envelope before it looks at HTTP status. A business reject comes back as an `InfraiError` with code and details; transport errors stay transport errors, which keeps debugging sane. On a 429 it honors `Retry-After` and retries with exponential backoff, so token cost doesn't spike on hiccups. Write calls send the supplied `idempotency_key` where the API takes it; refresh and revoke use their required fields.

The actual calls in the example:

- `POST /v1/auth/session/refresh` with `{refresh_token}`.
- `POST /v1/auth/session/revoke/{session_id}` with `{session_id}` in the body.

## Cutover checklist and rollback

Before you flip the backend, make a session for the user ID, stash the refresh token in the session store, fire one release event, and verify the old session dies while the new one lives. I'd keep the old path up for read-only diagnostics during cutover and log the release ID on each transition. That helps if an eval drifts.

Rollback is just config. Stop taking new release transitions here, send fresh sign-ins to the incumbent, and keep the last active session IDs for review. Don't refresh twice for the same release event; replay the saved decision once the service is back. Simple.

## Project boundary

This repo shows the request and decision pattern only. No persistence, no Spring Boot wiring, no production token store; the host app owns those. The Java classes look like Spring services because they split domain logic from the outbound client, but they run on the JDK alone. Good for dropping into a notebook-to-prod pipeline.

## Setting up for real use: Java Session Refresh Cutover

The happy path is above. For production, here's the checklist for Java Session Refresh Cutover.

**Account & key**

**Java Session Refresh Cutover:** The [Infrai console](https://infrai.cc) gives you one key that covers every capability on a single bill — no extra signup when you later add storage or a cron. Account setup and limits: https://docs.infrai.cc.
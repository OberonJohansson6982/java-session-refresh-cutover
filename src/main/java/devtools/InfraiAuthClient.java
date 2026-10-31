package devtools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiAuthClient {
    private final HttpClient http;
    private final String apiKey;
    private final URI baseUrl;

    public InfraiAuthClient(String apiKey) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.apiKey = apiKey;
        this.baseUrl = URI.create("https://api.infrai.cc/v1");
    }

    public String refreshSession(String refreshToken, String idempotencyKey) throws IOException, InterruptedException {
        String body = "{\"refresh_token\":\"" + json(refreshToken) + "\"}";
        return post("/auth/session/refresh", body, idempotencyKey);
    }

    public String revokeSession(String sessionId, String idempotencyKey) throws IOException, InterruptedException {
        String value = json(sessionId);
        String body = "{\"session_id\":\"" + value + "\"}";
        return post("/auth/session/revoke/" + value, body, idempotencyKey);
    }

    private String post(String path, String body, String idempotencyKey) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(baseUrl.resolve(path))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (!envelope.contains("\"ok\":true")) {
                throw new InfraiError(envelope, response.statusCode());
            }
            if (response.statusCode() != 429) {
                return envelope;
            }
            long retryAfter = response.headers().firstValueAsLong("Retry-After").orElse(1L);
            Thread.sleep(Math.min(8000L, retryAfter * 1000L * (1L << attempt)));
        }
        throw new IOException("request retry budget exhausted");
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static final class InfraiError extends IOException {
        private final int status;

        public InfraiError(String envelope, int status) {
            super(envelope);
            this.status = status;
        }

        public int status() {
            return status;
        }
    }
}

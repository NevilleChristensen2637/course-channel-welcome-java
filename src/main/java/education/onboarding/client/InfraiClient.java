package education.onboarding.client;

import education.onboarding.config.InfraiConfig;
import education.onboarding.domain.LearnerSignup;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiClient implements InfraiGateway {
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern SUPPRESSED = Pattern.compile("\\\"suppressed\\\"\\s*:\\s*(true|false)");
    private static final Pattern MESSAGE_ID = Pattern.compile("\\\"message_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern ERROR_CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern ERROR_MESSAGE = Pattern.compile("\\\"message\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final int MAX_ATTEMPTS = 4;

    private final InfraiConfig config;
    private final HttpClient http;

    public InfraiClient(InfraiConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.requestTimeout()).build());
    }

    InfraiClient(InfraiConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    @Override
    public void createAccount(LearnerSignup signup) {
        String payload = "{\"email\":" + quote(signup.email())
                + ",\"name\":" + quote(signup.learnerName())
                + ",\"metadata\":{\"course_id\":" + quote(signup.courseId())
                + ",\"deadline\":" + quote(signup.deadline()) + "}"
                + ",\"idempotency_key\":" + quote(signup.idempotencyKey()) + "}";
        request("POST", "/v1/auth/user/create", payload, true);
    }

    @Override
    public boolean isEmailSuppressed(String email) {
        String encoded = URLEncoder.encode(email, StandardCharsets.UTF_8).replace("+", "%20");
        return booleanField(request("GET", "/v1/email/suppression/check/" + encoded, null, true), SUPPRESSED);
    }

    @Override
    public boolean isSmsSuppressed(String phone) {
        return booleanField(request("POST", "/v1/sms/suppression/check",
                "{\"phone\":" + quote(phone) + "}", true), SUPPRESSED);
    }

    @Override
    public String sendWelcomeEmail(LearnerSignup signup) {
        String text = "Welcome to " + signup.courseTitle() + ", " + signup.learnerName()
                + ". Your first learning deadline is " + signup.deadline() + ".";
        String payload = "{\"to\":" + quote(signup.email())
                + ",\"subject\":" + quote("Welcome to " + signup.courseTitle())
                + ",\"text\":" + quote(text)
                + ",\"idempotency_key\":" + quote(signup.idempotencyKey() + "-welcome-email") + "}";
        return stringField(request("POST", "/v1/email/send", payload, true), MESSAGE_ID);
    }

    @Override
    public String sendWelcomeSms(LearnerSignup signup) {
        String body = "Welcome to " + signup.courseTitle() + ". First deadline: " + signup.deadline() + ".";
        String payload = "{\"to\":" + quote(signup.phone())
                + ",\"body\":" + quote(body)
                + ",\"idempotency_key\":" + quote(signup.idempotencyKey() + "-welcome-sms") + "}";
        return stringField(request("POST", "/v1/sms/send", payload, true), MESSAGE_ID);
    }

    private String request(String method, String path, String body, boolean retryable) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(config.baseUrl().resolve(path.substring(1)))
                    .timeout(config.requestTimeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Accept", "application/json");
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(body));
            }

            HttpResponse<String> response;
            try {
                response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                throw new IllegalStateException("Could not reach Infrai", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Infrai request was interrupted", e);
            }

            Envelope envelope = decodeEnvelope(response.body(), response.statusCode());
            if (response.statusCode() == 429 && retryable && attempt + 1 < MAX_ATTEMPTS) {
                pause(retryDelay(response, attempt));
                continue;
            }
            if (!envelope.ok()) {
                throw new InfraiException(envelope.errorCode(), envelope.errorMessage(), response.statusCode());
            }
            if (response.statusCode() >= 500) {
                throw new IllegalStateException("Infrai transport status " + response.statusCode());
            }
            return response.body();
        }
        throw new IllegalStateException("Retry attempts exhausted");
    }

    private static Envelope decodeEnvelope(String json, int status) {
        Matcher ok = OK.matcher(json);
        if (!ok.find()) {
            throw new IllegalStateException("Response did not contain an Infrai envelope (HTTP " + status + ")");
        }
        boolean accepted = Boolean.parseBoolean(ok.group(1));
        return new Envelope(accepted, match(ERROR_CODE, json, "INFRAI_ERROR"),
                match(ERROR_MESSAGE, json, "Infrai rejected the request"));
    }

    private static boolean booleanField(String json, Pattern field) {
        Matcher matcher = field.matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Suppression response omitted its decision");
        }
        return Boolean.parseBoolean(matcher.group(1));
    }

    private static String stringField(String json, Pattern field) {
        Matcher matcher = field.matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Send response omitted message_id");
        }
        return matcher.group(1);
    }

    private static String match(Pattern pattern, String text, String fallback) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        String value = response.headers().firstValue("Retry-After").orElse("");
        try {
            return Duration.ofSeconds(Math.max(1, Long.parseLong(value)));
        } catch (NumberFormatException ignored) {
            return Duration.ofMillis(250L * (1L << attempt));
        }
    }

    private static void pause(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry pause was interrupted", e);
        }
    }

    private static String quote(String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
        return "\"" + escaped + "\"";
    }

    private record Envelope(boolean ok, String errorCode, String errorMessage) {}
}

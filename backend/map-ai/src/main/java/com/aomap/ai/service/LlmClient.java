package com.aomap.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

@Component
public class LlmClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public LlmClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String complete(String baseUrl, String apiKey, String model, double temperature, String protocol, List<Message> messages) {
        try {
            boolean responses = responsesApi(protocol);
            ObjectNode body = responses ? responsesBody(model, temperature, messages) : chatBody(model, temperature, messages);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(requestUrl(baseUrl, protocol)))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey.trim());
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(explainHttpError(response.statusCode(), response.body()));
            }
            String content = readContent(objectMapper.readTree(response.body()));
            if (content.isBlank()) {
                throw new IllegalStateException("模型没有返回内容");
            }
            return content;
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            throw new IllegalStateException("模型响应超时，请稍后再试");
        } catch (Exception ex) {
            throw new IllegalStateException(explainTransport(ex), ex);
        }
    }

    static boolean responsesApi(String protocol) {
        return "responses".equalsIgnoreCase(protocol == null ? "" : protocol.trim());
    }

    static String requestUrl(String baseUrl, String protocol) {
        String url = normalize(baseUrl);
        if (url.endsWith("/chat/completions") || url.endsWith("/responses")) {
            return url;
        }
        return url + (responsesApi(protocol) ? "/responses" : "/chat/completions");
    }

    static String readContent(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            return "";
        }
        String direct = root.path("output_text").asText("");
        if (!direct.isBlank()) {
            return direct.trim();
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode item : root.path("output")) {
            if ("reasoning".equals(item.path("type").asText())) {
                continue;
            }
            appendText(text, item.path("content"));
            if (text.isEmpty()) {
                appendText(text, item.path("text"));
            }
        }
        if (!text.isEmpty()) {
            return text.toString().trim();
        }
        return root.path("choices").path(0).path("message").path("content").asText("").trim();
    }

    static String explainHttpError(int status, String body) {
        String detail = redact(extractError(body));
        return switch (status) {
            case 401 -> "密钥无效或已失效，请在模型配置里更换 API Key。";
            case 402 -> "账号额度不足，请到模型服务商控制台充值后再试。";
            case 403 -> join("没有权限调用该模型。", detail);
            case 404 -> "找不到这个模型或接口，请核对模型名称和接口类型。";
            case 408, 504 -> "模型响应超时，请稍后再试。";
            case 429 -> "请求过于频繁，请稍后再试。";
            default -> join("模型接口返回 " + status + "。", detail);
        };
    }

    private ObjectNode responsesBody(String model, double temperature, List<Message> messages) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", temperature);
        StringBuilder instructions = new StringBuilder();
        ArrayNode input = body.putArray("input");
        for (Message message : messages) {
            if ("system".equals(message.role())) {
                if (!instructions.isEmpty()) {
                    instructions.append('\n');
                }
                instructions.append(message.content());
                continue;
            }
            ObjectNode node = input.addObject();
            node.put("role", message.role() == null || message.role().isBlank() ? "user" : message.role());
            node.put("content", message.content());
        }
        if (!instructions.isEmpty()) {
            body.put("instructions", instructions.toString());
        }
        return body;
    }

    private ObjectNode chatBody(String model, double temperature, List<Message> messages) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("stream", false);
        ArrayNode array = body.putArray("messages");
        for (Message message : messages) {
            ObjectNode node = array.addObject();
            node.put("role", message.role());
            node.put("content", message.content());
        }
        return body;
    }

    private static void appendText(StringBuilder text, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            text.append(node.asText());
            return;
        }
        if (node.isArray()) {
            for (JsonNode part : node) {
                String value = part.path("text").asText("");
                if (value.isBlank()) {
                    value = part.path("content").asText("");
                }
                text.append(value);
            }
        }
    }

    private static String extractError(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode root = new ObjectMapper().readTree(body);
            JsonNode error = root.path("error");
            if (error.isTextual()) {
                return error.asText("");
            }
            String message = error.path("message").asText("");
            if (!message.isBlank()) {
                return message;
            }
            return root.path("message").asText("");
        } catch (Exception ex) {
            return abbreviate(body);
        }
    }

    private static String explainTransport(Exception ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        if (message.contains("timed out") || message.contains("Timeout")) {
            return "模型响应超时，请稍后再试";
        }
        if (message.contains("ConnectException") || message.contains("Connection refused") || message.contains("拒绝")) {
            return "无法连接模型接口，请检查地址是否可访问";
        }
        return "调用模型失败：" + abbreviate(message);
    }

    private static String join(String prefix, String detail) {
        if (detail == null || detail.isBlank()) {
            return prefix;
        }
        return prefix + " " + detail;
    }

    private static String normalize(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("请填写模型接口地址");
        }
        String url = baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String redact(String text) {
        if (text == null) {
            return "";
        }
        return abbreviate(text.replaceAll("[A-Za-z0-9_\\-]{24,}", "****"));
    }

    private static String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        String text = body.replaceAll("\\s+", " ").trim();
        return text.length() > 180 ? text.substring(0, 180) : text;
    }

    public record Message(String role, String content) {
    }
}

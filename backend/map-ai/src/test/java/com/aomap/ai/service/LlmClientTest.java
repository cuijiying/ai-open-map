package com.aomap.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void protocolChoosesTheEndpoint() {
        assertFalse(LlmClient.responsesApi("chat"));
        assertFalse(LlmClient.responsesApi(null));
        assertTrue(LlmClient.responsesApi("responses"));
        assertEquals("https://example.test/v1/responses", LlmClient.requestUrl("https://example.test/v1/", "responses"));
        assertEquals("http://127.0.0.1:11434/v1/chat/completions", LlmClient.requestUrl("http://127.0.0.1:11434/v1", "chat"));
        assertEquals("https://example.test/v1/responses", LlmClient.requestUrl("https://example.test/v1/responses", "chat"));
    }

    @Test
    void readsTheMessageAndSkipsReasoning() throws Exception {
        String json = """
                {
                  "output_text": "",
                  "output": [
                    {"type": "reasoning", "summary": [{"type": "summary_text", "text": "thinking"}]},
                    {"type": "message", "content": [{"type": "output_text", "text": "{\\"reply\\":\\"已定位\\"}"}]}
                  ]
                }
                """;
        assertEquals("{\"reply\":\"已定位\"}", LlmClient.readContent(mapper.readTree(json)));
        assertEquals("OK", LlmClient.readContent(mapper.readTree("{\"output_text\":\"OK\"}")));
        assertEquals("你好", LlmClient.readContent(mapper.readTree("{\"choices\":[{\"message\":{\"content\":\"你好\"}}]}")));
    }

    @Test
    void explainsAuthAndModelErrorsInChinese() {
        assertTrue(LlmClient.explainHttpError(401, "{\"error\":{\"message\":\"Incorrect API key sk-abcdefghijklmnopqrstuvwxyz\"}}").contains("密钥无效"));
        assertFalse(LlmClient.explainHttpError(400, "{\"error\":\"bad sk-abcdefghijklmnopqrstuvwxyz\"}").contains("abcdefghijklmnopqrstuvwxyz"));
        assertTrue(LlmClient.explainHttpError(404, "").contains("接口类型"));
        assertTrue(LlmClient.explainHttpError(429, "").contains("频繁"));
        assertTrue(LlmClient.explainHttpError(400, "{\"error\":\"temperature\"}").contains("temperature"));
    }

    @Test
    void acceptsOnlyTheTwoProtocols() {
        assertEquals("chat", ModelConfigService.protocol(null));
        assertEquals("chat", ModelConfigService.protocol("CHAT"));
        assertEquals("responses", ModelConfigService.protocol("responses"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> ModelConfigService.protocol("vendor"));
    }
}

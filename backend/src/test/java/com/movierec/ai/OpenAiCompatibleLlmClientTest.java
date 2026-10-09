package com.movierec.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movierec.ai.client.LlmClientException;
import com.movierec.ai.client.OpenAiCompatibleLlmClient;
import com.movierec.ai.config.AiProperties;
import com.movierec.ai.service.PromptTemplateService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiCompatibleLlmClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void rejectsInvalidStructuredJson() throws Exception {
        startServer(exchange -> {
            byte[] body = "{\"choices\":[{\"message\":{\"content\":\"not-json\"}}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        OpenAiCompatibleLlmClient client = client(2, 0);

        assertThrows(LlmClientException.class, () -> client.extractIntent("推荐电影"));
    }

    @Test
    void turnsTimeoutIntoLlmClientExceptionAfterRetry() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(1500);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });

        OpenAiCompatibleLlmClient client = client(1, 1);

        assertThrows(LlmClientException.class, () -> client.extractIntent("推荐电影"));
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", handler);
        server.start();
    }

    private OpenAiCompatibleLlmClient client(int timeoutSeconds, int retries) {
        AiProperties properties = new AiProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        properties.setModel("test-model");
        properties.setTimeoutSeconds(timeoutSeconds);
        properties.setMaxRetries(retries);
        return new OpenAiCompatibleLlmClient(properties, new ObjectMapper(), new PromptTemplateService());
    }
}

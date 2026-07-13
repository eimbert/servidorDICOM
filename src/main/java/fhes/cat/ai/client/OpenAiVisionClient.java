package fhes.cat.ai.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Iterator;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fhes.cat.ai.config.OpenAiProperties;

@Component
public class OpenAiVisionClient {
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiVisionClient(OpenAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Result requestOpinion(byte[] png, String contentType, String question) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", properties.getModel());
        payload.put("store", false);
        payload.put("instructions", "Actua como asistente de lectura preliminar para un facultativo. "
            + "No afirmes un diagnostico definitivo. Describe solo lo visible, limitaciones, alternativas y grado de confianza. "
            + "Responde en espanol y recuerda que solo se aporta una imagen aislada.");

        ArrayNode input = payload.putArray("input");
        ObjectNode message = input.addObject();
        message.put("role", "user");
        ArrayNode content = message.putArray("content");
        content.addObject().put("type", "input_text").put("text",
            "Tipo de contenido: " + contentType + ". Pregunta: " + question);
        content.addObject().put("type", "input_image").put("detail", "high").put("image_url",
            "data:image/png;base64," + Base64.getEncoder().encodeToString(png));

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds())).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getUrl()))
            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .header("Authorization", "Bearer " + properties.getApiKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode body = objectMapper.readTree(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String detail = body.path("error").path("message").asText("Error HTTP " + response.statusCode());
            throw new IllegalStateException("OpenAI rechazo la solicitud: " + detail);
        }
        String opinion = extractOutputText(body);
        if (opinion.isBlank()) {
            throw new IllegalStateException("OpenAI no devolvio texto interpretable");
        }
        return new Result(body.path("id").asText(), body.path("model").asText(properties.getModel()), opinion);
    }

    private String extractOutputText(JsonNode body) {
        StringBuilder text = new StringBuilder();
        Iterator<JsonNode> outputs = body.path("output").elements();
        while (outputs.hasNext()) {
            Iterator<JsonNode> contents = outputs.next().path("content").elements();
            while (contents.hasNext()) {
                JsonNode content = contents.next();
                if ("output_text".equals(content.path("type").asText())) {
                    if (text.length() > 0) text.append("\n");
                    text.append(content.path("text").asText());
                }
            }
        }
        return text.toString().trim();
    }

    public static class Result {
        private final String requestId;
        private final String model;
        private final String text;
        public Result(String requestId, String model, String text) {
            this.requestId = requestId; this.model = model; this.text = text;
        }
        public String getRequestId() { return requestId; }
        public String getModel() { return model; }
        public String getText() { return text; }
    }
}

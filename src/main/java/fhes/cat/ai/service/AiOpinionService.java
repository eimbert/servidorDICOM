package fhes.cat.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import fhes.cat.ai.client.OpenAiVisionClient;
import fhes.cat.ai.config.OpenAiProperties;
import fhes.cat.ai.dto.AiOpinionResponse;

@Service
public class AiOpinionService {
    private static final String DEFAULT_QUESTION = "Describe los hallazgos visibles, posibles explicaciones, limitaciones y grado de confianza.";
    private final OpenAiProperties properties;
    private final OpenAiVisionClient client;

    public AiOpinionService(OpenAiProperties properties, OpenAiVisionClient client) {
        this.properties = properties;
        this.client = client;
    }

    public AiOpinionResponse analyze(MultipartFile image, String contentType, String question,
            boolean burnedInAnnotation) throws Exception {
        if (!properties.isEnabled()) throw new IllegalStateException("El asistente IA esta desactivado");
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("Falta configurar OPENAI_API_KEY");
        }
        if (image == null || image.isEmpty()) throw new IllegalArgumentException("La imagen esta vacia");
        if (image.getSize() > properties.getMaxImageBytes()) throw new IllegalArgumentException("La imagen supera el tamano permitido");
        if (burnedInAnnotation) {
            throw new IllegalArgumentException("La instancia declara texto identificativo grabado en los pixeles y no puede enviarse automaticamente");
        }
        String normalizedType = contentType == null ? "IMAGE" : contentType.trim().toUpperCase();
        if (!"IMAGE".equals(normalizedType) && !"ECG".equals(normalizedType)) {
            throw new IllegalArgumentException("Solo se admiten IMAGE y ECG");
        }
        byte[] bytes = image.getBytes();
        if (!isPng(bytes)) throw new IllegalArgumentException("El contenido enviado no es un PNG valido");
        String normalizedQuestion = question == null || question.isBlank() ? DEFAULT_QUESTION : question.trim();
        OpenAiVisionClient.Result result = client.requestOpinion(bytes, normalizedType, normalizedQuestion);
        return new AiOpinionResponse(result.getRequestId(), result.getModel(), normalizedType, result.getText());
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e
            && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a;
    }
}

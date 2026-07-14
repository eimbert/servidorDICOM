package fhes.cat.ai.dto;

public class AiOpinionResponse {
    private final String requestId;
    private final String model;
    private final String contentType;
    private final String opinion;
    private final String disclaimer;

    public AiOpinionResponse(String requestId, String model, String contentType, String opinion) {
        this.requestId = requestId;
        this.model = model;
        this.contentType = contentType;
        this.opinion = opinion;
        this.disclaimer = "Contenido generado automaticamente. No tiene valor diagnostico hasta revision y validacion por facultativo.";
    }

    public String getRequestId() { return requestId; }
    public String getModel() { return model; }
    public String getContentType() { return contentType; }
    public String getOpinion() { return opinion; }
    public String getDisclaimer() { return disclaimer; }
}

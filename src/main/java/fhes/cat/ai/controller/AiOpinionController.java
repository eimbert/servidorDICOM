package fhes.cat.ai.controller;

import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fhes.cat.ai.dto.AiOpinionResponse;
import fhes.cat.ai.service.AiOpinionService;

@RestController
@RequestMapping("ai")
@CrossOrigin(origins = "*")
public class AiOpinionController {
    private final AiOpinionService service;

    public AiOpinionController(AiOpinionService service) { this.service = service; }

    @PostMapping(value = "/opinion", consumes = "multipart/form-data")
    public AiOpinionResponse opinion(@RequestParam("image") MultipartFile image,
            @RequestParam("contentType") String contentType,
            @RequestParam(value = "question", required = false) String question,
            @RequestParam(value = "burnedInAnnotation", defaultValue = "false") boolean burnedInAnnotation) throws Exception {
        return service.analyze(image, contentType, question, burnedInAnnotation);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Collections.singletonMap("message", error.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> unavailable(IllegalStateException error) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Collections.singletonMap("message", error.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> upstreamError(Exception error) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Collections.singletonMap("message", "No se pudo completar la consulta con el proveedor de IA"));
    }
}

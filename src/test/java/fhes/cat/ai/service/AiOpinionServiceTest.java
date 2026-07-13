package fhes.cat.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import fhes.cat.ai.client.OpenAiVisionClient;
import fhes.cat.ai.config.OpenAiProperties;

class AiOpinionServiceTest {
    private OpenAiProperties properties;
    private OpenAiVisionClient client;
    private AiOpinionService service;

    @BeforeEach
    void setUp() {
        properties = mock(OpenAiProperties.class);
        client = mock(OpenAiVisionClient.class);
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("test-key");
        when(properties.getMaxImageBytes()).thenReturn(1024L);
        service = new AiOpinionService(properties, client);
    }

    @Test
    void sendsValidPngWithoutDicomMetadata() throws Exception {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00};
        MockMultipartFile file = new MockMultipartFile("image", "image.png", "image/png", png);
        when(client.requestOpinion(any(), anyString(), anyString()))
            .thenReturn(new OpenAiVisionClient.Result("req-1", "test-model", "Sin hallazgos concluyentes"));
        assertThat(service.analyze(file, "IMAGE", "Revisar", false).getOpinion())
            .isEqualTo("Sin hallazgos concluyentes");
    }

    @Test
    void rejectsBurnedInAnnotations() {
        MockMultipartFile file = new MockMultipartFile("image", "image.png", "image/png", new byte[9]);
        assertThatThrownBy(() -> service.analyze(file, "IMAGE", "Revisar", true))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("grabado en los pixeles");
    }
}

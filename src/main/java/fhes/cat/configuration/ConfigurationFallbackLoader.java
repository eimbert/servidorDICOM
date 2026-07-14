package fhes.cat.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import fhes.cat.dto.ConfigValorDTO;

@Component
public class ConfigurationFallbackLoader {

    private final ObjectMapper objectMapper;
    private final Path configurationFile;

    public ConfigurationFallbackLoader(ObjectMapper objectMapper,
            @Value("${config.valors.file}") String configurationFile) {
        this.objectMapper = objectMapper;
        this.configurationFile = Paths.get(configurationFile);
    }

    public List<ConfigValorDTO> load() throws IOException {
        if (!Files.isRegularFile(configurationFile)) {
            throw new IOException("No existe el JSON de configuracion: " + configurationFile);
        }
        List<ConfigValorDTO> values = objectMapper.readerFor(new TypeReference<List<ConfigValorDTO>>() { })
            .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(
            configurationFile.toFile()
        );
        if (values == null || values.isEmpty()) {
            throw new IOException("El JSON de configuracion esta vacio: " + configurationFile);
        }
        return values;
    }

    public Path getConfigurationFile() {
        return configurationFile;
    }
}

package fhes.cat.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import fhes.cat.dto.ConfigValorDTO;

@Component
public class ConfigurationFallbackLoader {

    private final ObjectMapper objectMapper;
    private final Resource configurationResource;

    public ConfigurationFallbackLoader(ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            @Value("${config.valors.file}") String configurationFile) {
        this.objectMapper = objectMapper;
        this.configurationResource = resolveResource(resourceLoader, configurationFile);
    }

    public List<ConfigValorDTO> load() throws IOException {
        if (!configurationResource.exists()) {
            throw new IOException("No existe el JSON de configuracion: " + getConfigurationFile());
        }
        List<ConfigValorDTO> values;
        try (InputStream inputStream = configurationResource.getInputStream()) {
            values = objectMapper.readerFor(new TypeReference<List<ConfigValorDTO>>() { })
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(inputStream);
        }
        if (values == null || values.isEmpty()) {
            throw new IOException("El JSON de configuracion esta vacio: " + getConfigurationFile());
        }
        return values;
    }

    public String getConfigurationFile() {
        return configurationResource.getDescription();
    }

    private Resource resolveResource(ResourceLoader resourceLoader, String configurationFile) {
        if (configurationFile.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)
                || configurationFile.startsWith("file:")
                || configurationFile.startsWith("http:")
                || configurationFile.startsWith("https:")) {
            return resourceLoader.getResource(configurationFile);
        }
        return new FileSystemResource(configurationFile);
    }
}

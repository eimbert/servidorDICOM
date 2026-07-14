package fhes.cat.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ConfigurationFallbackLoaderTest {
    @Test
    void loadsFlatConfigValorList() throws Exception {
        Path file = Files.createTempFile("config-valors", ".json");
        Files.writeString(file, "[{\"camp\":\"IP_PACS\",\"valor1\":\"192.0.2.10\"}]");
        ConfigurationFallbackLoader loader = new ConfigurationFallbackLoader(new ObjectMapper(), file.toString());
        assertThat(loader.load()).hasSize(1);
        assertThat(loader.load().get(0).getCamp()).isEqualTo("IP_PACS");
    }

    @Test
    void rejectsMissingFile() {
        ConfigurationFallbackLoader loader = new ConfigurationFallbackLoader(new ObjectMapper(), "missing-config.json");
        assertThatThrownBy(loader::load).isInstanceOf(Exception.class).hasMessageContaining("No existe");
    }
}

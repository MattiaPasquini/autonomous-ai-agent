package ch.supsi.pasquini.agente_ai_autonomo.config;

import ch.supsi.pasquini.agente_ai_autonomo.ai.gemini.AgentClientGemini;
import ch.supsi.pasquini.agente_ai_autonomo.tool.FilesystemTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AgentClientGeminiConfig {

    @Bean
    public AgentClientGemini getClient(@Value("${google.genai.api-key}") String apiKey,
                                       @Value("${google.genai.model}") String model,
                                       List<FilesystemTools> tools) {

        return AgentClientGemini.create(model, apiKey, tools.toArray());
    }
}

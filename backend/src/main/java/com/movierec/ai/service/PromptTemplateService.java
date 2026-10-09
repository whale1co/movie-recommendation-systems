package com.movierec.ai.service;

import com.movierec.ai.client.LlmClientException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class PromptTemplateService {
    private final String intentPrompt = load("prompts/intent-extraction-v1.txt");
    private final String recommendationPrompt = load("prompts/recommendation-explanation-v1.txt");

    public String intentPrompt() {
        return intentPrompt;
    }

    public String recommendationPrompt() {
        return recommendationPrompt;
    }

    private String load(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new LlmClientException("无法加载提示模板: " + path, ex);
        }
    }
}

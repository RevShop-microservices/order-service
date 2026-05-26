package com.example.order_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class EmbeddingService {

    @Value("${ai-service.url:http://localhost:8085}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Double> getEmbedding(String text) {
        try {
            String url = aiServiceUrl + "/api/ai/embeddings";
            List<Double> response = restTemplate.postForObject(
                    url,
                    text,
                    List.class
            );
            return response;
        } catch (Exception e) {
            throw new RuntimeException("AI embedding service unavailable: " + e.getMessage());
        }
    }
}


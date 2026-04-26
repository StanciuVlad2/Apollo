package com.restaurant.cocktails.service;

import com.restaurant.cocktails.dto.CocktailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-1.5-pro}")
    private String model;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.objectMapper = objectMapper;
    }

    public CocktailResponse generateCocktail(String userDescription) {
        try {
            String prompt = buildPrompt(userDescription);
            String response = callGeminiAPI(prompt);
            return parseCocktailResponse(response);
        } catch (Exception e) {
            log.error("Error generating cocktail", e);
            throw new RuntimeException("Failed to generate cocktail: " + e.getMessage());
        }
    }

    private String buildPrompt(String userDescription) {
        return String.format("""
                Based on the following description about a person, create a unique and creative cocktail recipe.

                User description: %s

                Please provide a response in the following JSON format:
                {
                  "name": "cocktail name",
                  "story": "a creative story about this cocktail that relates to the user's description (2-3 sentences)",
                  "ingredients": ["ingredient 1", "ingredient 2", "ingredient 3"],
                  "instructions": "detailed mixing instructions"
                }

                Make sure the cocktail is creative, unique, and reflects the user's personality or preferences described above.
                IMPORTANT: Respond ONLY with valid JSON, no additional text before or after.
                """, userDescription);
    }

    private String callGeminiAPI(String prompt) {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            ),
            "generationConfig", Map.of(
                "temperature", 0.9,
                "topK", 40,
                "topP", 0.95,
                "maxOutputTokens", 8092
            )
        );

        String response = webClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/v1beta/models/" + model + ":generateContent")
                .queryParam("key", apiKey)
                .build())
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        log.info("Gemini API response: {}", response);
        return response;
    }

    private CocktailResponse parseCocktailResponse(String apiResponse) {
        try {
            JsonNode root = objectMapper.readTree(apiResponse);
            JsonNode candidates = root.path("candidates");

            if (candidates.isEmpty()) {
                throw new RuntimeException("No response from Gemini API");
            }

            String generatedText = candidates.get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

            // Extract JSON from potential markdown code blocks
            String jsonText = generatedText.trim();
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7);
            }
            if (jsonText.startsWith("```")) {
                jsonText = jsonText.substring(3);
            }
            if (jsonText.endsWith("```")) {
                jsonText = jsonText.substring(0, jsonText.length() - 3);
            }
            jsonText = jsonText.trim();

            JsonNode cocktailJson = objectMapper.readTree(jsonText);

            CocktailResponse cocktailResponse = new CocktailResponse();
            cocktailResponse.setName(cocktailJson.path("name").asText());
            cocktailResponse.setStory(cocktailJson.path("story").asText());
            cocktailResponse.setInstructions(cocktailJson.path("instructions").asText());

            List<String> ingredients = new ArrayList<>();
            JsonNode ingredientsNode = cocktailJson.path("ingredients");
            if (ingredientsNode.isArray()) {
                ingredientsNode.forEach(node -> ingredients.add(node.asText()));
            }
            cocktailResponse.setIngredients(ingredients);

            return cocktailResponse;
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
            throw new RuntimeException("Failed to parse cocktail response: " + e.getMessage());
        }
    }
}

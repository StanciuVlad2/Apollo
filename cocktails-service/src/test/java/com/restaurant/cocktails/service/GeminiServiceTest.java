package com.restaurant.cocktails.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.cocktails.dto.CocktailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiServiceTest {

    private final GeminiService service = new GeminiService(WebClient.builder(), new ObjectMapper());

    @Test
    void generateCocktail_missingApiKey_failsFast() {
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "model", "gemini-2.5-flash");

        assertThatThrownBy(() -> service.generateCocktail("An adventurous person who loves citrus drinks"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Gemini API key is missing")
            .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void parseCocktailResponse_extractsPayloadFromMarkdownFence() throws Exception {
        String apiResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\n{\"name\":\"Sunset Breeze\",\"story\":\"A bright drink for an outgoing personality.\",\"ingredients\":[\"gin\",\"citrus\",\"mint\"],\"instructions\":\"Shake with ice and strain into a chilled glass.\"}\n```"
                      }
                    ]
                  }
                }
              ]
            }
            """;

        Method parseMethod = GeminiService.class.getDeclaredMethod("parseCocktailResponse", String.class);
        parseMethod.setAccessible(true);

        CocktailResponse response = (CocktailResponse) parseMethod.invoke(service, apiResponse);

        assertThat(response.getName()).isEqualTo("Sunset Breeze");
        assertThat(response.getStory()).contains("outgoing personality");
        assertThat(response.getInstructions()).contains("Shake with ice");
        assertThat(response.getIngredients()).containsExactly("gin", "citrus", "mint");
    }
}


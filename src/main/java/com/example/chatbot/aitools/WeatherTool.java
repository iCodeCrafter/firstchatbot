package com.example.chatbot.aitools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.util.Locale;

@Component
public class WeatherTool {

    private final RestClient restClient;
    private final String apiKey;

    public WeatherTool(RestClient.Builder restClientBuilder,
                       @Value("${weather.api-key:}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl("https://api.weatherapi.com/v1").build();
        this.apiKey = apiKey;
    }

    @Tool(description = """
            Get the current weather for a city, region, or postal code. Call this tool when current
            weather is relevant to the user's delivery-related question. Pass a location such as
            London or Paris, France. Returns observed conditions, temperature, humidity, and wind.
            """)
    public String getCurrentWeather(String location) {
        System.out.println("Using the Weather API");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Weather service is not configured. Set WEATHER_API_KEY.");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Location must not be blank.");
        }
        String query = location.trim();
        if (query.length() > 200) {
            throw new IllegalArgumentException("Location must be no longer than 200 characters.");
        }

        JsonNode response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/current.json")
                            .queryParam("key", apiKey)
                            .queryParam("q", query)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "Weather service returned HTTP status " + exception.getStatusCode().value() + ".");
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("Unable to reach the weather service.");
        }

        if (response == null) {
            throw new IllegalStateException("Weather service returned an empty response.");
        }

        JsonNode current = response.path("current");
        JsonNode locationData = response.path("location");
        String city = requiredText(locationData, "name");
        String region = text(locationData, "region");
        String country = text(locationData, "country");
        String observedAt = requiredText(locationData, "localtime");
        String condition = requiredText(current.path("condition"), "text");

        return String.format(Locale.ROOT,
                "Current weather in %s%s%s (observed %s): %s; %.1f°C (%.1f°F), "
                        + "feels like %.1f°C (%.1f°F); humidity %d%%; wind %.1f km/h.",
                city,
                region.isBlank() ? "" : ", " + region,
                country.isBlank() ? "" : ", " + country,
                observedAt,
                condition,
                requiredNumber(current, "temp_c"),
                requiredNumber(current, "temp_f"),
                requiredNumber(current, "feelslike_c"),
                requiredNumber(current, "feelslike_f"),
                requiredInteger(current, "humidity"),
                requiredNumber(current, "wind_kph"));
    }

    private static String requiredText(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalStateException("Weather service response is missing " + field + ".");
        }
        return value.asText();
    }

    private static String text(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        return value.isTextual() ? value.asText() : "";
    }

    private static double requiredNumber(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        if (!value.isNumber() || !Double.isFinite(value.doubleValue())) {
            throw new IllegalStateException("Weather service response is missing " + field + ".");
        }
        return value.doubleValue();
    }

    private static int requiredInteger(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        if (!value.canConvertToInt()) {
            throw new IllegalStateException("Weather service response is missing " + field + ".");
        }
        return value.intValue();
    }
}

package com.example.chatbot.aitools;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeatherToolTest {

    @Test
    void fetchesAndFormatsCurrentWeather() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WeatherTool tool = new WeatherTool(builder, "test-key");
        server.expect(requestTo("https://api.weatherapi.com/v1/current.json?key=test-key&q=London"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "location": {
                            "name": "London",
                            "region": "City of London, Greater London",
                            "country": "United Kingdom",
                            "localtime": "2026-11-04 17:00"
                          },
                          "current": {
                            "temp_c": 12.5,
                            "temp_f": 54.5,
                            "feelslike_c": 11.0,
                            "feelslike_f": 51.8,
                            "humidity": 70,
                            "wind_kph": 8.0,
                            "condition": {"text": "Partly cloudy"}
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        String result = tool.getCurrentWeather(" London ");

        assertTrue(result.contains("London, City of London, Greater London, United Kingdom"));
        assertTrue(result.contains("Partly cloudy"));
        assertTrue(result.contains("12.5°C (54.5°F)"));
        assertTrue(result.contains("humidity 70%"));
        server.verify();
    }

    @Test
    void reportsProviderStatusWithoutExposingTheApiKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WeatherTool tool = new WeatherTool(builder, "secret-test-key");
        server.expect(requestTo("https://api.weatherapi.com/v1/current.json?key=secret-test-key&q=London"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> tool.getCurrentWeather("London"));

        assertEquals("Weather service returned HTTP status 401.", exception.getMessage());
        assertTrue(!exception.getMessage().contains("secret-test-key"));
        server.verify();
    }

    @Test
    void rejectsBlankLocationAndMissingApiKeyBeforeMakingARequest() {
        WeatherTool configuredTool = new WeatherTool(RestClient.builder(), "test-key");
        assertThrows(IllegalArgumentException.class, () -> configuredTool.getCurrentWeather("  "));

        WeatherTool unconfiguredTool = new WeatherTool(RestClient.builder(), " ");
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> unconfiguredTool.getCurrentWeather("London"));
        assertTrue(exception.getMessage().contains("WEATHER_API_KEY"));
    }
}

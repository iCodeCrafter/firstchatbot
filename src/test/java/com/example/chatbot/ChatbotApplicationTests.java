package com.example.chatbot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
class ChatbotApplicationTests {

    @Test
    void contextLoads() {
    }
}
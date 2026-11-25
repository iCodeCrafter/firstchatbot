package com.example.chatbot.controller;

import com.example.chatbot.service.WebsiteBuilderService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class WebsiteBuilderControllerTest {

    @Test
    void rejectsBlankRequirementsBeforeCallingTheService() {
        WebsiteBuilderService service = mock(WebsiteBuilderService.class);
        WebsiteBuilderController controller = new WebsiteBuilderController(service);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.generateWebsite(" \n "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(service);
    }
}

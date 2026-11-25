package com.example.chatbot.controller;

import com.example.chatbot.service.WebsiteBuilderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/app/api/builder")
public class WebsiteBuilderController {

    private final WebsiteBuilderService websiteBuilderService;

    public WebsiteBuilderController(WebsiteBuilderService websiteBuilderService) {
        this.websiteBuilderService = websiteBuilderService;
    }

    @PostMapping("/website")
    public String generateWebsite(@RequestBody String message) {
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Website requirements must not be blank");
        }
        return websiteBuilderService.generate(message);
    }
}

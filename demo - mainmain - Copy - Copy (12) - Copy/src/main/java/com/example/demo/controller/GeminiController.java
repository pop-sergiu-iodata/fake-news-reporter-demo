package com.example.demo.controller;

import com.example.demo.service.RabbitMQSender;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiController {

    private final RabbitMQSender rabbitMQSender;

    @PostMapping("/ask-with-response")
    public String askWithResponse(@RequestBody String text) {
        rabbitMQSender.send(text);
        return "Request received and queued. The result will be processed and logged.";
    }
}

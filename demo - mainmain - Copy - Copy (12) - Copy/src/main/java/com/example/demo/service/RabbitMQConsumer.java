package com.example.demo.service;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.model.GeminiResponse;
import com.google.genai.errors.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQConsumer {

    private final GeminiService geminiService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receiveMessage(String prompt) throws InterruptedException {
        System.out.println("Received message: " + prompt);
        try {
            GeminiResponse response = geminiService.askGeminiWithResponse(prompt);
            System.out.println("Response from Gemini: " + response);
            // Respect the rate limit of 10 requests per minute
            Thread.sleep(7000);
        } catch (ClientException e) {
            if (e.getMessage().contains("429")) {
                System.err.println("Error: Daily limit reached. Cannot send more messages today.");
            } else {
                throw e;
            }
        }
    }
}

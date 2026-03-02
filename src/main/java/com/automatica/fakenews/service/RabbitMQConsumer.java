package com.automatica.fakenews.service;

import com.automatica.fakenews.config.RabbitMQConfig;
import com.automatica.fakenews.model.GeminiResponse;
import com.google.genai.errors.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQConsumer {

    private final GeminiService geminiService;

    @Autowired
    public RabbitMQConsumer(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public String receiveMessage(String prompt) {
        System.out.println("Received message for fact-checking: " + prompt);
        try {
            GeminiResponse response = geminiService.askGeminiWithResponse(prompt);
            System.out.println("Response from Gemini: " + response);
            return response.name();
        } catch (ClientException e) {
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                System.err.println("Error: Daily limit reached. Cannot send more messages today.");
                return "RATE_LIMIT_EXCEEDED";
            } else {
                e.printStackTrace();
                return "ERROR";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }
}

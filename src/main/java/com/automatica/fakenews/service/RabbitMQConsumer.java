package com.automatica.fakenews.service;

import com.automatica.fakenews.config.RabbitMQConfig;
import com.automatica.fakenews.model.GeminiResponse;
import com.google.genai.errors.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConsumer.class);
    private final GeminiService geminiService;

    @Autowired
    public RabbitMQConsumer(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public String receiveMessage(String prompt) {
        log.info("Received message for fact-checking: {}", prompt);
        try {
            GeminiResponse response = geminiService.askGeminiWithResponse(prompt);
            log.info("Response from Gemini: {}", response);
            return response.name();
        } catch (ClientException e) {
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                log.error("Error: Daily limit reached. Cannot send more messages today.");
                return "RATE_LIMIT_EXCEEDED";
            } else {
                log.error("ClientException in RabbitMQConsumer", e);
                return "ERROR";
            }
        } catch (Exception e) {
            log.error("Exception in RabbitMQConsumer", e);
            return "ERROR";
        }
    }
}

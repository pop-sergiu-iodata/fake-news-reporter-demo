package com.example.demo.service;

import com.example.demo.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQSender {

    private final RabbitTemplate rabbitTemplate;

    public void send(String prompt) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NAME, prompt);
    }
}

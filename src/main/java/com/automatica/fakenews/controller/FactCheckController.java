package com.automatica.fakenews.controller;

import com.automatica.fakenews.config.RabbitMQConfig;
import com.automatica.fakenews.model.FactCheckHistory;
import com.automatica.fakenews.model.GeminiResponse;
import com.automatica.fakenews.model.User;
import com.automatica.fakenews.repository.FactCheckHistoryRepository;
import com.automatica.fakenews.repository.UserRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class FactCheckController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private FactCheckHistoryRepository factCheckHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/fact-check")
    public String factCheckForm() {
        return "fact-check";
    }

    @PostMapping("/fact-check")
    public String performFactCheck(@RequestParam("text") String text, Model model, Principal principal) {
        System.out.println("Sending fact-check request to RabbitMQ: " + text);
        // Send to queue and wait for the synchronous reply
        Object response = rabbitTemplate.convertSendAndReceive(RabbitMQConfig.QUEUE_NAME, text);
        
        String result = (response != null) ? response.toString() : "NO_RESPONSE";
        System.out.println("Received fact-check response from RabbitMQ: " + result);
        
        // Save to history if logged in and result is a valid GeminiResponse
        if (principal != null) {
            try {
                GeminiResponse geminiResponse = GeminiResponse.valueOf(result);
                Optional<User> userOpt = userRepository.findByUsername(principal.getName());
                if (userOpt.isPresent()) {
                    FactCheckHistory history = new FactCheckHistory(userOpt.get(), text, geminiResponse, LocalDateTime.now());
                    factCheckHistoryRepository.save(history);
                }
            } catch (IllegalArgumentException e) {
                // Not a valid GeminiResponse (e.g., RATE_LIMIT_EXCEEDED, ERROR), don't save to history
            }
        }
        
        model.addAttribute("text", text);
        model.addAttribute("result", result);
        return "fact-check";
    }

    @GetMapping("/fact-check/history")
    public String viewHistory(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<User> userOpt = userRepository.findByUsername(principal.getName());
        if (userOpt.isPresent()) {
            List<FactCheckHistory> history = factCheckHistoryRepository.findByUserOrderByTimestampDesc(userOpt.get());
            model.addAttribute("history", history);
        }
        
        return "fact-check-history";
    }
}

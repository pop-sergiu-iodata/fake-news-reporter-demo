package com.example.demo.service;

import com.example.demo.model.GeminiResponse;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final Client client;

    public String askGemini(String prompt){
        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-2.5-flash-lite",
                        prompt,
                        null);

       return response.text();
    }

    public GeminiResponse askGeminiWithResponse(String text) {
        String response = askGemini("Answer with TRUE, FALSE or INCONCLUSIVE:" + text);
        if (response.toLowerCase().contains("true")) {
            return GeminiResponse.TRUE;
        } else if (response.toLowerCase().contains("false")) {
            return GeminiResponse.FALSE;
        } else {
            return GeminiResponse.INCONCLUSIVE;
        }
    }
}

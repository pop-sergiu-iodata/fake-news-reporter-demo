package com.automatica.fakenews.service;

import com.automatica.fakenews.model.GeminiResponse;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final Client client;

    @Autowired
    public GeminiService(Client client) {
        this.client = client;
    }

    public String askGemini(String prompt){
        try {
            GenerateContentResponse response =
                    client.models.generateContent(
                            "gemini-2.5-flash-lite",
                            prompt,
                            null);

            return response.text();
        } catch (Exception e) {
            System.err.println("Error communicating with Gemini: " + e.getMessage());
            return "ERROR";
        }
    }

    public GeminiResponse askGeminiWithResponse(String text) {
        String response = askGemini("Fact check this. Answer ONLY with TRUE, FALSE or INCONCLUSIVE: " + text);
        if (response.toUpperCase().contains("TRUE")) {
            return GeminiResponse.TRUE;
        } else if (response.toUpperCase().contains("FALSE")) {
            return GeminiResponse.FALSE;
        } else {
            return GeminiResponse.INCONCLUSIVE;
        }
    }
}

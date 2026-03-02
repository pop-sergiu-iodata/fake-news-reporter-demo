package com.example.demo;

import com.example.demo.service.GeminiService;
import com.google.genai.Client;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class GeminiTest
{
    @Test
    void askGeminiAThing()
    {
        Client client = new Client();
        GeminiService service = new GeminiService(client);
        String prompt = "Apa fierbe la 50 de grade";
        System.out.println(service.askGemini(prompt));
    }

    /*
    @Test
    void askGeminiFromConsole()
    {
        System.out.println("Introdu textul pentru Gemini:");
        Scanner input = new Scanner(System.in);
        String prompt = input.nextLine();

        Client client = new Client(); // folosește cheia din env
        GeminiService service = new GeminiService(new GeminiClient(client));


        System.out.println(service.askGemini(prompt));
    }
   */
    @Test
    void askGeminiFromNews()
    {
        Client client = new Client();
        GeminiService service = new GeminiService(client);
        String prompt = "When Helene Svinos had to urgently admit her dog for treatment last year it cost her £1,600.\n" +
                "\n" +
                "She ended up having to take out a loan to cover subsequent treatment which was higher than her insurance policy covered.\n" +
                "\n" +
                "It's one case illustrating why veterinary practices may now be forced to publish prices of common treatments so pet owners can shop around and choose the best value option.\n" +
                "\n" +
                "They will also need to have an official operating licence to help drive up standards, under proposals from the Department for Environment, Food & Rural Affairs (Defra) -in the first reforms in 60 years";
        System.out.println(service.askGemini(prompt));
    }

    @Test
    void askGeminiFromConsole() {
        Client client = new Client();  // SDK-ul citește API key din environment
        GeminiService service = new GeminiService(client);

        Scanner input = new Scanner(System.in);
        System.out.println("Introdu textul pentru Gemini (scrie 'exit' pentru a opri):");

        while (true) {
            String prompt = input.nextLine();
            if (prompt.equalsIgnoreCase("exit")) break;

            String result = service.askGemini(prompt);
            System.out.println("Răspuns Gemini: " + result);
        }
    }

    @Test
    void askGeminiToReturnOne()
    {
        Client client = new Client();
        GeminiService service = new GeminiService(client);
        String prompt = "Apa fierbe la 50 de grade";
        System.out.println(service.askGemini(prompt + ". Raspunde doar cu un cuvant, TRUE sau FALSE"));
    }

    @Test
    void askGeminiToReturnOneNews()
    {
        Client client = new Client();
        GeminiService service = new GeminiService(client);
        String prompt = "When Helene Svinos had to urgently admit her dog for treatment last year it cost her £1,600.\n" +
                "\n" +
                "She ended up having to take out a loan to cover subsequent treatment which was higher than her insurance policy covered.\n" +
                "\n" +
                "It's one case illustrating why veterinary practices may now be forced to publish prices of common treatments so pet owners can shop around and choose the best value option.\n" +
                "\n" +
                "They will also need to have an official operating licence to help drive up standards, under proposals from the Department for Environment, Food & Rural Affairs (Defra) -in the first reforms in 60 years";
       String  result = service.askGemini(prompt + ". PLEASE ANSWER WITH ONE WORD, TRUE OR FALSE");

        assertEquals("TRUE", result);
    }
}

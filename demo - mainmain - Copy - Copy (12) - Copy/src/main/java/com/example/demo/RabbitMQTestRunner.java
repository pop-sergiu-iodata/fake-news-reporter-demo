package com.example.demo;

import com.example.demo.service.RabbitMQSender;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
@RequiredArgsConstructor
public class RabbitMQTestRunner implements CommandLineRunner {


    private final RabbitMQSender rabbitMQSender;

    @Override
    public void run(String... args) throws Exception {
        Scanner input = new Scanner(System.in);
        System.out.println("Enter text for Gemini (or type 'exit' to quit):");

        while (true) {
            System.out.print("Text: ");
            String prompt = input.nextLine();

            if (prompt.equalsIgnoreCase("exit")) {
                System.out.println("Exiting Gemini test runner.");
                break;
            }

            rabbitMQSender.send(prompt);
            System.out.println("Sent '" + prompt + "' to the queue.");
        }

        input.close();
    }
}

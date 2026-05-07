package com.automatica.fakenews.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MyService {

    private static final Logger log =
            LoggerFactory.getLogger(MyService.class);

    public void doWork() {
        log.info("Processing started");
        Exception exception = new RuntimeException("Test exception");
        log.error("Something failed", exception);
    }
}
package com.automatica.fakenews.service;

import com.automatica.fakenews.model.FakeNewsReport;
import com.automatica.fakenews.repository.FakeNewsReportRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class LocalAgentService {

    private static final Logger log = LoggerFactory.getLogger(LocalAgentService.class);

    @Autowired
    private FakeNewsReportRepository reportRepository;

    @Value("${python.path:ai_agent/.venv/Scripts/python.exe}")
    private String pythonPath;

    @Value("${local.ai.script:local_ai_agent/LocalAIAgent.py}")
    private String scriptPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public CompletableFuture<Void> classifyUncategorizedReports() {
        List<FakeNewsReport> reportsToClassify = reportRepository.findByCategory("Other");
        reportsToClassify.addAll(reportRepository.findByCategory("General"));
        reportsToClassify.addAll(reportRepository.findByCategory(null));

        if (reportsToClassify.isEmpty()) {
            log.info("No reports found for local classification.");
            return CompletableFuture.completedFuture(null);
        }

        log.info("Starting batch classification for {} reports.", reportsToClassify.size());

        try {
            // Prepare data for Python
            List<Map<String, Object>> inputItems = new ArrayList<>();
            for (FakeNewsReport report : reportsToClassify) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", report.getId());
                item.put("text", report.getDescription());
                inputItems.add(item);
            }

            String jsonInput = objectMapper.writeValueAsString(inputItems);

            // Create a temporary file to pass the JSON payload to Python
            File tempInputFile = File.createTempFile("local_ai_input", ".json");
            java.nio.file.Files.writeString(tempInputFile.toPath(), jsonInput);

            // Execute Python script once, passing the file path
            String jsonOutput = runBatchClassification(tempInputFile.getAbsolutePath());

            // Delete the temp file after execution
            tempInputFile.delete();

            if (jsonOutput != null && !jsonOutput.isEmpty()) {
                List<Map<String, Object>> results = objectMapper.readValue(
                        jsonOutput, 
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                for (Map<String, Object> result : results) {
                    Long id = Long.valueOf(result.get("id").toString());
                    String category = (String) result.get("category");

                    reportRepository.findById(id).ifPresent(report -> {
                        if (category != null && !category.equals("Other")) {
                            report.setCategory(category);
                            reportRepository.save(report);
                        }
                    });
                }
                log.info("Batch classification completed successfully.");
            }
        } catch (Exception e) {
            log.error("Error during batch classification", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private String runBatchClassification(String jsonInput) {
        try {
            String workingDir = System.getProperty("user.dir");
            File pythonFile = new File(workingDir, pythonPath);
            File scriptFile = new File(workingDir, scriptPath);

            ProcessBuilder pb = new ProcessBuilder(
                    pythonFile.getAbsolutePath(), 
                    scriptFile.getAbsolutePath(), 
                    jsonInput
            );
            pb.directory(new File(workingDir));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString();
            } else {
                log.error("Batch Python script failed with exit code: {}", exitCode);
            }
        } catch (Exception e) {
            log.error("Error running batch classification script", e);
        }
        return null;
    }
}

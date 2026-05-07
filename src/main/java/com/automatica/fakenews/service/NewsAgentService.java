package com.automatica.fakenews.service;

import com.automatica.fakenews.model.FakeNewsReport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NewsAgentService {

    private static final Logger log = LoggerFactory.getLogger(NewsAgentService.class);

    @Autowired
    private FakeNewsReportService reportService;

    @Value("${python.path:ai_agent/.venv/Scripts/python.exe}")
    private String pythonPath;

    @Value("${python.script:ai_agent/TestAgain.py}")
    private String scriptPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<FakeNewsReport> fetchAndAnalyzeTopNews() {
        List<FakeNewsReport> newReports = new ArrayList<>();
        try {
            // Resolve relative paths to absolute paths
            String workingDir = System.getProperty("user.dir");
            File pythonFile = new File(workingDir, pythonPath);
            File scriptFile = new File(workingDir, scriptPath);

            log.info("Executing Python script: {}", scriptFile.getAbsolutePath());
            log.info("Using Python interpreter: {}", pythonFile.getAbsolutePath());

            // Build the process to run the Python script
            ProcessBuilder pb = new ProcessBuilder(pythonFile.getAbsolutePath(), scriptFile.getAbsolutePath());
            pb.directory(new File(workingDir));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            
            // Read the output from the script
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("PYTHON OUTPUT: {}", line); // Log each line as it comes
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // Parse the JSON output
                List<Map<String, String>> results = objectMapper.readValue(
                    output.toString(), 
                    new TypeReference<List<Map<String, String>>>() {}
                );

                for (Map<String, String> result : results) {
                    FakeNewsReport report = new FakeNewsReport();
                    report.setNewsSource(result.getOrDefault("source", "AI Auto-Fetch"));
                    report.setUrl(result.getOrDefault("url", "N/A"));
                    report.setCategory("General");
                    
                    String title = result.getOrDefault("title", "No Title");
                    String content = result.getOrDefault("content", "No content");
                    String analysis = result.getOrDefault("analysis", "No analysis");
                    
                    report.setDescription("TITLE: " + title + "\n\nCONTENT:\n" + content + "\n\nAI ANALYSIS:\n" + analysis);
                    report.setReportedAt(LocalDateTime.now());
                    report.setApproved(true); // Auto-approved since it's an admin action
                    
                    // Save to database via existing service
                    reportService.saveReport(report);
                    newReports.add(report);
                }
            } else {
                log.error("Python script failed with exit code: {}", exitCode);
                log.error("Output: {}", output.toString());
            }

        } catch (Exception e) {
            log.error("Exception during Python script execution", e);
        }
        return newReports;
    }
}

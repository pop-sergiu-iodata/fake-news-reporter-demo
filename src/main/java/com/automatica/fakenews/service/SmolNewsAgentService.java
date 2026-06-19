package com.automatica.fakenews.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SmolNewsAgentService {

    private static final Logger log = LoggerFactory.getLogger(SmolNewsAgentService.class);

    @Value("${smol.python.path:smol_fetch_news_and_classify_app/.venv/Scripts/python.exe}")
    private String pythonPath;

    @Value("${smol.script.path:smol_fetch_news_and_classify_app/run_agent_cli.py}")
    private String scriptPath;

    @Value("${smol.workspace.path:smol_fetch_news_and_classify_app/workspace}")
    private String workspacePath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String runAgent(String prompt) {
        try {
            String workingDir = System.getProperty("user.dir");
            File pythonFile = new File(workingDir, pythonPath);
            File scriptFile = new File(workingDir, scriptPath);
            File appDir = new File(workingDir, "smol_fetch_news_and_classify_app");

            log.info("Executing Smol Agent with prompt: {}", prompt);

            ProcessBuilder pb = new ProcessBuilder(pythonFile.getAbsolutePath(), scriptFile.getAbsolutePath(), prompt);
            pb.directory(appDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("SMOL AGENT OUTPUT: {}", line);
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString();
            } else {
                log.error("Smol Agent failed with exit code: {}", exitCode);
                return "Error: Agent failed with exit code " + exitCode;
            }

        } catch (Exception e) {
            log.error("Exception during Smol Agent execution", e);
            return "Error: " + e.getMessage();
        }
    }

    public JsonNode getLatestNews(String source) {
        try {
            Path path = Paths.get(workspacePath, "news", source.toLowerCase() + "_news.json");
            if (Files.exists(path)) {
                return objectMapper.readTree(path.toFile());
            }
        } catch (Exception e) {
            log.error("Error reading news JSON", e);
        }
        return null;
    }

    public String extractSource(String output, String defaultSource) {
        if (output == null) return defaultSource;
        String lower = output.toLowerCase();
        if (lower.contains("bbc")) return "bbc";
        if (lower.contains("guardian")) return "guardian";
        if (lower.contains("techcrunch")) return "techcrunch";
        if (lower.contains("ars")) return "ars";
        if (lower.contains("sciencedaily")) return "sciencedaily";
        return defaultSource;
    }

    public List<String> getAvailableSources() {
        List<String> sources = new ArrayList<>();
        try {
            Path newsDir = Paths.get(workspacePath, "news");
            if (Files.exists(newsDir)) {
                Files.list(newsDir).forEach(path -> {
                    String filename = path.getFileName().toString();
                    if (filename.endsWith("_news.json")) {
                        sources.add(filename.replace("_news.json", ""));
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error listing news files", e);
        }
        return sources;
    }
}

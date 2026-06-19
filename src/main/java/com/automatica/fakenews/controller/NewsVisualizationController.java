package com.automatica.fakenews.controller;

import com.automatica.fakenews.service.SmolNewsAgentService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/news-visualization")
public class NewsVisualizationController {

    @Autowired
    private SmolNewsAgentService smolNewsAgentService;

    @GetMapping
    public String index(Model model, @RequestParam(required = false) String source) {
        List<String> sources = smolNewsAgentService.getAvailableSources();
        model.addAttribute("sources", sources);
        
        if (source != null && !source.isEmpty()) {
            JsonNode newsData = smolNewsAgentService.getLatestNews(source);
            model.addAttribute("newsData", newsData);
            model.addAttribute("selectedSource", source);
        } else if (!sources.isEmpty()) {
            // Default to first source if available
            String firstSource = sources.get(0);
            JsonNode newsData = smolNewsAgentService.getLatestNews(firstSource);
            model.addAttribute("newsData", newsData);
            model.addAttribute("selectedSource", firstSource);
        }
        
        return "admin/news-visualization";
    }

    @PostMapping("/run-agent")
    public String runAgent(@RequestParam String prompt, RedirectAttributes redirectAttributes) {
        String result = smolNewsAgentService.runAgent(prompt);
        redirectAttributes.addFlashAttribute("agentResult", result);
        
        // Try to guess the source from the agent output to redirect to the right view
        String source = smolNewsAgentService.extractSource(result, "bbc");
        
        return "redirect:/admin/news-visualization?source=" + source;
    }
}

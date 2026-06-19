package com.automatica.fakenews.controller;

import com.automatica.fakenews.model.FakeNewsReport;
import com.automatica.fakenews.service.FakeNewsReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private FakeNewsReportService reportService;

    @Autowired
    private com.automatica.fakenews.service.NewsAgentService newsAgentService;

    @Autowired
    private com.automatica.fakenews.service.LocalAgentService localAgentService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<FakeNewsReport> pendingReports = reportService.getPendingReports();
        List<FakeNewsReport> approvedReports = reportService.getApprovedReports();
        List<FakeNewsReport> rejectedReports = reportService.getRejectedReports();
        
        // Add all reports sorted by date, but specifically highlight AI ones
        model.addAttribute("pendingReports", pendingReports);
        model.addAttribute("approvedReports", approvedReports);
        model.addAttribute("rejectedReports", rejectedReports);
        
        return "admin/dashboard";
    }

    @PostMapping("/analyze-news")
    public String analyzeNews(RedirectAttributes redirectAttributes) {
        List<FakeNewsReport> results = newsAgentService.fetchAndAnalyzeTopNews();
        if (!results.isEmpty()) {
            redirectAttributes.addFlashAttribute("successMessage", "Successfully analyzed " + results.size() + " new articles!");
            redirectAttributes.addFlashAttribute("newlyAnalyzed", results);
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "AI analysis failed. Check logs.");
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/classify-local")
    public String classifyLocal(RedirectAttributes redirectAttributes) {
        localAgentService.classifyUncategorizedReports();
        redirectAttributes.addFlashAttribute("successMessage", "Local AI classification started in the background! Refresh in a moment to see results.");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/approve/{id}")
    public String approveReport(@PathVariable Long id, 
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        reportService.approveReport(id, username);
        redirectAttributes.addFlashAttribute("successMessage", "Report approved successfully!");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/reject/{id}")
    public String rejectReport(@PathVariable Long id, 
                               RedirectAttributes redirectAttributes) {
        reportService.rejectReport(id);
        redirectAttributes.addFlashAttribute("successMessage", "Report rejected successfully!");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/delete/{id}")
    public String deleteReport(@PathVariable Long id, 
                               RedirectAttributes redirectAttributes) {
        reportService.deleteReport(id);
        redirectAttributes.addFlashAttribute("successMessage", "Report deleted successfully!");
        return "redirect:/admin/dashboard";
    }
}

package com.automatica.fakenews.controller;

import com.automatica.fakenews.model.Comment;
import com.automatica.fakenews.model.FakeNewsReport;
import com.automatica.fakenews.model.User;
import com.automatica.fakenews.repository.CommentRepository;
import com.automatica.fakenews.repository.FakeNewsReportRepository;
import com.automatica.fakenews.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private FakeNewsReportRepository reportRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{id}")
    public String viewReport(@PathVariable Long id, Model model) {
        Optional<FakeNewsReport> reportOpt = reportRepository.findById(id);
        
        if (reportOpt.isPresent()) {
            FakeNewsReport report = reportOpt.get();
            model.addAttribute("report", report);
            model.addAttribute("comments", commentRepository.findByReportIdOrderByCreatedAtDesc(id));
            return "report-details";
        } else {
            return "redirect:/reports";
        }
    }

    @PostMapping("/{id}/comment")
    public String addComment(@PathVariable Long id, 
                             @RequestParam("content") String content,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        
        if (userDetails == null) {
            return "redirect:/login";
        }

        Optional<FakeNewsReport> reportOpt = reportRepository.findById(id);
        
        if (reportOpt.isPresent()) {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            FakeNewsReport report = reportOpt.get();
            
            Comment comment = new Comment(content, user, report);
            commentRepository.save(comment);
            
            redirectAttributes.addFlashAttribute("successMessage", "Comment added successfully!");
            return "redirect:/reports/" + id;
        } else {
            return "redirect:/reports";
        }
    }
}

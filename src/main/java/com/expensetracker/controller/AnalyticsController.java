package com.expensetracker.controller;

import com.expensetracker.entity.User;
import com.expensetracker.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
            if (endDate == null) endDate = LocalDate.now();
            
            Map<String, Object> data = analyticsService.getDashboardData(user, startDate, endDate);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("Error loading dashboard data for user: {}", user.getUsername(), e);
            throw new RuntimeException("Error loading dashboard data: " + e.getMessage(), e);
        }
    }

    @GetMapping("/category-spending")
    public ResponseEntity<List<Object[]>> getCategorySpending(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();
        
        return ResponseEntity.ok(analyticsService.getCategorySpending(user, startDate, endDate));
    }

    @GetMapping("/monthly-trend")
    public ResponseEntity<List<Object[]>> getMonthlyTrend(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(12);
        if (endDate == null) endDate = LocalDate.now();
        
        return ResponseEntity.ok(analyticsService.getMonthlyTrend(user, startDate, endDate));
    }

    @GetMapping("/budget-status")
    public ResponseEntity<List<Map<String, Object>>> getBudgetStatus(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(analyticsService.getBudgetStatus(user));
    }
}




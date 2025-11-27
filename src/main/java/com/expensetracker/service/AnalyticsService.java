package com.expensetracker.service;

import com.expensetracker.entity.*;
import com.expensetracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class AnalyticsService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    public Map<String, Object> getDashboardData(User user, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> dashboardData = new HashMap<>();
        
        // Total income and expenses
        BigDecimal totalIncome = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                user, TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpenses = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                user, TransactionType.EXPENSE, startDate, endDate);
        
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
        
        dashboardData.put("totalIncome", totalIncome);
        dashboardData.put("totalExpenses", totalExpenses);
        dashboardData.put("netAmount", totalIncome.subtract(totalExpenses));
        
        // Category-wise spending
        List<Object[]> categorySpending = transactionRepository.getCategoryWiseSpending(
                user, TransactionType.EXPENSE, startDate, endDate);
        dashboardData.put("categorySpending", categorySpending);
        
        // Monthly trend
        List<Object[]> monthlyTrend = transactionRepository.getMonthlyTrend(
                user, TransactionType.EXPENSE, startDate, endDate);
        dashboardData.put("monthlyTrend", monthlyTrend);
        
        return dashboardData;
    }

    public List<Object[]> getCategorySpending(User user, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.getCategoryWiseSpending(user, TransactionType.EXPENSE, startDate, endDate);
    }

    public List<Object[]> getMonthlyTrend(User user, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.getMonthlyTrend(user, TransactionType.EXPENSE, startDate, endDate);
    }

    public List<Map<String, Object>> getBudgetStatus(User user) {
        List<Budget> activeBudgets = budgetRepository.findActiveBudgetsForDate(user, LocalDate.now());
        List<Map<String, Object>> budgetStatus = new ArrayList<>();
        
        for (Budget budget : activeBudgets) {
            Map<String, Object> status = new HashMap<>();
            status.put("budget", budget);
            
            LocalDate startDate = budget.getStartDate();
            LocalDate endDate = budget.getEndDate();
            
            BigDecimal spent;
            if (budget.getCategory() != null) {
                spent = transactionRepository.sumAmountByUserAndTypeAndCategoryAndDateRange(
                        user, TransactionType.EXPENSE, budget.getCategory().getId(), startDate, endDate);
            } else {
                spent = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                        user, TransactionType.EXPENSE, startDate, endDate);
            }
            
            if (spent == null) spent = BigDecimal.ZERO;
            
            status.put("spent", spent);
            status.put("remaining", budget.getAmount().subtract(spent));
            status.put("percentage", spent.divide(budget.getAmount(), 2, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
            status.put("isOverBudget", spent.compareTo(budget.getAmount()) > 0);
            status.put("isNearLimit", spent.divide(budget.getAmount(), 2, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).compareTo(BigDecimal.valueOf(budget.getAlertThreshold())) >= 0);
            
            budgetStatus.add(status);
        }
        
        return budgetStatus;
    }
}




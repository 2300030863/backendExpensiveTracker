package com.expensetracker.service;

import com.expensetracker.entity.*;
import com.expensetracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnalyticsService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private UserGroupRepository userGroupRepository;

    public Map<String, Object> getDashboardData(User user, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> dashboardData = new HashMap<>();
        
        // Reload user with userGroup to handle lazy loading
        User fullUser = userRepository.findByIdWithUserGroup(user.getId())
                .orElse(user);
        
        UserGroup userGroup = fullUser.getUserGroup();
        boolean isInUserGroup = userGroup != null;
        boolean isAdmin = fullUser.getRole() == UserRole.ROLE_ADMIN;
        boolean isOwner = fullUser.getRole() == UserRole.ROLE_OWNER;
        
        BigDecimal totalIncome;
        BigDecimal totalExpenses;
        List<Object[]> categorySpending;
        List<Object[]> monthlyTrend;
        
        if (isOwner) {
            // Owner sees their own personal data for dashboard analytics
            totalIncome = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                    fullUser, TransactionType.INCOME, startDate, endDate);
            totalExpenses = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                    fullUser, TransactionType.EXPENSE, startDate, endDate);
            
            categorySpending = transactionRepository.getCategoryWiseSpending(
                    fullUser, TransactionType.EXPENSE, startDate, endDate);
            
            monthlyTrend = transactionRepository.getMonthlyTrend(
                    fullUser, TransactionType.EXPENSE, startDate, endDate);
        } else if (isAdmin) {
            // Admin - aggregate data from all managed users and their groups
            Optional<Admin> adminOpt = adminRepository.findByUsername(fullUser.getUsername());
            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                
                // Use the same query logic as transaction list to ensure consistency
                List<Transaction> allTransactions = transactionRepository.findByAdminManagedUsersAndDateRange(
                    fullUser, admin, startDate, endDate);
                
                totalIncome = allTransactions.stream()
                        .filter(t -> t.getType() == TransactionType.INCOME)
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                totalExpenses = allTransactions.stream()
                        .filter(t -> t.getType() == TransactionType.EXPENSE)
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                // Calculate category-wise spending
                Map<String, BigDecimal> categoryMap = allTransactions.stream()
                        .filter(t -> t.getType() == TransactionType.EXPENSE)
                        .collect(Collectors.groupingBy(
                                t -> t.getCategory().getName(),
                                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                        ));
                
                categorySpending = categoryMap.entrySet().stream()
                        .map(e -> new Object[]{e.getKey(), e.getValue()})
                        .collect(Collectors.toList());
                
                // Calculate monthly trend for admin's managed users
                monthlyTrend = transactionRepository.getMonthlyTrendByAdminManagedUsers(
                        fullUser, admin, TransactionType.EXPENSE, startDate, endDate);
            } else {
                // Admin entity not found, use personal data
                totalIncome = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                        fullUser, TransactionType.INCOME, startDate, endDate);
                totalExpenses = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                        fullUser, TransactionType.EXPENSE, startDate, endDate);
                
                categorySpending = transactionRepository.getCategoryWiseSpending(
                        fullUser, TransactionType.EXPENSE, startDate, endDate);
                
                monthlyTrend = transactionRepository.getMonthlyTrend(
                        fullUser, TransactionType.EXPENSE, startDate, endDate);
            }
        } else if (isInUserGroup) {
            // User is in a group - aggregate data from all group members
            Long groupId = userGroup.getId();
            
            totalIncome = transactionRepository.sumAmountByUserGroupAndTypeAndDateRange(
                    groupId, TransactionType.INCOME, startDate, endDate);
            totalExpenses = transactionRepository.sumAmountByUserGroupAndTypeAndDateRange(
                    groupId, TransactionType.EXPENSE, startDate, endDate);
            
            categorySpending = transactionRepository.getCategoryWiseSpendingByUserGroup(
                    groupId, TransactionType.EXPENSE, startDate, endDate);
            
            monthlyTrend = transactionRepository.getMonthlyTrendByUserGroup(
                    groupId, TransactionType.EXPENSE, startDate, endDate);
        } else {
            // Regular user - only their own transactions
            totalIncome = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                    user, TransactionType.INCOME, startDate, endDate);
            totalExpenses = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                    user, TransactionType.EXPENSE, startDate, endDate);
            
            categorySpending = transactionRepository.getCategoryWiseSpending(
                    user, TransactionType.EXPENSE, startDate, endDate);
            
            monthlyTrend = transactionRepository.getMonthlyTrend(
                    user, TransactionType.EXPENSE, startDate, endDate);
        }
        
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
        
        dashboardData.put("totalIncome", totalIncome);
        dashboardData.put("totalExpenses", totalExpenses);
        dashboardData.put("netAmount", totalIncome.subtract(totalExpenses));
        dashboardData.put("categorySpending", categorySpending);
        dashboardData.put("monthlyTrend", monthlyTrend);
        
        return dashboardData;
    }

    public List<Object[]> getCategorySpending(User user, LocalDate startDate, LocalDate endDate) {
        // Reload user with userGroup to handle lazy loading
        User fullUser = userRepository.findByIdWithUserGroup(user.getId())
                .orElse(user);
        
        boolean isAdmin = fullUser.getRole() == UserRole.ROLE_ADMIN;
        boolean isOwner = fullUser.getRole() == UserRole.ROLE_OWNER;
        UserGroup userGroup = fullUser.getUserGroup();
        
        if (isOwner) {
            // Owner - show only their personal category spending
            return transactionRepository.getCategoryWiseSpending(
                    fullUser, TransactionType.EXPENSE, startDate, endDate);
        } else if (isAdmin) {
            // Admin - aggregate category spending from all managed users using same query as transaction list
            Optional<Admin> adminOpt = adminRepository.findByUsername(fullUser.getUsername());
            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                
                // Use the same query logic as transaction list to ensure consistency
                List<Transaction> allTransactions = transactionRepository.findByAdminManagedUsersAndDateRange(
                    fullUser, admin, startDate, endDate);
                
                // Filter and group by category
                Map<String, BigDecimal> categoryMap = allTransactions.stream()
                        .filter(t -> t.getType() == TransactionType.EXPENSE)
                        .collect(Collectors.groupingBy(
                                t -> t.getCategory().getName(),
                                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                        ));
                
                return categoryMap.entrySet().stream()
                        .map(e -> new Object[]{e.getKey(), e.getValue()})
                        .collect(Collectors.toList());
            } else {
                // Admin entity not found, show personal category spending
                return transactionRepository.getCategoryWiseSpending(
                        fullUser, TransactionType.EXPENSE, startDate, endDate);
            }
        } else if (userGroup != null) {
            return transactionRepository.getCategoryWiseSpendingByUserGroup(
                    userGroup.getId(), TransactionType.EXPENSE, startDate, endDate);
        }
        return transactionRepository.getCategoryWiseSpending(user, TransactionType.EXPENSE, startDate, endDate);
    }

    public List<Object[]> getMonthlyTrend(User user, LocalDate startDate, LocalDate endDate) {
        // Reload user with userGroup to handle lazy loading
        User fullUser = userRepository.findByIdWithUserGroup(user.getId())
                .orElse(user);
        
        boolean isOwner = fullUser.getRole() == UserRole.ROLE_OWNER;
        boolean isAdmin = fullUser.getRole() == UserRole.ROLE_ADMIN;
        
        // Owner sees only their personal monthly trend
        if (isOwner) {
            return transactionRepository.getMonthlyTrend(fullUser, TransactionType.EXPENSE, startDate, endDate);
        }
        
        // Admin sees monthly trend for all managed users
        if (isAdmin) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(fullUser.getUsername());
            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                List<Transaction> allTransactions = transactionRepository.findByAdminManagedUsersAndDateRange(
                    fullUser, admin, startDate, endDate);
                
                // Group by month and sum expenses
                Map<String, BigDecimal> monthlyData = new HashMap<>();
                for (Transaction t : allTransactions) {
                    if (t.getType() == TransactionType.EXPENSE) {
                        String monthKey = t.getTransactionDate().getYear() + "-" + 
                                         String.format("%02d", t.getTransactionDate().getMonthValue());
                        monthlyData.merge(monthKey, t.getAmount(), BigDecimal::add);
                    }
                }
                
                // Convert to list of Object[]
                return monthlyData.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> new Object[]{e.getKey(), e.getValue()})
                    .collect(Collectors.toList());
            }
        }
        
        UserGroup userGroup = fullUser.getUserGroup();
        if (userGroup != null) {
            return transactionRepository.getMonthlyTrendByUserGroup(
                    userGroup.getId(), TransactionType.EXPENSE, startDate, endDate);
        }
        return transactionRepository.getMonthlyTrend(user, TransactionType.EXPENSE, startDate, endDate);
    }

    public List<Map<String, Object>> getBudgetStatus(User user) {
        List<Budget> activeBudgets = budgetRepository.findActiveBudgetsForDate(user, LocalDate.now());
        List<Map<String, Object>> budgetStatus = new ArrayList<>();
        
        // Reload user with userGroup to handle lazy loading
        User fullUser = userRepository.findByIdWithUserGroup(user.getId()).orElse(user);
        boolean isAdmin = fullUser.getRole() == UserRole.ROLE_ADMIN;
        boolean isOwner = fullUser.getRole() == UserRole.ROLE_OWNER;
        boolean isInUserGroup = fullUser.getUserGroup() != null;
        
        for (Budget budget : activeBudgets) {
            Map<String, Object> status = new HashMap<>();
            status.put("budget", budget);
            
            LocalDate startDate = budget.getStartDate();
            LocalDate endDate = budget.getEndDate();
            
            BigDecimal spent;
            
            // Calculate spent amount based on user context
            if (isOwner) {
                // Owner sees only their personal spending against budgets
                if (budget.getCategory() != null) {
                    spent = transactionRepository.sumAmountByUserAndTypeAndCategoryAndDateRange(
                            fullUser, TransactionType.EXPENSE, budget.getCategory().getId(), startDate, endDate);
                } else {
                    spent = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                            fullUser, TransactionType.EXPENSE, startDate, endDate);
                }
            } else if (isAdmin) {
                // Admin sees spending from all managed users using the same query as transactions
                Optional<Admin> adminOpt = adminRepository.findByUsername(fullUser.getUsername());
                if (adminOpt.isPresent()) {
                    Admin admin = adminOpt.get();
                    List<Transaction> allTransactions = transactionRepository.findByAdminManagedUsersAndDateRange(
                        fullUser, admin, startDate, endDate);
                    
                    // Filter by category if specified and sum expenses
                    if (budget.getCategory() != null) {
                        spent = allTransactions.stream()
                                .filter(t -> t.getType() == TransactionType.EXPENSE 
                                          && t.getCategory().getId().equals(budget.getCategory().getId()))
                                .map(Transaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                    } else {
                        spent = allTransactions.stream()
                                .filter(t -> t.getType() == TransactionType.EXPENSE)
                                .map(Transaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                    }
                } else {
                    // Admin entity not found, use personal spending
                    if (budget.getCategory() != null) {
                        spent = transactionRepository.sumAmountByUserAndTypeAndCategoryAndDateRange(
                                fullUser, TransactionType.EXPENSE, budget.getCategory().getId(), startDate, endDate);
                    } else {
                        spent = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                                fullUser, TransactionType.EXPENSE, startDate, endDate);
                    }
                }
            } else if (isInUserGroup) {
                // For users in a group, calculate spent for the entire group
                Long groupId = fullUser.getUserGroup().getId();
                if (budget.getCategory() != null) {
                    spent = transactionRepository.sumAmountByUserGroupAndTypeAndCategoryAndDateRange(
                            groupId, TransactionType.EXPENSE, budget.getCategory().getId(), startDate, endDate);
                } else {
                    spent = transactionRepository.sumAmountByUserGroupAndTypeAndDateRange(
                            groupId, TransactionType.EXPENSE, startDate, endDate);
                }
            } else {
                // For regular users, calculate their own spent amount
                if (budget.getCategory() != null) {
                    spent = transactionRepository.sumAmountByUserAndTypeAndCategoryAndDateRange(
                            fullUser, TransactionType.EXPENSE, budget.getCategory().getId(), startDate, endDate);
                } else {
                    spent = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                            fullUser, TransactionType.EXPENSE, startDate, endDate);
                }
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




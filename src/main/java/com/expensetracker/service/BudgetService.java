package com.expensetracker.service;

import com.expensetracker.entity.Budget;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.User;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Budget> getAllBudgets(User user) {
        return budgetRepository.findByUserAndIsActiveTrueOrderByStartDateDesc(user);
    }

    public Budget getBudget(User user, Long id) {
        return budgetRepository.findById(id)
                .filter(budget -> budget.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Budget not found"));
    }

    public Budget createBudget(User user, Budget budget) {
        if (budget.getCategory() != null && budget.getCategory().getId() != null) {
            Category category = categoryRepository.findById(budget.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            budget.setCategory(category);
        } else {
            budget.setCategory(null);
        }
        
        budget.setUser(user);
        return budgetRepository.save(budget);
    }

    public Budget updateBudget(User user, Long id, Budget budgetDetails) {
        Budget budget = getBudget(user, id);
        
        budget.setAmount(budgetDetails.getAmount());
        budget.setStartDate(budgetDetails.getStartDate());
        budget.setEndDate(budgetDetails.getEndDate());
        budget.setType(budgetDetails.getType());
        budget.setAlertThreshold(budgetDetails.getAlertThreshold());
        budget.setIsActive(budgetDetails.getIsActive());
        
        if (budgetDetails.getCategory() != null && budgetDetails.getCategory().getId() != null) {
            Category category = categoryRepository.findById(budgetDetails.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            budget.setCategory(category);
        } else {
            budget.setCategory(null);
        }
        
        return budgetRepository.save(budget);
    }

    public void deleteBudget(User user, Long id) {
        Budget budget = getBudget(user, id);
        budget.setIsActive(false);
        budgetRepository.save(budget);
    }
}




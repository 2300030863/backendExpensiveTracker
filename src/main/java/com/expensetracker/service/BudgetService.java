package com.expensetracker.service;

import com.expensetracker.entity.Admin;
import com.expensetracker.entity.Budget;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.User;
import com.expensetracker.entity.UserRole;
import com.expensetracker.repository.AdminRepository;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private AdminRepository adminRepository;

    public List<Budget> getAllBudgets(User user) {
        // If user is in a group, show all budgets from the group
        if (user.getUserGroup() != null) {
            return budgetRepository.findByUserGroupAndIsActiveTrueOrderByStartDateDesc(user.getUserGroup().getId());
        }
        return budgetRepository.findByUserAndIsActiveTrueOrderByStartDateDesc(user);
    }

    public Budget getBudget(User user, Long id) {
        return budgetRepository.findById(id)
                .filter(budget -> {
                    // Allow if user owns the budget
                    if (budget.getUser().getId().equals(user.getId())) return true;
                    // Allow if both users are in the same group
                    if (user.getUserGroup() != null && budget.getUser().getUserGroup() != null) {
                        return user.getUserGroup().getId().equals(budget.getUser().getUserGroup().getId());
                    }
                    return false;
                })
                .orElseThrow(() -> new RuntimeException("Budget not found"));
    }

    public Budget createBudget(User user, Budget budget) {
        // PERMISSION CHECK: Prevent group members from creating budgets
        // Allowed: Users NOT in a group, Group admins, ROLE_ADMIN, or ROLE_OWNER
        // Denied: Regular group members
        
        // Allow ROLE_ADMIN and ROLE_OWNER to create budgets without restrictions
        if (user.getRole() == UserRole.ROLE_ADMIN || user.getRole() == UserRole.ROLE_OWNER) {
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
        
        // For regular users: check if they are in a group
        if (user.getUserGroup() != null) {
            // User is in a group - check if they are the admin of the group
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                // Check if this admin manages the user's group
                boolean isGroupAdmin = user.getUserGroup().getAdmin().getId().equals(admin.getId());
                if (!isGroupAdmin) {
                    throw new RuntimeException("Access denied: Group members cannot create budgets. Only group admins can create budgets.");
                }
            } else {
                // User is in a group but is not an admin
                throw new RuntimeException("Access denied: Group members cannot create budgets. Only group admins can create budgets.");
            }
        }
        
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
        
        // PERMISSION CHECK: Ensure only authorized users can update budgets
        // Allowed: Budget owner, Group admin, or Owner role
        // Denied: Regular group members (even if they can view the budget)
        boolean isOwner = budget.getUser().getId().equals(user.getId());
        boolean isOwnerRole = user.getRole() == UserRole.ROLE_OWNER;
        
        // Check if user is the admin of the budget owner's group
        boolean isGroupAdmin = false;
        if (user.getRole() == UserRole.ROLE_ADMIN && budget.getUser().getUserGroup() != null) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Long budgetGroupAdminId = budget.getUser().getUserGroup().getAdmin().getId();
                isGroupAdmin = adminOpt.get().getId().equals(budgetGroupAdminId);
            }
        }
        
        // Reject if user is not owner, group admin, or owner role
        if (!isOwner && !isGroupAdmin && !isOwnerRole) {
            throw new RuntimeException("Access denied: You don't have permission to update this budget");
        }
        
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
        
        // PERMISSION CHECK: Ensure only authorized users can delete budgets
        // Allowed: Budget owner, Group admin, or Owner role
        // Denied: Regular group members (even if they can view the budget)
        boolean isOwner = budget.getUser().getId().equals(user.getId());
        boolean isOwnerRole = user.getRole() == UserRole.ROLE_OWNER;
        
        // Check if user is the admin of the budget owner's group
        boolean isGroupAdmin = false;
        if (user.getRole() == UserRole.ROLE_ADMIN && budget.getUser().getUserGroup() != null) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Long budgetGroupAdminId = budget.getUser().getUserGroup().getAdmin().getId();
                isGroupAdmin = adminOpt.get().getId().equals(budgetGroupAdminId);
            }
        }
        
        // Reject if user is not owner, group admin, or owner role
        if (!isOwner && !isGroupAdmin && !isOwnerRole) {
            throw new RuntimeException("Access denied: You don't have permission to delete this budget");
        }
        
        budget.setIsActive(false);
        budgetRepository.save(budget);
    }
}




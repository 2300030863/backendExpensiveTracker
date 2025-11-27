package com.expensetracker.controller;

import com.expensetracker.entity.Budget;
import com.expensetracker.entity.User;
import com.expensetracker.service.BudgetService;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/budgets")
@CrossOrigin(origins = "*")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<Budget>> getAllBudgets(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(budgetService.getAllBudgets(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Budget> getBudget(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(budgetService.getBudget(user, id));
    }

    @PostMapping
    public ResponseEntity<Budget> createBudget(@AuthenticationPrincipal User user, @Valid @RequestBody Budget budget) {
        return ResponseEntity.ok(budgetService.createBudget(user, budget));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody Budget budget) {
        return ResponseEntity.ok(budgetService.updateBudget(user, id, budget));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@AuthenticationPrincipal User user, @PathVariable Long id) {
        budgetService.deleteBudget(user, id);
        return ResponseEntity.ok().build();
    }
}




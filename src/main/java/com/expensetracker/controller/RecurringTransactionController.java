package com.expensetracker.controller;

import com.expensetracker.dto.RecurringTransactionDto;
import com.expensetracker.dto.RecurringTransactionRequest;
import com.expensetracker.entity.RecurringTransaction;
import com.expensetracker.entity.User;
import com.expensetracker.service.RecurringTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/recurring-transactions")
@CrossOrigin(origins = "*")
public class RecurringTransactionController {

    @Autowired
    private RecurringTransactionService recurringTransactionService;

    @GetMapping
    public ResponseEntity<List<RecurringTransactionDto>> getAllRecurringTransactions(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<RecurringTransaction> recurringTransactions = recurringTransactionService.getAllRecurringTransactions(user);
        List<RecurringTransactionDto> dtos = recurringTransactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/active")
    public ResponseEntity<List<RecurringTransactionDto>> getActiveRecurringTransactions(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<RecurringTransaction> recurringTransactions = recurringTransactionService.getActiveRecurringTransactions(user);
        List<RecurringTransactionDto> dtos = recurringTransactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringTransactionDto> getRecurringTransaction(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        RecurringTransaction recurringTransaction = recurringTransactionService.getRecurringTransaction(id, user);
        return ResponseEntity.ok(convertToDto(recurringTransaction));
    }

    @PostMapping
    public ResponseEntity<RecurringTransactionDto> createRecurringTransaction(@Valid @RequestBody RecurringTransactionRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        RecurringTransaction recurringTransaction = recurringTransactionService.createRecurringTransaction(user, request);
        return ResponseEntity.ok(convertToDto(recurringTransaction));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransactionDto> updateRecurringTransaction(@PathVariable Long id, @Valid @RequestBody RecurringTransactionRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        RecurringTransaction recurringTransaction = recurringTransactionService.updateRecurringTransaction(id, user, request);
        return ResponseEntity.ok(convertToDto(recurringTransaction));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringTransaction(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        recurringTransactionService.deleteRecurringTransaction(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<RecurringTransactionDto> toggleRecurringTransaction(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        RecurringTransaction recurringTransaction = recurringTransactionService.toggleRecurringTransaction(id, user);
        return ResponseEntity.ok(convertToDto(recurringTransaction));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Void> executeRecurringTransaction(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        recurringTransactionService.executeRecurringTransaction(id, user);
        return ResponseEntity.ok().build();
    }

    private RecurringTransactionDto convertToDto(RecurringTransaction recurringTransaction) {
        RecurringTransactionDto dto = new RecurringTransactionDto();
        dto.setId(recurringTransaction.getId());
        dto.setDescription(recurringTransaction.getDescription());
        dto.setAmount(recurringTransaction.getAmount());
        dto.setType(recurringTransaction.getType());
        dto.setRecurrenceType(recurringTransaction.getRecurrenceType());
        dto.setStartDate(recurringTransaction.getStartDate());
        dto.setEndDate(recurringTransaction.getEndDate());
        dto.setNextDueDate(recurringTransaction.getNextDueDate());
        dto.setNotes(recurringTransaction.getNotes());
        dto.setIsActive(recurringTransaction.getIsActive());
        dto.setCreatedAt(recurringTransaction.getCreatedAt());
        dto.setUpdatedAt(recurringTransaction.getUpdatedAt());
        
        if (recurringTransaction.getCategory() != null) {
            dto.setCategoryId(recurringTransaction.getCategory().getId());
            dto.setCategoryName(recurringTransaction.getCategory().getName());
        }
        
        if (recurringTransaction.getAccount() != null) {
            dto.setAccountId(recurringTransaction.getAccount().getId());
            dto.setAccountName(recurringTransaction.getAccount().getName());
        }
        
        return dto;
    }
}


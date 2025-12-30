package com.expensetracker.controller;

import com.expensetracker.dto.TransactionDto;
import com.expensetracker.dto.TransactionRequest;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.User;
import com.expensetracker.service.TransactionService;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN', 'ROLE_OWNER')")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    private TransactionDto convertToDto(Transaction t, User currentUser) {
        TransactionDto dto = new TransactionDto(
                t.getId(), t.getAmount(), t.getDescription(), t.getType().name(), t.getTransactionDate(), t.getNotes(),
                t.getCategory().getId(), t.getCategory().getName(), t.getCategory().getColor(),
                t.getAccount().getId(), t.getAccount().getName(),
                t.getAccount().getType() != null ? t.getAccount().getType().name() : null,
                t.getCreatedAt()
        );
        
        // Add user information
        if (t.getUser() != null) {
            dto.setUserId(t.getUser().getId());
            dto.setUsername(t.getUser().getUsername());
            dto.setUserEmail(t.getUser().getEmail());
        }
        
        // Determine permissions based on role and ownership
        boolean isOwner = t.getUser() != null && t.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().name().equals("ROLE_ADMIN");
        boolean isOwnerRole = currentUser.getRole().name().equals("ROLE_OWNER");
        
        // Check if current user is admin of the transaction owner's group
        boolean isGroupAdmin = false;
        if (isAdmin && t.getUser() != null && t.getUser().getUserGroup() != null) {
            // Get the admin of the transaction owner's group and check if it matches current user
            try {
                if (t.getUser().getUserGroup().getAdmin() != null) {
                    String groupAdminUsername = t.getUser().getUserGroup().getAdmin().getUsername();
                    isGroupAdmin = groupAdminUsername.equals(currentUser.getUsername());
                }
            } catch (Exception e) {
                // Ignore lazy loading issues
            }
        }
        
        // PERMISSION RULES:
        // - Transaction owner: CAN edit/delete their own transactions
        // - Group admin (admin of the transaction owner's group): CAN edit/delete group members' transactions
        // - Owner role: CAN edit/delete any transaction
        // - Regular group members: CANNOT edit/delete other members' transactions (read-only access)
        boolean canEdit = isOwner || isGroupAdmin || isOwnerRole;
        boolean canDelete = isOwner || isGroupAdmin || isOwnerRole;
        
        dto.setCanDelete(canDelete);
        dto.setCanEdit(canEdit);
        
        return dto;
    }

    @GetMapping
    public ResponseEntity<Page<TransactionDto>> getAllTransactions(
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Page<Transaction> page = transactionService.getAllTransactions(user, pageable);
        Page<TransactionDto> mapped = page.map(t -> convertToDto(t, user));
        return ResponseEntity.ok(mapped);
    }

    @GetMapping("/search")
    public ResponseEntity<List<TransactionDto>> searchTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long accountId) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        List<Transaction> list = transactionService.searchTransactions(user, startDate, endDate, categoryId, accountId);
        return ResponseEntity.ok(list.stream().map(t -> convertToDto(t, user)).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransaction(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Transaction t = transactionService.getTransaction(user, id);
        return ResponseEntity.ok(convertToDto(t, user));
    }

    @PostMapping
    public ResponseEntity<?> createTransaction(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TransactionRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            Transaction t = transactionService.createTransaction(user, request);
            return ResponseEntity.ok(convertToDto(t, user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            Transaction t = transactionService.updateTransaction(user, id, request);
            return ResponseEntity.ok(convertToDto(t, user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        transactionService.deleteTransaction(user, id);
        return ResponseEntity.ok().build();
    }
}




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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public ResponseEntity<Page<TransactionDto>> getAllTransactions(
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Page<Transaction> page = transactionService.getAllTransactions(user, pageable);
        Page<TransactionDto> mapped = page.map(t -> new TransactionDto(
                t.getId(), t.getAmount(), t.getDescription(), t.getType().name(), t.getTransactionDate(), t.getNotes(),
                t.getCategory().getId(), t.getCategory().getName(), t.getCategory().getColor(),
                t.getAccount().getId(), t.getAccount().getName(),
                t.getAccount().getType() != null ? t.getAccount().getType().name() : null,
                t.getCreatedAt()
        ));
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
        return ResponseEntity.ok(list.stream().map(t -> new TransactionDto(
                t.getId(), t.getAmount(), t.getDescription(), t.getType().name(), t.getTransactionDate(), t.getNotes(),
                t.getCategory().getId(), t.getCategory().getName(), t.getCategory().getColor(),
                t.getAccount().getId(), t.getAccount().getName(),
                t.getAccount().getType() != null ? t.getAccount().getType().name() : null,
                t.getCreatedAt()
        )).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransaction(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Transaction t = transactionService.getTransaction(user, id);
        return ResponseEntity.ok(new TransactionDto(
                t.getId(), t.getAmount(), t.getDescription(), t.getType().name(), t.getTransactionDate(), t.getNotes(),
                t.getCategory().getId(), t.getCategory().getName(), t.getCategory().getColor(),
                t.getAccount().getId(), t.getAccount().getName(),
                t.getAccount().getType() != null ? t.getAccount().getType().name() : null,
                t.getCreatedAt()
        ));
    }

    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TransactionRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Transaction t = transactionService.createTransaction(user, request);
        return ResponseEntity.ok(new TransactionDto(
                t.getId(), t.getAmount(), t.getDescription(), t.getType().name(), t.getTransactionDate(), t.getNotes(),
                t.getCategory().getId(), t.getCategory().getName(), t.getCategory().getColor(),
                t.getAccount().getId(), t.getAccount().getName(),
                t.getAccount().getType() != null ? t.getAccount().getType().name() : null,
                t.getCreatedAt()
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDto> updateTransaction(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Transaction t = transactionService.updateTransaction(user, id, request);
        return ResponseEntity.ok(new TransactionDto(
                t.getId(), t.getAmount(), t.getDescription(), t.getType().name(), t.getTransactionDate(), t.getNotes(),
                t.getCategory().getId(), t.getCategory().getName(), t.getCategory().getColor(),
                t.getAccount().getId(), t.getAccount().getName(),
                t.getAccount().getType() != null ? t.getAccount().getType().name() : null,
                t.getCreatedAt()
        ));
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




package com.expensetracker.service;

import com.expensetracker.dto.TransactionRequest;
import com.expensetracker.entity.*;
import com.expensetracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    public Page<Transaction> getAllTransactions(User user, Pageable pageable) {
        return transactionRepository.findByUserOrderByTransactionDateDesc(user, pageable);
    }

    public List<Transaction> searchTransactions(User user, LocalDate startDate, LocalDate endDate, 
                                               Long categoryId, Long accountId) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        if (categoryId != null) {
            return transactionRepository.findByUserAndCategoryIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                    user, categoryId, startDate, endDate);
        } else if (accountId != null) {
            return transactionRepository.findByUserAndAccountIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                    user, accountId, startDate, endDate);
        } else {
            return transactionRepository.findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(
                    user, startDate, endDate);
        }
    }

    public Transaction getTransaction(User user, Long id) {
        return transactionRepository.findById(id)
                .filter(transaction -> transaction.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    public Transaction createTransaction(User user, TransactionRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setType(request.getType());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setNotes(request.getNotes());
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAccount(account);

        if (request.getRecurringTransactionId() != null) {
            RecurringTransaction recurringTransaction = recurringTransactionRepository.findById(request.getRecurringTransactionId())
                    .orElseThrow(() -> new RuntimeException("Recurring transaction not found"));
            transaction.setRecurringTransaction(recurringTransaction);
        }

        // Update account balance
        updateAccountBalance(account, request.getAmount(), request.getType());

        return transactionRepository.save(transaction);
    }

    public Transaction updateTransaction(User user, Long id, TransactionRequest request) {
        Transaction transaction = getTransaction(user, id);
        
        // Revert old transaction's impact on account balance
        updateAccountBalance(transaction.getAccount(), transaction.getAmount(), 
                transaction.getType() == TransactionType.EXPENSE ? TransactionType.INCOME : TransactionType.EXPENSE);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setType(request.getType());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setNotes(request.getNotes());
        transaction.setCategory(category);
        transaction.setAccount(account);

        if (request.getRecurringTransactionId() != null) {
            RecurringTransaction recurringTransaction = recurringTransactionRepository.findById(request.getRecurringTransactionId())
                    .orElseThrow(() -> new RuntimeException("Recurring transaction not found"));
            transaction.setRecurringTransaction(recurringTransaction);
        }

        // Apply new transaction's impact on account balance
        updateAccountBalance(account, request.getAmount(), request.getType());

        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(User user, Long id) {
        Transaction transaction = getTransaction(user, id);
        
        // Revert transaction's impact on account balance
        updateAccountBalance(transaction.getAccount(), transaction.getAmount(), 
                transaction.getType() == TransactionType.EXPENSE ? TransactionType.INCOME : TransactionType.EXPENSE);

        transactionRepository.delete(transaction);
    }

    private void updateAccountBalance(Account account, java.math.BigDecimal amount, TransactionType type) {
        if (type == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            account.setBalance(account.getBalance().subtract(amount));
        }
        accountRepository.save(account);
    }
}




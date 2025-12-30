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
import java.util.Optional;

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
    
    @Autowired
    private AdminRepository adminRepository;

    public Page<Transaction> getAllTransactions(User user, Pageable pageable) {
        // If user is admin, show all transactions from admin and users they manage
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                return transactionRepository.findByAdminManagedUsersOrderByTransactionDateDesc(
                    user, adminOpt.get(), pageable);
            } else {
                // Admin entity not found, just show admin's own transactions
                return transactionRepository.findByUserOrderByTransactionDateDesc(user, pageable);
            }
        }
        
        // If user is in a group, show only group member transactions (NOT admin's transactions)
        if (user.getUserGroup() != null) {
            return transactionRepository.findByUserGroupOrderByTransactionDateDesc(
                user.getUserGroup().getId(), pageable);
        }
        
        // Default: user's own transactions
        return transactionRepository.findByUserOrderByTransactionDateDesc(user, pageable);
    }

    public List<Transaction> searchTransactions(User user, LocalDate startDate, LocalDate endDate, 
                                               Long categoryId, Long accountId) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        // If user is admin, search across all managed user transactions and admin's own
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                if (categoryId != null) {
                    return transactionRepository.findByAdminManagedUsersAndCategoryAndDateRange(
                            user, admin, categoryId, startDate, endDate);
                } else if (accountId != null) {
                    return transactionRepository.findByAdminManagedUsersAndAccountAndDateRange(
                            user, admin, accountId, startDate, endDate);
                } else {
                    return transactionRepository.findByAdminManagedUsersAndDateRange(
                            user, admin, startDate, endDate);
                }
            } else {
                // Admin entity not found, search admin's own transactions
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
        }

        // If user is in a group, search only group member transactions (NOT admin's)
        if (user.getUserGroup() != null) {
            Long groupId = user.getUserGroup().getId();
            if (categoryId != null) {
                return transactionRepository.findByUserGroupAndCategoryAndDateRange(
                        groupId, categoryId, startDate, endDate);
            } else if (accountId != null) {
                return transactionRepository.findByUserGroupAndAccountAndDateRange(
                        groupId, accountId, startDate, endDate);
            } else {
                return transactionRepository.findByUserGroupAndDateRange(
                        groupId, startDate, endDate);
            }
        }

        // Default: user's own transactions
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
                .filter(transaction -> {
                    // Allow if user owns the transaction
                    if (transaction.getUser().getId().equals(user.getId())) {
                        return true;
                    }
                    // Allow if admin manages the transaction's user
                    if (user.getRole() == UserRole.ROLE_ADMIN) {
                        Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
                        if (adminOpt.isPresent()) {
                            Admin admin = adminOpt.get();
                            User txnUser = transaction.getUser();
                            if ((txnUser.getAdmin() != null && txnUser.getAdmin().getId().equals(admin.getId())) ||
                                (txnUser.getUserGroup() != null && txnUser.getUserGroup().getAdmin().getId().equals(admin.getId()))) {
                                return true;
                            }
                        }
                    }
                    // Allow if both users are in the same group
                    if (user.getUserGroup() != null && transaction.getUser().getUserGroup() != null) {
                        return user.getUserGroup().getId().equals(transaction.getUser().getUserGroup().getId());
                    }
                    return false;
                })
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    public Transaction createTransaction(User user, TransactionRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Check if account has sufficient balance for expense transactions
        if (request.getType() == TransactionType.EXPENSE) {
            if (account.getBalance().compareTo(request.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance in account. Available balance: " + account.getBalance());
            }
        }

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
        
        // PERMISSION CHECK: Ensure only authorized users can update transactions
        // Allowed: Transaction owner, Group admin, or Owner role
        // Denied: Regular group members (even if they can view the transaction)
        boolean isOwner = transaction.getUser().getId().equals(user.getId());
        boolean isOwnerRole = user.getRole() == UserRole.ROLE_OWNER;
        
        // Check if user is the admin of the transaction owner's group
        boolean isGroupAdmin = false;
        if (user.getRole() == UserRole.ROLE_ADMIN && transaction.getUser().getUserGroup() != null) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Long txnGroupAdminId = transaction.getUser().getUserGroup().getAdmin().getId();
                isGroupAdmin = adminOpt.get().getId().equals(txnGroupAdminId);
            }
        }
        
        // Reject if user is not owner, group admin, or owner role
        if (!isOwner && !isGroupAdmin && !isOwnerRole) {
            throw new RuntimeException("Access denied: You don't have permission to update this transaction");
        }
        
        // Revert old transaction's impact on account balance
        updateAccountBalance(transaction.getAccount(), transaction.getAmount(), 
                transaction.getType() == TransactionType.EXPENSE ? TransactionType.INCOME : TransactionType.EXPENSE);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Check if account has sufficient balance for expense transactions
        if (request.getType() == TransactionType.EXPENSE) {
            if (account.getBalance().compareTo(request.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance in account. Available balance: " + account.getBalance());
            }
        }

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
        
        // PERMISSION CHECK: Ensure only authorized users can delete transactions
        // Allowed: Transaction owner, Group admin, or Owner role
        // Denied: Regular group members (even if they can view the transaction)
        boolean isOwner = transaction.getUser().getId().equals(user.getId());
        boolean isOwnerRole = user.getRole() == UserRole.ROLE_OWNER;
        
        // Check if user is the admin of the transaction owner's group
        boolean isGroupAdmin = false;
        if (user.getRole() == UserRole.ROLE_ADMIN && transaction.getUser().getUserGroup() != null) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Long txnGroupAdminId = transaction.getUser().getUserGroup().getAdmin().getId();
                isGroupAdmin = adminOpt.get().getId().equals(txnGroupAdminId);
            }
        }
        
        // Reject if user is not owner, group admin, or owner role
        if (!isOwner && !isGroupAdmin && !isOwnerRole) {
            throw new RuntimeException("Access denied: You don't have permission to delete this transaction");
        }
        
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




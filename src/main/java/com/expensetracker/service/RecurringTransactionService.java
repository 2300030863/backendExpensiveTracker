package com.expensetracker.service;

import com.expensetracker.dto.RecurringTransactionRequest;
import com.expensetracker.entity.*;
import com.expensetracker.repository.AccountRepository;
import com.expensetracker.repository.AdminRepository;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.RecurringTransactionRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RecurringTransactionService {

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private UserGroupRepository userGroupRepository;

    public List<RecurringTransaction> getAllRecurringTransactions(User user) {
        // If user is in a group, show all recurring transactions from the group
        if (user.getUserGroup() != null) {
            return recurringTransactionRepository.findByUserGroupOrderByNextDueDateAsc(
                user.getUserGroup().getId());
        }
        return recurringTransactionRepository.findByUserOrderByNextDueDateAsc(user);
    }

    public List<RecurringTransaction> getActiveRecurringTransactions(User user) {
        // If user is in a group, show all active recurring transactions from the group
        if (user.getUserGroup() != null) {
            return recurringTransactionRepository.findByUserGroupAndIsActiveTrueOrderByNextDueDateAsc(
                user.getUserGroup().getId());
        }
        return recurringTransactionRepository.findByUserAndIsActiveTrueOrderByNextDueDateAsc(user);
    }

    public RecurringTransaction getRecurringTransaction(Long id, User user) {
        return recurringTransactionRepository.findById(id)
                .filter(rt -> {
                    // Allow if user owns the recurring transaction
                    if (rt.getUser().getId().equals(user.getId())) {
                        return true;
                    }
                    // Allow if both users are in the same group
                    if (user.getUserGroup() != null && rt.getUser().getUserGroup() != null) {
                        return user.getUserGroup().getId().equals(rt.getUser().getUserGroup().getId());
                    }
                    return false;
                })
                .orElseThrow(() -> new RuntimeException("Recurring transaction not found"));
    }

    public RecurringTransaction createRecurringTransaction(User user, RecurringTransactionRequest request) {
        // PERMISSION CHECK: Prevent group members from creating recurring transactions
        // Allowed: Users NOT in a group, Group admins, or Owner role
        // Denied: Regular group members
        if (user.getUserGroup() != null && user.getRole() != UserRole.ROLE_OWNER) {
            // User is in a group - check if they are the admin of the group
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                // Check if this admin manages any groups (including the user's group)
                List<UserGroup> adminGroups = userGroupRepository.findByAdmin(admin);
                boolean isGroupAdmin = adminGroups.stream()
                    .anyMatch(group -> group.getId().equals(user.getUserGroup().getId()));
                
                if (!isGroupAdmin) {
                    throw new RuntimeException("Access denied: Group members cannot create recurring transactions. Only group admins can create recurring transactions.");
                }
            } else {
                // User is in a group but is not an admin
                throw new RuntimeException("Access denied: Group members cannot create recurring transactions. Only group admins can create recurring transactions.");
            }
        }
        
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        RecurringTransaction recurringTransaction = new RecurringTransaction();
        recurringTransaction.setDescription(request.getDescription());
        recurringTransaction.setAmount(request.getAmount());
        recurringTransaction.setType(request.getType());
        recurringTransaction.setRecurrenceType(request.getRecurrenceType());
        recurringTransaction.setStartDate(request.getStartDate());
        recurringTransaction.setEndDate(request.getEndDate());
        recurringTransaction.setNotes(request.getNotes());
        recurringTransaction.setUser(user);
        recurringTransaction.setCategory(category);
        recurringTransaction.setAccount(account);
        recurringTransaction.setIsActive(true);

        // Calculate next due date
        recurringTransaction.setNextDueDate(calculateNextDueDate(request.getStartDate(), request.getRecurrenceType()));

        return recurringTransactionRepository.save(recurringTransaction);
    }

    public RecurringTransaction updateRecurringTransaction(Long id, User user, RecurringTransactionRequest request) {
        RecurringTransaction recurringTransaction = getRecurringTransaction(id, user);

        // PERMISSION CHECK: Ensure only authorized users can update recurring transactions
        // Allowed: Transaction owner, Group admin, or Owner role
        // Denied: Regular group members (even if they can view the recurring transaction)
        boolean isOwner = recurringTransaction.getUser().getId().equals(user.getId());
        boolean isOwnerRole = user.getRole() == UserRole.ROLE_OWNER;
        
        // Check if user is the admin of the recurring transaction owner's group
        boolean isGroupAdmin = false;
        if (user.getRole() == UserRole.ROLE_ADMIN && recurringTransaction.getUser().getUserGroup() != null) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Long rtGroupAdminId = recurringTransaction.getUser().getUserGroup().getAdmin().getId();
                isGroupAdmin = adminOpt.get().getId().equals(rtGroupAdminId);
            }
        }
        
        // Reject if user is not owner, group admin, or owner role
        if (!isOwner && !isGroupAdmin && !isOwnerRole) {
            throw new RuntimeException("Access denied: You don't have permission to update this recurring transaction");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        recurringTransaction.setDescription(request.getDescription());
        recurringTransaction.setAmount(request.getAmount());
        recurringTransaction.setType(request.getType());
        recurringTransaction.setRecurrenceType(request.getRecurrenceType());
        recurringTransaction.setStartDate(request.getStartDate());
        recurringTransaction.setEndDate(request.getEndDate());
        recurringTransaction.setNotes(request.getNotes());
        recurringTransaction.setCategory(category);
        recurringTransaction.setAccount(account);

        // Recalculate next due date if recurrence type changed
        if (!recurringTransaction.getRecurrenceType().equals(request.getRecurrenceType())) {
            recurringTransaction.setNextDueDate(calculateNextDueDate(request.getStartDate(), request.getRecurrenceType()));
        }

        return recurringTransactionRepository.save(recurringTransaction);
    }

    public void deleteRecurringTransaction(Long id, User user) {
        RecurringTransaction recurringTransaction = getRecurringTransaction(id, user);
        
        // PERMISSION CHECK: Ensure only authorized users can delete recurring transactions
        // Allowed: Transaction owner, Group admin, or Owner role
        // Denied: Regular group members (even if they can view the recurring transaction)
        boolean isOwner = recurringTransaction.getUser().getId().equals(user.getId());
        boolean isOwnerRole = user.getRole() == UserRole.ROLE_OWNER;
        
        // Check if user is the admin of the recurring transaction owner's group
        boolean isGroupAdmin = false;
        if (user.getRole() == UserRole.ROLE_ADMIN && recurringTransaction.getUser().getUserGroup() != null) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Long rtGroupAdminId = recurringTransaction.getUser().getUserGroup().getAdmin().getId();
                isGroupAdmin = adminOpt.get().getId().equals(rtGroupAdminId);
            }
        }
        
        // Reject if user is not owner, group admin, or owner role
        if (!isOwner && !isGroupAdmin && !isOwnerRole) {
            throw new RuntimeException("Access denied: You don't have permission to delete this recurring transaction");
        }
        
        recurringTransactionRepository.delete(recurringTransaction);
    }

    public RecurringTransaction toggleRecurringTransaction(Long id, User user) {
        RecurringTransaction recurringTransaction = getRecurringTransaction(id, user);
        
        // PERMISSION CHECK: Ensure only authorized users can toggle recurring transactions
        // Allowed: Transaction owner, Group admin, or Owner role
        // Denied: Regular group members (even if they can view the recurring transaction)
        boolean isOwner = recurringTransaction.getUser().getId().equals(user.getId());
        boolean isOwnerRole = user.getRole() == UserRole.ROLE_OWNER;
        
        // Check if user is the admin of the recurring transaction owner's group
        boolean isGroupAdmin = false;
        if (user.getRole() == UserRole.ROLE_ADMIN && recurringTransaction.getUser().getUserGroup() != null) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Long rtGroupAdminId = recurringTransaction.getUser().getUserGroup().getAdmin().getId();
                isGroupAdmin = adminOpt.get().getId().equals(rtGroupAdminId);
            }
        }
        
        // Reject if user is not owner, group admin, or owner role
        if (!isOwner && !isGroupAdmin && !isOwnerRole) {
            throw new RuntimeException("Access denied: You don't have permission to toggle this recurring transaction");
        }
        
        recurringTransaction.setIsActive(!recurringTransaction.getIsActive());
        return recurringTransactionRepository.save(recurringTransaction);
    }

    public void executeRecurringTransaction(Long id, User user) {
        RecurringTransaction recurringTransaction = getRecurringTransaction(id, user);

        if (!recurringTransaction.getIsActive()) {
            throw new RuntimeException("Cannot execute inactive recurring transaction");
        }

        // Create transaction from recurring transaction
        Transaction transaction = new Transaction();
        transaction.setAmount(recurringTransaction.getAmount());
        transaction.setDescription(recurringTransaction.getDescription());
        transaction.setType(recurringTransaction.getType());
        transaction.setTransactionDate(LocalDate.now());
        transaction.setNotes(recurringTransaction.getNotes());
        transaction.setUser(recurringTransaction.getUser()); // Use recurring transaction's owner, not current user
        transaction.setCategory(recurringTransaction.getCategory());
        transaction.setAccount(recurringTransaction.getAccount());
        transaction.setRecurringTransaction(recurringTransaction);

        transactionRepository.save(transaction);

        // Update account balance
        updateAccountBalance(recurringTransaction.getAccount(), recurringTransaction.getAmount(), recurringTransaction.getType());

        // Calculate next due date
        LocalDate nextDueDate = calculateNextDueDate(recurringTransaction.getNextDueDate(), recurringTransaction.getRecurrenceType());
        
        // Check if recurring transaction should end
        if (recurringTransaction.getEndDate() != null && nextDueDate.isAfter(recurringTransaction.getEndDate())) {
            recurringTransaction.setIsActive(false);
        } else {
            recurringTransaction.setNextDueDate(nextDueDate);
        }

        recurringTransactionRepository.save(recurringTransaction);
    }

    private LocalDate calculateNextDueDate(LocalDate currentDate, RecurrenceType recurrenceType) {
        switch (recurrenceType) {
            case DAILY:
                return currentDate.plusDays(1);
            case WEEKLY:
                return currentDate.plusWeeks(1);
            case MONTHLY:
                return currentDate.plusMonths(1);
            case YEARLY:
                return currentDate.plusYears(1);
            default:
                throw new IllegalArgumentException("Unknown recurrence type: " + recurrenceType);
        }
    }

    private void updateAccountBalance(Account account, java.math.BigDecimal amount, TransactionType type) {
        if (type == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            account.setBalance(account.getBalance().subtract(amount));
        }
        accountRepository.save(account);
    }

    public void processDueRecurringTransactions() {
        LocalDate today = LocalDate.now();
        List<User> users = recurringTransactionRepository.findDistinctUsers();

        for (User user : users) {
            List<RecurringTransaction> dueTransactions = recurringTransactionRepository.findDueRecurringTransactions(user, today);
            for (RecurringTransaction recurringTransaction : dueTransactions) {
                try {
                    executeRecurringTransaction(recurringTransaction.getId(), user);
                } catch (Exception e) {
                    // Log error but continue processing other transactions
                    System.err.println("Error processing recurring transaction " + recurringTransaction.getId() + ": " + e.getMessage());
                }
            }
        }
    }
}


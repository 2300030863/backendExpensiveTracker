package com.expensetracker.service;

import com.expensetracker.dto.UserWithGroupDto;
import com.expensetracker.entity.*;
import com.expensetracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailCommunicationService emailCommunicationService;

    /**
     * Get admin entity for the current admin user
     * Auto-creates Admin entity if user has ROLE_ADMIN but no Admin record exists
     */
    public Admin getAdminByUser(User adminUser) {
        if (adminUser.getRole() == UserRole.ROLE_OWNER) {
            // Owner can see all admins - return null to indicate no filtering needed
            return null;
        }
        
        if (adminUser.getRole() != UserRole.ROLE_ADMIN) {
            throw new RuntimeException("User is not an admin");
        }
        
        Optional<Admin> adminOpt = adminRepository.findByEmail(adminUser.getEmail());
        if (!adminOpt.isPresent()) {
            // Check by username as well
            adminOpt = adminRepository.findByUsername(adminUser.getUsername());
            
            if (!adminOpt.isPresent()) {
                // Auto-create Admin entity for this user
                try {
                    Admin admin = new Admin();
                    admin.setUsername(adminUser.getUsername());
                    admin.setEmail(adminUser.getEmail());
                    admin.setPassword(adminUser.getPassword());
                    admin.setFirstName(adminUser.getFirstName());
                    admin.setLastName(adminUser.getLastName());
                    admin.setIsActive(true);
                    admin.setOwner(adminUser); // Set as owner if created from ROLE_ADMIN user
                    return adminRepository.save(admin);
                } catch (Exception e) {
                    // In case of duplicate entry, query again
                    adminOpt = adminRepository.findByUsername(adminUser.getUsername());
                    if (adminOpt.isPresent()) {
                        return adminOpt.get();
                    }
                    throw new RuntimeException("Failed to create or retrieve admin record: " + e.getMessage());
                }
            }
        }
        
        return adminOpt.get();
    }

    /**
     * Get all users belonging to this admin (data isolation)
     */
    public List<User> getUsersForAdmin(User adminUser) {
        if (adminUser.getRole() == UserRole.ROLE_OWNER) {
            // Owner sees all users
            return userRepository.findByRole(UserRole.ROLE_USER);
        }
        
        Admin admin = getAdminByUser(adminUser);
        return userRepository.findByAdmin(admin);
    }

    /**
     * Get all users with user group information (for Owner Dashboard)
     */
    public List<UserWithGroupDto> getUsersWithGroupInfo(User adminUser) {
        List<User> users;
        
        if (adminUser.getRole() == UserRole.ROLE_OWNER) {
            // Owner sees all users and admins (except other owners)
            // Shows ALL admins, but only users who are NOT in user groups
            users = userRepository.findAllExcludingOwnersAndGroupMembers(UserRole.ROLE_OWNER, UserRole.ROLE_ADMIN);
        } else if (adminUser.getRole() == UserRole.ROLE_ADMIN) {
            // Admin sees only their own users
            Admin admin = getAdminByUser(adminUser);
            users = userRepository.findByAdmin(admin);
        } else {
            // Regular users cannot use this endpoint
            throw new RuntimeException("Access denied");
        }
        
        // Convert to DTO with group information
        return users.stream()
            .map(UserWithGroupDto::new)
            .collect(Collectors.toList());
    }

    /**
     * Change user role (User to Admin or Admin to User) - Owner only
     */
    public void changeUserRole(Long userId, String targetRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent changing ROLE_OWNER
        if (user.getRole() == UserRole.ROLE_OWNER) {
            throw new RuntimeException("Cannot change owner role");
        }

        UserRole newRole = UserRole.valueOf(targetRole);
        UserRole oldRole = user.getRole();

        // Check if already has the target role
        if (user.getRole() == newRole) {
            throw new RuntimeException("User already has role: " + targetRole);
        }

        // Simply update the role field
        user.setRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());
        
        // If promoting to ROLE_ADMIN, clear the admin reference (admins are not under other admins)
        if (newRole == UserRole.ROLE_ADMIN) {
            user.setAdmin(null);
            user.setUserGroup(null); // Remove from user group if any
        }
        
        // Save the updated user
        User savedUser = userRepository.save(user);
        
        // Send email notification if promoted to ADMIN
        if (newRole == UserRole.ROLE_ADMIN && oldRole == UserRole.ROLE_USER) {
            try {
                sendRoleChangeNotification(savedUser, "ADMIN");
            } catch (Exception e) {
                // Log error but don't fail the role change
                System.err.println("Failed to send role change email: " + e.getMessage());
            }
        }
    }

    /**
     * Create a new user under this admin
     */
    public User createUserForAdmin(User adminUser, String username, String email, String password, 
                                   String firstName, String lastName) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        Admin admin = getAdminByUser(adminUser);
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(UserRole.ROLE_USER);
        user.setAdmin(admin);
        
        return userRepository.save(user);
    }

    /**
     * Delete a user - only if belongs to this admin
     */
    public void deleteUserForAdmin(User adminUser, Long userId) {
        Optional<User> userOpt = getUserByIdForAdmin(adminUser, userId);
        
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found or access denied");
        }
        
        User user = userOpt.get();
        
        // Prevent deleting admin users or owner
        if (user.getRole() == UserRole.ROLE_ADMIN || user.getRole() == UserRole.ROLE_OWNER) {
            throw new RuntimeException("Cannot delete admin or owner users");
        }
        
        // Delete all related records EXCEPT transactions (keep for audit trail)
        // Order matters - delete from child tables first
        
        // 1. Delete password reset tokens
        passwordResetTokenRepository.deleteByUserId(userId);
        
        // 2. KEEP transactions but null out user reference - preserve for admin portal
        List<Transaction> userTransactions = transactionRepository.findByUserId(userId);
        for (Transaction transaction : userTransactions) {
            transaction.setUser(null); // Null out user but keep transaction for history
            transactionRepository.save(transaction);
        }
        
        // 3. Delete all recurring transactions for this user
        recurringTransactionRepository.deleteByUserId(userId);
        
        // 4. Delete all budgets for this user
        budgetRepository.deleteByUserId(userId);
        
        // 5. Delete all categories created by this user (not default ones)
        categoryRepository.deleteByUserId(userId);
        
        // 6. Delete all accounts for this user
        accountRepository.deleteByUserId(userId);
        
        // 7. Finally, delete the user
        userRepository.deleteById(userId);
    }

    /**
     * Get user by ID - only if belongs to this admin
     */
    public Optional<User> getUserByIdForAdmin(User adminUser, Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Owner can access any user
            if (adminUser.getRole() == UserRole.ROLE_OWNER) {
                return userOpt;
            }
            
            // Admin can only access their own users
            Admin admin = getAdminByUser(adminUser);
            if (user.getAdmin() != null && user.getAdmin().getId().equals(admin.getId())) {
                return userOpt;
            }
        }
        
        return Optional.empty();
    }

    /**
     * Get transactions for admin - includes admin's own transactions AND group member transactions
     */
    public List<Transaction> getTransactionsForAdmin(User adminUser) {
        if (adminUser.getRole() == UserRole.ROLE_OWNER) {
            return transactionRepository.findAll();
        }
        
        Admin admin = getAdminByUser(adminUser);
        
        // Get admin's own transactions
        List<Transaction> adminTransactions = transactionRepository.findByUser(adminUser);
        
        // Get all user groups managed by this admin
        List<UserGroup> userGroups = userGroupRepository.findByAdmin(admin);
        
        // Get all users from these user groups (group members only, not admin)
        List<User> groupUsers = userGroups.stream()
                .flatMap(group -> group.getUsers().stream())
                .filter(user -> user.getRole() == UserRole.ROLE_USER) // Only group members
                .collect(Collectors.toList());
        
        // Get transactions from group members
        List<Transaction> groupTransactions = groupUsers.stream()
                .flatMap(user -> transactionRepository.findByUser(user).stream())
                .collect(Collectors.toList());
        
        // Combine admin's transactions and group member transactions
        List<Transaction> allTransactions = new java.util.ArrayList<>(adminTransactions);
        allTransactions.addAll(groupTransactions);
        
        return allTransactions;
    }

    /**
     * Get transactions for a specific user (only if user belongs to this admin)
     */
    public List<Transaction> getTransactionsForUserByAdmin(User adminUser, Long userId) {
        Optional<User> userOpt = getUserByIdForAdmin(adminUser, userId);
        if (userOpt.isPresent()) {
            return transactionRepository.findByUser(userOpt.get());
        }
        throw new RuntimeException("User not found or access denied");
    }

    /**
     * Approve a transaction (admin can approve user transactions)
     */
    public Transaction approveTransaction(User adminUser, Long transactionId) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        
        if (transactionOpt.isPresent()) {
            Transaction transaction = transactionOpt.get();
            
            // Check if transaction belongs to admin's user
            if (adminUser.getRole() == UserRole.ROLE_OWNER || 
                (transaction.getAdmin() != null && 
                 transaction.getAdmin().getOwner().getId().equals(adminUser.getId()))) {
                
                transaction.setIsApproved(true);
                return transactionRepository.save(transaction);
            }
        }
        
        throw new RuntimeException("Transaction not found or access denied");
    }

    /**
     * Create category for admin
     */
    public Category createCategoryForAdmin(User adminUser, String name, String description, String color) {
        Admin admin = getAdminByUser(adminUser);
        
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setColor(color);
        category.setAdmin(admin);
        category.setIsDefault(false);
        
        return categoryRepository.save(category);
    }

    /**
     * Get categories for admin and their users
     */
    public List<Category> getCategoriesForAdmin(User adminUser) {
        if (adminUser.getRole() == UserRole.ROLE_OWNER) {
            return categoryRepository.findAll();
        }
        
        // For admin, get categories from admin's own account + all group members
        List<Category> categories = new ArrayList<>();
        
        // Add admin's own categories + defaults
        categories.addAll(categoryRepository.findByUserOrIsDefaultTrueOrderByName(adminUser));
        
        // Get categories from all user groups managed by this admin
        Admin admin = getAdminByUser(adminUser);
        List<UserGroup> userGroups = userGroupRepository.findByAdmin(admin);
        
        for (UserGroup group : userGroups) {
            List<Category> groupCategories = categoryRepository.findByUserGroupOrIsDefaultTrue(group.getId());
            for (Category cat : groupCategories) {
                // Add if not already in the list and not a default category (to avoid duplicates)
                if (!cat.getIsDefault() && categories.stream().noneMatch(c -> c.getId().equals(cat.getId()))) {
                    categories.add(cat);
                }
            }
        }
        
        return categories;
    }

    /**
     * Create budget for admin
     */
    public Budget createBudgetForAdmin(User adminUser, Double amount, Long categoryId) {
        Admin admin = getAdminByUser(adminUser);
        
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (!categoryOpt.isPresent()) {
            throw new RuntimeException("Category not found");
        }
        
        Budget budget = new Budget();
        budget.setAmount(BigDecimal.valueOf(amount));
        budget.setCategory(categoryOpt.get());
        budget.setAdmin(admin);
        
        return budgetRepository.save(budget);
    }

    /**
     * Get dashboard statistics for admin
     */
    public java.util.Map<String, Object> getAdminDashboardStats(User adminUser) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        List<User> users = getUsersForAdmin(adminUser);
        List<Transaction> transactions = getTransactionsForAdmin(adminUser);
        
        stats.put("totalUsers", users.size());
        stats.put("totalTransactions", transactions.size());
        stats.put("pendingApprovals", transactions.stream()
                .filter(t -> t.getIsApproved() == null || !t.getIsApproved())
                .count());
        
        double totalIncome = transactions.stream()
                .filter(t -> TransactionType.INCOME.equals(t.getType()))
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();
        
        double totalExpense = transactions.stream()
                .filter(t -> TransactionType.EXPENSE.equals(t.getType()))
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();
        
        stats.put("totalIncome", totalIncome);
        stats.put("totalExpense", totalExpense);
        stats.put("totalCategories", getCategoriesForAdmin(adminUser).size());
        
        return stats;
    }

    // OWNER-ONLY METHODS
    
    /**
     * Create a new admin (OWNER only)
     */
    public User createAdminUser(User ownerUser, String username, String email, String password, 
                               String firstName, String lastName) {
        if (ownerUser.getRole() != UserRole.ROLE_OWNER) {
            throw new RuntimeException("Only OWNER can create admins");
        }
        
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        
        // Create Admin user
        User adminUser = new User();
        adminUser.setUsername(username);
        adminUser.setEmail(email);
        adminUser.setPassword(passwordEncoder.encode(password));
        adminUser.setFirstName(firstName);
        adminUser.setLastName(lastName);
        adminUser.setRole(UserRole.ROLE_ADMIN);
        adminUser = userRepository.save(adminUser);
        
        // Create Admin entity
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setOwner(ownerUser);
        adminRepository.save(admin);
        
        return adminUser;
    }

    /**
     * Get all admins (OWNER only)
     */
    public List<User> getAllAdmins(User ownerUser) {
        if (ownerUser.getRole() != UserRole.ROLE_OWNER) {
            throw new RuntimeException("Only OWNER can view all admins");
        }
        return userRepository.findByRole(UserRole.ROLE_ADMIN);
    }

    /**
     * Delete admin (OWNER only)
     */
    public void deleteAdmin(User ownerUser, Long adminId) {
        if (ownerUser.getRole() != UserRole.ROLE_OWNER) {
            throw new RuntimeException("Only OWNER can delete admins");
        }
        
        Optional<User> adminUserOpt = userRepository.findById(adminId);
        if (adminUserOpt.isPresent() && adminUserOpt.get().getRole() == UserRole.ROLE_ADMIN) {
            // Find and delete Admin entity
            adminRepository.findByEmail(adminUserOpt.get().getEmail())
                    .ifPresent(admin -> adminRepository.delete(admin));
            
            // Delete admin user
            userRepository.deleteById(adminId);
        }
    }
    
    // USER GROUP METHODS
    
    /**
     * Create a new user group for admin
     */
    public UserGroup createUserGroup(User adminUser, String name, String description) {
        // For OWNER, create a default admin entity or allow without admin
        if (adminUser.getRole() == UserRole.ROLE_OWNER) {
            // Owner needs to have an admin entity - create one if not exists
            Optional<Admin> adminOpt = adminRepository.findByEmail(adminUser.getEmail());
            Admin admin;
            if (!adminOpt.isPresent()) {
                admin = new Admin();
                admin.setUsername(adminUser.getUsername());
                admin.setEmail(adminUser.getEmail());
                admin.setPassword(adminUser.getPassword());
                admin.setOwner(adminUser);
                admin = adminRepository.save(admin);
            } else {
                admin = adminOpt.get();
            }
            
            UserGroup userGroup = new UserGroup();
            userGroup.setName(name);
            userGroup.setDescription(description);
            userGroup.setAdmin(admin);
            userGroup = userGroupRepository.save(userGroup);
            
            // Automatically assign the admin user to this group
            adminUser.setUserGroup(userGroup);
            userRepository.save(adminUser);
            
            return userGroup;
        }
        
        // For regular ADMIN
        Admin admin = getAdminByUser(adminUser);
        
        UserGroup userGroup = new UserGroup();
        userGroup.setName(name);
        userGroup.setDescription(description);
        userGroup.setAdmin(admin);
        userGroup = userGroupRepository.save(userGroup);
        
        // Automatically assign the admin user to this group
        adminUser.setUserGroup(userGroup);
        userRepository.save(adminUser);
        
        return userGroup;
    }
    
    /**
     * Get all user groups for admin
     */
    public List<UserGroup> getUserGroupsForAdmin(User adminUser) {
        if (adminUser.getRole() == UserRole.ROLE_OWNER) {
            return userGroupRepository.findAll();
        }
        
        // For ROLE_ADMIN, try to get their admin entity
        Optional<Admin> adminOpt = adminRepository.findByEmail(adminUser.getEmail());
        if (!adminOpt.isPresent()) {
            // Return empty list if no admin entity exists yet
            return new java.util.ArrayList<>();
        }
        
        return userGroupRepository.findByAdmin(adminOpt.get());
    }
    
    /**
     * Assign user to user group
     */
    public User assignUserToGroup(User adminUser, Long userId, Long groupId) {
        // Verify user belongs to admin
        Optional<User> userOpt = getUserByIdForAdmin(adminUser, userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found or access denied");
        }
        
        // Verify group belongs to admin
        Optional<UserGroup> groupOpt = userGroupRepository.findById(groupId);
        if (!groupOpt.isPresent()) {
            throw new RuntimeException("User group not found");
        }
        
        UserGroup group = groupOpt.get();
        Admin admin = getAdminByUser(adminUser);
        
        // Check ownership
        if (adminUser.getRole() != UserRole.ROLE_OWNER && 
            !group.getAdmin().getId().equals(admin.getId())) {
            throw new RuntimeException("Access denied to this user group");
        }
        
        User user = userOpt.get();
        user.setUserGroup(group);
        return userRepository.save(user);
    }
    
    /**
     * Get users in a specific group
     */
    public List<User> getUsersInGroup(User adminUser, Long groupId) {
        Optional<UserGroup> groupOpt = userGroupRepository.findById(groupId);
        if (!groupOpt.isPresent()) {
            throw new RuntimeException("User group not found");
        }
        
        UserGroup group = groupOpt.get();
        Admin admin = getAdminByUser(adminUser);
        
        // Check ownership
        if (adminUser.getRole() != UserRole.ROLE_OWNER && 
            admin != null && !group.getAdmin().getId().equals(admin.getId())) {
            throw new RuntimeException("Access denied to this user group");
        }
        
        // Filter out admin users from the group members list
        return group.getUsers().stream()
                .filter(user -> user.getRole() == UserRole.ROLE_USER)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get accounts for a specific user (Admin/Owner viewing user's accounts)
     */
    public List<Account> getUserAccountsForAdmin(User adminUser, Long userId) {
        // Verify the user exists and belongs to this admin
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        
        // Check access rights
        if (adminUser.getRole() == UserRole.ROLE_OWNER) {
            // Owner can see all user accounts
            return user.getAccounts();
        } else if (adminUser.getRole() == UserRole.ROLE_ADMIN) {
            Admin admin = getAdminByUser(adminUser);
            // Admin can only see accounts of users they manage
            if (user.getAdmin() != null && user.getAdmin().getId().equals(admin.getId())) {
                return user.getAccounts();
            } else if (user.getUserGroup() != null && user.getUserGroup().getAdmin().getId().equals(admin.getId())) {
                // Also allow if user is in a group managed by this admin
                return user.getAccounts();
            } else {
                throw new RuntimeException("Access denied to this user's accounts");
            }
        }
        
        throw new RuntimeException("Insufficient permissions");
    }
    
    /**
     * Promote user to admin (create admin from existing user)
     */
    public User promoteUserToAdmin(User adminUser, Long userId) {
        if (adminUser.getRole() != UserRole.ROLE_OWNER) {
            throw new RuntimeException("Only OWNER can promote users to admin");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        
        if (user.getRole() == UserRole.ROLE_ADMIN || user.getRole() == UserRole.ROLE_OWNER) {
            throw new RuntimeException("User is already an admin or owner");
        }
        
        // Update role to ADMIN
        user.setRole(UserRole.ROLE_ADMIN);
        user.setAdmin(null); // Admins don't belong to other admins
        user.setUserGroup(null); // Remove from user group
        userRepository.save(user);
        
        // Create Admin entity
        Admin newAdmin = new Admin();
        newAdmin.setUsername(user.getUsername());
        newAdmin.setEmail(user.getEmail());
        newAdmin.setPassword(user.getPassword()); // Already encoded
        newAdmin.setOwner(adminUser);
        adminRepository.save(newAdmin);
        
        return user;
    }
    
    /**
     * Block a user - prevents login
     */
    public void blockUser(User adminUser, Long userId) {
        Optional<User> userOpt = getUserByIdForAdmin(adminUser, userId);
        
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found or access denied");
        }
        
        User user = userOpt.get();
        
        // Cannot block admins or owners
        if (user.getRole() == UserRole.ROLE_ADMIN || user.getRole() == UserRole.ROLE_OWNER) {
            throw new RuntimeException("Cannot block admin or owner users");
        }
        
        if (user.isBlocked()) {
            throw new RuntimeException("User is already blocked");
        }
        
        user.setBlocked(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    /**
     * Unblock a user - allows login
     */
    public void unblockUser(User adminUser, Long userId) {
        Optional<User> userOpt = getUserByIdForAdmin(adminUser, userId);
        
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found or access denied");
        }
        
        User user = userOpt.get();
        
        if (!user.isBlocked()) {
            throw new RuntimeException("User is not blocked");
        }
        
        user.setBlocked(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    /**
     * Send email notification when user role is changed to ADMIN
     */
    private void sendRoleChangeNotification(User user, String newRole) {
        try {
            String subject = "Your Role Has Been Updated - Expense Tracker";
            String message = String.format(
                "Congratulations! Your account has been upgraded to %s role.\n\n" +
                "You now have access to additional features and privileges in the Expense Tracker system.\n\n" +
                "Please log in to your account to explore your new capabilities.\n\n" +
                "If you have any questions, please contact the system administrator.",
                newRole
            );
            
            emailCommunicationService.sendEmailToUsers(
                List.of(user.getId()),
                subject,
                message,
                "System Owner"
            );
        } catch (Exception e) {
            // Log but don't throw - email failure shouldn't fail the role change
            System.err.println("Failed to send role change notification: " + e.getMessage());
        }
    }
}

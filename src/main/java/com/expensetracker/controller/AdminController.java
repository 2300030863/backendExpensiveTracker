package com.expensetracker.controller;

import com.expensetracker.dto.AdminUserRequest;
import com.expensetracker.dto.CategoryDto;
import com.expensetracker.dto.EmailRequest;
import com.expensetracker.dto.UserWithGroupDto;
import com.expensetracker.entity.*;
import com.expensetracker.service.AdminService;
import com.expensetracker.service.EmailCommunicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
public class AdminController {

    @Autowired
    private AdminService adminService;
    
    @Autowired
    private EmailCommunicationService emailCommunicationService;

    /**
     * Get dashboard statistics
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats(@AuthenticationPrincipal User adminUser) {
        try {
            Map<String, Object> stats = adminService.getAdminDashboardStats(adminUser);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all users belonging to this admin (with data isolation)
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@AuthenticationPrincipal User adminUser) {
        try {
            List<User> users = adminService.getUsersForAdmin(adminUser);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all users with user group information (for Owner/Admin Dashboard)
     */
    @GetMapping("/users-with-groups")
    public ResponseEntity<?> getAllUsersWithGroups(@AuthenticationPrincipal User adminUser) {
        try {
            List<UserWithGroupDto> users = adminService.getUsersWithGroupInfo(adminUser);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Change user role (User to Admin or Admin to User) - Owner only
     */
    @PutMapping("/users/{id}/change-role")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<?> changeUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User ownerUser) {
        try {
            String targetRole = request.get("role");
            if (!"ROLE_USER".equals(targetRole) && !"ROLE_ADMIN".equals(targetRole)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid role. Must be ROLE_USER or ROLE_ADMIN"));
            }
            
            adminService.changeUserRole(id, targetRole);
            return ResponseEntity.ok(Map.of("message", "Role changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user by ID (only if belongs to this admin)
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id, @AuthenticationPrincipal User adminUser) {
        try {
            return adminService.getUserByIdForAdmin(adminUser, id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create a new user under this admin
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody @Valid AdminUserRequest request,
                                       @AuthenticationPrincipal User adminUser) {
        try {
            User user = adminService.createUserForAdmin(
                    adminUser,
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a user (only if belongs to this admin)
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId,
                                       @AuthenticationPrincipal User adminUser) {
        try {
            adminService.deleteUserForAdmin(adminUser, userId);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all transactions for users under this admin
     */
    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions(@AuthenticationPrincipal User adminUser) {
        try {
            List<Transaction> transactions = adminService.getTransactionsForAdmin(adminUser);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get transactions for a specific user (only if user belongs to this admin)
     */
    @GetMapping("/users/{userId}/transactions")
    public ResponseEntity<?> getUserTransactions(@PathVariable Long userId,
                                                 @AuthenticationPrincipal User adminUser) {
        try {
            List<Transaction> transactions = adminService.getTransactionsForUserByAdmin(adminUser, userId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Approve a transaction
     */
    @PatchMapping("/transactions/{transactionId}/approve")
    public ResponseEntity<?> approveTransaction(@PathVariable Long transactionId,
                                                @AuthenticationPrincipal User adminUser) {
        try {
            Transaction transaction = adminService.approveTransaction(adminUser, transactionId);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all categories for this admin
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories(@AuthenticationPrincipal User adminUser) {
        try {
            List<Category> categories = adminService.getCategoriesForAdmin(adminUser);
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create a category for this admin
     */
    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody @Valid CategoryDto categoryDto,
                                           @AuthenticationPrincipal User adminUser) {
        try {
            Category category = adminService.createCategoryForAdmin(
                    adminUser,
                    categoryDto.getName(),
                    categoryDto.getDescription(),
                    categoryDto.getColor()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(category);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create a budget for this admin
     */
    @PostMapping("/budgets")
    public ResponseEntity<?> createBudget(@RequestBody Map<String, Object> request,
                                         @AuthenticationPrincipal User adminUser) {
        try {
            Double amount = Double.valueOf(request.get("amount").toString());
            Long categoryId = Long.valueOf(request.get("categoryId").toString());
            
            Budget budget = adminService.createBudgetForAdmin(adminUser, amount, categoryId);
            return ResponseEntity.status(HttpStatus.CREATED).body(budget);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // OWNER-ONLY ENDPOINTS

    /**
     * Create a new admin (OWNER only)
     */
    @PostMapping("/create-admin")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<?> createAdmin(@RequestBody @Valid AdminUserRequest request,
                                        @AuthenticationPrincipal User ownerUser) {
        try {
            User admin = adminService.createAdminUser(
                    ownerUser,
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(admin);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all admins (OWNER only)
     */
    @GetMapping("/admins")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<?> getAllAdmins(@AuthenticationPrincipal User ownerUser) {
        try {
            List<User> admins = adminService.getAllAdmins(ownerUser);
            return ResponseEntity.ok(admins);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete an admin (OWNER only)
     */
    @DeleteMapping("/admins/{adminId}")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<?> deleteAdmin(@PathVariable Long adminId,
                                        @AuthenticationPrincipal User ownerUser) {
        try {
            adminService.deleteAdmin(ownerUser, adminId);
            return ResponseEntity.ok(Map.of("message", "Admin deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    // USER GROUP ENDPOINTS
    
    /**
     * Create a new user group
     */
    @PostMapping("/user-groups")
    public ResponseEntity<?> createUserGroup(@RequestBody Map<String, String> request,
                                            @AuthenticationPrincipal User adminUser) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            UserGroup userGroup = adminService.createUserGroup(adminUser, name, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(userGroup);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get all user groups for admin
     */
    @GetMapping("/user-groups")
    public ResponseEntity<?> getUserGroups(@AuthenticationPrincipal User adminUser) {
        try {
            List<UserGroup> groups = adminService.getUserGroupsForAdmin(adminUser);
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Assign user to group
     */
    @PutMapping("/users/{userId}/group/{groupId}")
    public ResponseEntity<?> assignUserToGroup(@PathVariable Long userId,
                                               @PathVariable Long groupId,
                                               @AuthenticationPrincipal User adminUser) {
        try {
            User user = adminService.assignUserToGroup(adminUser, userId, groupId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get users in a group
     */
    @GetMapping("/user-groups/{groupId}/users")
    public ResponseEntity<?> getUsersInGroup(@PathVariable Long groupId,
                                            @AuthenticationPrincipal User adminUser) {
        try {
            List<User> users = adminService.getUsersInGroup(adminUser, groupId);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get accounts for a specific user (Admin/Owner can view user's accounts)
     */
    @GetMapping("/users/{userId}/accounts")
    public ResponseEntity<?> getUserAccounts(@PathVariable Long userId,
                                             @AuthenticationPrincipal User adminUser) {
        try {
            List<Account> accounts = adminService.getUserAccountsForAdmin(adminUser, userId);
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Promote user to admin (OWNER only)
     */
    @PostMapping("/users/{userId}/promote")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<?> promoteUserToAdmin(@PathVariable Long userId,
                                               @AuthenticationPrincipal User ownerUser) {
        try {
            User user = adminService.promoteUserToAdmin(ownerUser, userId);
            return ResponseEntity.ok(Map.of(
                "message", "User promoted to admin successfully",
                "user", user
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Send email to selected users (Owner only) - sends to registered emails
     */
    @PostMapping("/send-email")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<?> sendEmailToUsers(@Valid @RequestBody EmailRequest emailRequest,
                                              @AuthenticationPrincipal User ownerUser) {
        try {
            String senderName = ownerUser.getFirstName() + " " + ownerUser.getLastName();
            int sentCount = emailCommunicationService.sendEmailToUsers(
                emailRequest.getRecipientIds(),
                emailRequest.getSubject(),
                emailRequest.getMessage(),
                senderName
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Emails sent successfully to registered email addresses",
                "sentCount", sentCount,
                "totalRequested", emailRequest.getRecipientIds().size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send emails: " + e.getMessage()));
        }
    }
    
    /**
     * Send email to all users or all admins (Owner only)
     */
    @PostMapping("/send-email-to-all")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<?> sendEmailToAllByRole(@RequestParam String role,
                                                   @RequestParam String subject,
                                                   @RequestParam String message,
                                                   @AuthenticationPrincipal User ownerUser) {
        try {
            if (!"ROLE_USER".equals(role) && !"ROLE_ADMIN".equals(role)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid role. Must be ROLE_USER or ROLE_ADMIN"));
            }
            
            String senderName = ownerUser.getFirstName() + " " + ownerUser.getLastName();
            int sentCount = emailCommunicationService.sendEmailToAllByRole(role, subject, message, senderName);
            
            return ResponseEntity.ok(Map.of(
                "message", "Emails sent successfully to all " + (role.equals("ROLE_ADMIN") ? "admins" : "users"),
                "sentCount", sentCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send emails: " + e.getMessage()));
        }
    }
    
    /**
     * Block a user - prevents login (only for ROLE_USER group members)
     */
    @PostMapping("/users/{userId}/block")
    public ResponseEntity<?> blockUser(@PathVariable Long userId,
                                      @AuthenticationPrincipal User adminUser) {
        try {
            adminService.blockUser(adminUser, userId);
            return ResponseEntity.ok(Map.of("message", "User blocked successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Unblock a user - allows login
     */
    @PostMapping("/users/{userId}/unblock")
    public ResponseEntity<?> unblockUser(@PathVariable Long userId,
                                        @AuthenticationPrincipal User adminUser) {
        try {
            adminService.unblockUser(adminUser, userId);
            return ResponseEntity.ok(Map.of("message", "User unblocked successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

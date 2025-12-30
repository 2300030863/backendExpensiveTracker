package com.expensetracker.service;

import com.expensetracker.entity.Account;
import com.expensetracker.entity.Admin;
import com.expensetracker.entity.User;
import com.expensetracker.entity.UserGroup;
import com.expensetracker.entity.UserRole;
import com.expensetracker.repository.AccountRepository;
import com.expensetracker.repository.AdminRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserGroupRepository userGroupRepository;

    public List<Account> getAllAccounts(User user) {
        // Reload user with userGroup to handle lazy loading
        User fullUser = userRepository.findByIdWithUserGroup(user.getId())
                .orElse(user);
        
        // If user is admin, show all accounts from users they manage (including admin's own)
        if (fullUser.getRole() == UserRole.ROLE_ADMIN) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(fullUser.getUsername());
            if (!adminOpt.isPresent()) {
                adminOpt = adminRepository.findByEmail(fullUser.getEmail());
            }
            
            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                
                // Get admin's own accounts
                List<Account> adminAccounts = accountRepository.findByUserAndIsActiveTrueOrderByName(fullUser);
                
                // Get accounts from users directly managed by this admin
                List<User> directUsers = userRepository.findByAdmin(admin);
                List<Account> directUserAccounts = directUsers.stream()
                        .flatMap(u -> accountRepository.findByUserAndIsActiveTrueOrderByName(u).stream())
                        .collect(Collectors.toList());
                
                // Get accounts from users in groups managed by this admin
                List<UserGroup> userGroups = userGroupRepository.findByAdmin(admin);
                List<Account> groupUserAccounts = userGroups.stream()
                        .flatMap(group -> group.getUsers().stream())
                        .flatMap(u -> accountRepository.findByUserAndIsActiveTrueOrderByName(u).stream())
                        .collect(Collectors.toList());
                
                // Combine all accounts and remove duplicates by ID
                List<Account> allAccounts = new ArrayList<>();
                allAccounts.addAll(adminAccounts);
                allAccounts.addAll(directUserAccounts);
                allAccounts.addAll(groupUserAccounts);
                
                // Remove duplicates by ID using a Map
                java.util.Map<Long, Account> uniqueAccountsMap = new java.util.LinkedHashMap<>();
                for (Account account : allAccounts) {
                    uniqueAccountsMap.put(account.getId(), account);
                }
                
                return new ArrayList<>(uniqueAccountsMap.values());
            }
        }
        
        // If user is in a group, show all accounts from the group (including admin's)
        UserGroup userGroup = fullUser.getUserGroup();
        if (userGroup != null) {
            return accountRepository.findByUserGroupIncludingAdminAndIsActiveTrueOrderByName(userGroup.getId());
        }
        
        // Default: user's own accounts
        return accountRepository.findByUserAndIsActiveTrueOrderByName(fullUser);
    }

    public Account getAccount(User user, Long id) {
        return accountRepository.findById(id)
                .filter(account -> {
                    // Allow if user owns the account
                    if (account.getUser().getId().equals(user.getId())) {
                        return true;
                    }
                    // Allow if admin manages the account's user
                    if (user.getRole() == UserRole.ROLE_ADMIN) {
                        Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
                        if (adminOpt.isPresent()) {
                            Admin admin = adminOpt.get();
                            User accountUser = account.getUser();
                            if ((accountUser.getAdmin() != null && accountUser.getAdmin().getId().equals(admin.getId())) ||
                                (accountUser.getUserGroup() != null && accountUser.getUserGroup().getAdmin().getId().equals(admin.getId()))) {
                                return true;
                            }
                        }
                    }
                    // Allow if both users are in the same group
                    if (user.getUserGroup() != null && account.getUser().getUserGroup() != null) {
                        return user.getUserGroup().getId().equals(account.getUser().getUserGroup().getId());
                    }
                    return false;
                })
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public Account createAccount(User user, Account account) {
        // PERMISSION CHECK: Prevent group members from creating accounts
        // Allowed: Users NOT in a group, Group admins, ROLE_ADMIN, or ROLE_OWNER
        // Denied: Regular group members
        
        // Allow ROLE_ADMIN and ROLE_OWNER to create accounts without restrictions
        if (user.getRole() == UserRole.ROLE_ADMIN || user.getRole() == UserRole.ROLE_OWNER) {
            account.setUser(user);
            return accountRepository.save(account);
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
                    throw new RuntimeException("Access denied: Group members cannot create accounts. Only group admins can create accounts.");
                }
            } else {
                // User is in a group but is not an admin
                throw new RuntimeException("Access denied: Group members cannot create accounts. Only group admins can create accounts.");
            }
        }
        
        account.setUser(user);
        return accountRepository.save(account);
    }

    public Account updateAccount(User user, Long id, Account accountDetails) {
        Account account = getAccount(user, id);
        
        // PERMISSION CHECK: Ensure only authorized users can update accounts
        // Allowed: Account owner, Group admin, or Owner role
        // Denied: Regular group members (even if they can view the account)
        boolean isOwner = account.getUser().getId().equals(user.getId());
        boolean isOwnerRole = user.getRole() == UserRole.ROLE_OWNER;
        
        // Check if user is the admin of the account owner's group
        boolean isGroupAdmin = false;
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                User accountUser = account.getUser();
                isGroupAdmin = (accountUser.getAdmin() != null && accountUser.getAdmin().getId().equals(admin.getId())) ||
                               (accountUser.getUserGroup() != null && accountUser.getUserGroup().getAdmin().getId().equals(admin.getId()));
            }
        }
        
        // Reject if user is not owner, group admin, or owner role
        if (!isOwner && !isGroupAdmin && !isOwnerRole) {
            throw new RuntimeException("Access denied: You don't have permission to update this account");
        }
        
        account.setName(accountDetails.getName());
        account.setDescription(accountDetails.getDescription());
        account.setType(accountDetails.getType());
        account.setIsActive(accountDetails.getIsActive());
        account.setBalance(accountDetails.getBalance());
        
        return accountRepository.save(account);
    }

    public void deleteAccount(User user, Long id) {
        Account account = getAccount(user, id);
        
        // PERMISSION CHECK: Ensure only authorized users can delete accounts
        // Allowed: Account owner, Group admin, or Owner role
        // Denied: Regular group members (even if they can view the account)
        boolean isOwner = account.getUser().getId().equals(user.getId());
        boolean isOwnerRole = user.getRole() == UserRole.ROLE_OWNER;
        
        // Check if user is the admin of the account owner's group
        boolean isGroupAdmin = false;
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                User accountUser = account.getUser();
                isGroupAdmin = (accountUser.getAdmin() != null && accountUser.getAdmin().getId().equals(admin.getId())) ||
                               (accountUser.getUserGroup() != null && accountUser.getUserGroup().getAdmin().getId().equals(admin.getId()));
            }
        }
        
        // Reject if user is not owner, group admin, or owner role
        if (!isOwner && !isGroupAdmin && !isOwnerRole) {
            throw new RuntimeException("Access denied: You don't have permission to delete this account");
        }
        
        account.setIsActive(false);
        accountRepository.save(account);
    }
}




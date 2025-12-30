package com.expensetracker.service;

import com.expensetracker.entity.Admin;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.User;
import com.expensetracker.entity.UserGroup;
import com.expensetracker.entity.UserRole;
import com.expensetracker.repository.AdminRepository;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.UserGroupRepository;
import com.expensetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private UserGroupRepository userGroupRepository;
    
    @Autowired
    private UserRepository userRepository;

    public List<Category> getAllCategories(User user) {
        try {
            List<Category> categories = new ArrayList<>();
            
            // Always include default categories
            List<Category> defaultCategories = categoryRepository.findAllDefaultCategories();
            categories.addAll(defaultCategories);
            
            // Add user's own categories
            List<Category> ownCategories = categoryRepository.findByUserId(user.getId());
            categories.addAll(ownCategories);
            
            // If user is Admin, include categories from managed users
            if (user.getRole() == UserRole.ROLE_ADMIN) {
                Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
                if (adminOpt.isPresent()) {
                    Admin admin = adminOpt.get();
                    
                    // Get categories from all managed users
                    List<Category> managedUserCategories = categoryRepository.findByAdminManagedUsers(admin.getId());
                    categories.addAll(managedUserCategories);
                }
            }
            
            // If user is in a group, include admin's categories and other group members' categories
            if (user.getUserGroup() != null) {
                Long groupId = user.getUserGroup().getId();
                Admin groupAdmin = user.getUserGroup().getAdmin();
                
                // Add admin's categories
                if (groupAdmin != null) {
                    Optional<User> adminUserOpt = userRepository.findByUsername(groupAdmin.getUsername());
                    if (adminUserOpt.isPresent()) {
                        List<Category> adminCategories = categoryRepository.findByUserId(adminUserOpt.get().getId());
                        categories.addAll(adminCategories);
                    }
                }
                
                // Add categories from group members
                List<Category> groupCategories = categoryRepository.findByGroupMembers(groupId);
                categories.addAll(groupCategories);
            }
            
            // Remove duplicates and sort by name
            return categories.stream()
                .distinct()
                .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            // Fallback to user's own categories in case of any error
            return categoryRepository.findByUserOrIsDefaultTrueOrderByName(user);
        }
    }

    public Category getCategory(User user, Long id) {
        return categoryRepository.findById(id)
                .filter(category -> {
                    // Allow default categories (isDefault = true)
                    if (category.getIsDefault() != null && category.getIsDefault()) return true;
                    // Allow default categories (user is null)
                    if (category.getUser() == null) return true;
                    // Allow if user owns the category
                    if (category.getUser().getId().equals(user.getId())) return true;
                    
                    // If user is admin, allow access to categories from their group members
                    if (user.getRole() == UserRole.ROLE_ADMIN && category.getUser().getUserGroup() != null) {
                        Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
                        if (adminOpt.isPresent()) {
                            Admin admin = adminOpt.get();
                            // Check if the category owner's group is managed by this admin
                            if (category.getUser().getUserGroup().getAdmin().getId().equals(admin.getId())) {
                                return true;
                            }
                        }
                    }
                    
                    // If user is in a group, allow access to categories from admin and other group members
                    if (user.getUserGroup() != null) {
                        // Check if category owner is the group admin
                        Admin groupAdmin = user.getUserGroup().getAdmin();
                        if (groupAdmin != null && category.getUser().getUsername().equals(groupAdmin.getUsername())) {
                            return true;
                        }
                        
                        // Check if both users are in the same group
                        if (category.getUser().getUserGroup() != null) {
                            return user.getUserGroup().getId().equals(category.getUser().getUserGroup().getId());
                        }
                    }
                    
                    return false;
                })
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    public Category createCategory(User user, Category category) {
        if (categoryRepository.existsByNameAndUser(category.getName(), user)) {
            throw new RuntimeException("Category with this name already exists");
        }
        
        category.setUser(user);
        category.setIsDefault(false);
        return categoryRepository.save(category);
    }

    public Category updateCategory(User user, Long id, Category categoryDetails) {
        Category category = getCategory(user, id);
        
        if (category.getIsDefault()) {
            throw new RuntimeException("Cannot modify default categories");
        }
        
        // PERMISSION CHECK: Ensure only authorized users can update categories
        // Allowed: Category owner, Group admin, or Owner role
        // Denied: Regular group members (even if they can view the category)
        boolean isOwner = category.getUser() != null && category.getUser().getId().equals(user.getId());
        boolean isOwnerRole = user.getRole() == UserRole.ROLE_OWNER;
        
        // Check if user is the admin of the category owner's group
        boolean isGroupAdmin = false;
        if (user.getRole() == UserRole.ROLE_ADMIN && category.getUser() != null && category.getUser().getUserGroup() != null) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Long categoryGroupAdminId = category.getUser().getUserGroup().getAdmin().getId();
                isGroupAdmin = adminOpt.get().getId().equals(categoryGroupAdminId);
            }
        }
        
        // Reject if user is not owner, group admin, or owner role
        if (!isOwner && !isGroupAdmin && !isOwnerRole) {
            throw new RuntimeException("Access denied: You don't have permission to update this category");
        }
        
        if (!category.getName().equals(categoryDetails.getName()) && 
            categoryRepository.existsByNameAndUser(categoryDetails.getName(), user)) {
            throw new RuntimeException("Category with this name already exists");
        }
        
        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        category.setColor(categoryDetails.getColor());
        
        return categoryRepository.save(category);
    }

    public void deleteCategory(User user, Long id) {
        Category category = getCategory(user, id);
        
        if (category.getIsDefault()) {
            throw new RuntimeException("Cannot delete default categories");
        }
        
        // PERMISSION CHECK: Ensure only authorized users can delete categories
        // Allowed: Category owner, Group admin, or Owner role
        // Denied: Regular group members (even if they can view the category)
        boolean isOwner = category.getUser() != null && category.getUser().getId().equals(user.getId());
        boolean isOwnerRole = user.getRole() == UserRole.ROLE_OWNER;
        
        // Check if user is the admin of the category owner's group
        boolean isGroupAdmin = false;
        if (user.getRole() == UserRole.ROLE_ADMIN && category.getUser() != null && category.getUser().getUserGroup() != null) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                Long categoryGroupAdminId = category.getUser().getUserGroup().getAdmin().getId();
                isGroupAdmin = adminOpt.get().getId().equals(categoryGroupAdminId);
            }
        }
        
        // Reject if user is not owner, group admin, or owner role
        if (!isOwner && !isGroupAdmin && !isOwnerRole) {
            throw new RuntimeException("Access denied: You don't have permission to delete this category");
        }
        
        categoryRepository.delete(category);
    }
}




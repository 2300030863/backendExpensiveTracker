package com.expensetracker.service;

import com.expensetracker.entity.Category;
import com.expensetracker.entity.User;
import com.expensetracker.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories(User user) {
        return categoryRepository.findByUserOrIsDefaultTrueOrderByName(user);
    }

    public Category getCategory(User user, Long id) {
        return categoryRepository.findById(id)
                .filter(category -> category.getUser() == null || category.getUser().getId().equals(user.getId()))
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
        
        categoryRepository.delete(category);
    }
}




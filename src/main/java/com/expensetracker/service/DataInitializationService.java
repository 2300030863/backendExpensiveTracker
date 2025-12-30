package com.expensetracker.service;

import com.expensetracker.entity.Category;
import com.expensetracker.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultCategories();
    }

    private void initializeDefaultCategories() {
        if (categoryRepository.count() == 0) {
            List<Category> defaultCategories = Arrays.asList(
                createDefaultCategory("Food & Dining", "Restaurants, groceries, and food expenses", "#FF6B6B"),
                createDefaultCategory("Transportation", "Gas, public transport, and vehicle expenses", "#4ECDC4"),
                createDefaultCategory("Shopping", "Clothing, electronics, and general shopping", "#45B7D1"),
                createDefaultCategory("Entertainment", "Movies, games, and leisure activities", "#96CEB4"),
                createDefaultCategory("Bills & Utilities", "Electricity, water, internet, and phone bills", "#FFEAA7"),
                createDefaultCategory("Healthcare", "Medical expenses, pharmacy, and health services", "#DDA0DD"),
                createDefaultCategory("Travel", "Hotels, flights, and travel expenses", "#98D8C8"),
                createDefaultCategory("Education", "Books, courses, and educational expenses", "#F7DC6F"),
                createDefaultCategory("Personal Care", "Haircuts, cosmetics, and personal hygiene", "#BB8FCE"),
                createDefaultCategory("Income", "Salary, freelance, and other income sources", "#85C1E9"),
                createDefaultCategory("Other", "Miscellaneous expenses and income", "#F8C471")
            );

            categoryRepository.saveAll(defaultCategories);
        }
    }

    private Category createDefaultCategory(String name, String description, String color) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setColor(color);
        category.setIsDefault(true);
        category.setUser(null);
        return category;
    }
}




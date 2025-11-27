package com.expensetracker.controller;

import com.expensetracker.dto.CategoryDto;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.User;
import com.expensetracker.service.CategoryService;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories(@AuthenticationPrincipal User user) {
        List<CategoryDto> dtos = categoryService.getAllCategories(user).stream()
                .map(c -> new CategoryDto(c.getId(), c.getName(), c.getDescription(), c.getColor(), c.getIsDefault()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategory(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Category c = categoryService.getCategory(user, id);
        return ResponseEntity.ok(new CategoryDto(c.getId(), c.getName(), c.getDescription(), c.getColor(), c.getIsDefault()));
    }

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@AuthenticationPrincipal User user, @Valid @RequestBody Category category) {
        Category c = categoryService.createCategory(user, category);
        return ResponseEntity.ok(new CategoryDto(c.getId(), c.getName(), c.getDescription(), c.getColor(), c.getIsDefault()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody Category category) {
        Category c = categoryService.updateCategory(user, id, category);
        return ResponseEntity.ok(new CategoryDto(c.getId(), c.getName(), c.getDescription(), c.getColor(), c.getIsDefault()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@AuthenticationPrincipal User user, @PathVariable Long id) {
        categoryService.deleteCategory(user, id);
        return ResponseEntity.ok().build();
    }
}




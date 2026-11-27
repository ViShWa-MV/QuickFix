package com.example.catalog.controller;

import com.example.catalog.dto.CategoryRequest;
import com.example.catalog.entity.Category;
import com.example.catalog.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Categories are public to read; only admins change them. */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Category createCategory(@Valid @RequestBody CategoryRequest request) {
        return categoryService.createCategory(toEntity(request));
    }

    @GetMapping
    public List<Category> getAllCategories(@PageableDefault(size = 50) Pageable pageable) {
        return categoryService.getAllCategories(pageable);
    }

    @GetMapping("/{id}")
    public Category getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Category updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return categoryService.updateCategory(id, toEntity(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return "Category deleted successfully";
    }

    private static Category toEntity(CategoryRequest r) {
        return Category.builder()
                .name(r.getName())
                .description(r.getDescription())
                .build();
    }
}

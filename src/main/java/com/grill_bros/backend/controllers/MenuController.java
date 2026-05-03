package com.grill_bros.backend.controllers;

import com.grill_bros.backend.common.ApiResponse;
import com.grill_bros.backend.common.PagedResponse;
import com.grill_bros.backend.dto.AdminUserResponse;
import com.grill_bros.backend.dto.menudtos.*;
import com.grill_bros.backend.service.menuservice.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/menu")
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/categories")
    @Operation(summary = "List all active categories (paginated)")
    public ResponseEntity<ApiResponse> listCategories() {
        List<CategoryResponse> categories = menuService.getActiveCategories();

        return ResponseEntity.ok(
                ApiResponse.ok(categories, "Menu categories fetched successfully"));
    }

    @GetMapping("/admin/categories")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Admin: List all categories (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<CategoryResponse>>> listCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<CategoryResponse> result = menuService.adminListCategories(pageable);

        return ResponseEntity.ok(
                ApiResponse.ok(PagedResponse.of(result))
        );
    }

    @GetMapping("/admin/items")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Admin: List menu items with optional filters")
    public ResponseEntity<ApiResponse<PagedResponse<MenuItemResponse>>> listItems(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean available,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<MenuItemResponse> result =
                menuService.adminListItems(categoryId, available, pageable);

        return ResponseEntity.ok(
                ApiResponse.ok(PagedResponse.of(result))
        );
    }

    @GetMapping("/items")
    @Operation(summary = "List all menu items")
    public ResponseEntity<ApiResponse<PagedResponse<MenuItemResponse>>> getAllMenuItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, 100);
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.ok(PagedResponse.of(menuService.getAvailableItems(pageable))));
    }

    @GetMapping("/items/{id}")
    @Operation(summary = "List menu item with this id")
    public ResponseEntity<ApiResponse> getMenuItemById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getItemById(id)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search menu items")
    public ResponseEntity<ApiResponse<PagedResponse<MenuItemResponse>>> searchItems(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<MenuItemResponse> result = menuService.searchItems(query, pageable);

        return ResponseEntity.ok(
                ApiResponse.ok(PagedResponse.of(result))
        );
    }

    @GetMapping("/categories/{slug}/items")
    @Operation(summary = "Get menu items by category slug")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getItemsByCategory(
            @PathVariable String slug
    ) {
        List<MenuItemResponse> items = menuService.getItemsByCategory(slug);

        return ResponseEntity.ok(
                ApiResponse.ok(items, "Menu items fetched successfully for " + slug)
        );
    }

    @PostMapping("/categories")
    @Operation(summary = "Create a new menu category")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest req
    ) {
        CategoryResponse response = menuService.createCategory(req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update a menu category")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest req
    ) {
        CategoryResponse response = menuService.updateCategory(id, req);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete (deactivate) a menu category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable UUID id
    ) {
        menuService.deleteCategory(id);

        return ResponseEntity.ok(
                ApiResponse.ok(null, "Category deleted successfully")
        );
    }

    @PostMapping("/items")
    @Operation(summary = "Create a new menu item")
    public ResponseEntity<ApiResponse<MenuItemResponse>> createItem(
            @Valid @RequestBody CreateMenuItemRequest req
    ) {
        MenuItemResponse response = menuService.createItem(req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @PutMapping("/items/{id}")
    @Operation(summary = "Update a menu item")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMenuItemRequest req
    ) {
        MenuItemResponse response = menuService.updateItem(id, req);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/items/{id}/availability")
    @Operation(summary = "Toggle menu item availability")
    public ResponseEntity<ApiResponse<MenuItemResponse>> toggleAvailability(
            @PathVariable UUID id,
            @RequestParam boolean available
    ) {
        MenuItemResponse response = menuService.toggleAvailability(id, available);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Delete (soft delete) a menu item")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable UUID id
    ) {
        menuService.deleteItem(id);

        return ResponseEntity.ok(
                ApiResponse.ok(null, "Menu item deleted successfully")
        );
    }

}

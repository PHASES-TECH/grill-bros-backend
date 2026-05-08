package com.grill_bros.backend.service.menuservice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.grill_bros.backend.cache.RedisKeys;
import com.grill_bros.backend.dto.menudtos.*;
import com.grill_bros.backend.exceptions.DuplicateResourceException;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.MenuCategory;
import com.grill_bros.backend.model.MenuItem;
import com.grill_bros.backend.repository.MenuCategoryRepository;
import com.grill_bros.backend.repository.MenuItemRepository;
import com.grill_bros.backend.service.cacheservice.CacheService;
import com.grill_bros.backend.service.utilsservice.ImageUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final CacheService cache;
    private final ImageUploadService imageUploadService;

    public List<MenuItemResponse> getMenuItems() {
        return itemRepository.findAll()
                .stream()
                .map(MenuItemResponse::from)
                .toList();
    }
    public List<CategoryResponse> getActiveCategories() {
        return cache.get(RedisKeys.MENU_CATEGORIES, new TypeReference<List<CategoryResponse>>() {})
                .orElseGet(() -> {
                    List<CategoryResponse> result = categoryRepository
                            .findAllByActiveTrueOrderByDisplayOrderAsc()
                            .stream()
                            .map(CategoryResponse::from)
                            .collect(Collectors.toList());
                    cache.set(RedisKeys.MENU_CATEGORIES, result, RedisKeys.TTL_MENU_SECONDS);
                    return result;
                });
    }

    public Page<MenuItemResponse> getAvailableItems(Pageable pageable) {
        return itemRepository
                .findAllByActiveTrueAndAvailableTrue(pageable)
                .map(MenuItemResponse::from);
    }

    public MenuItemResponse getItemById(UUID id) {
        String cacheKey = RedisKeys.menuItem(id.toString());
        return cache.get(cacheKey, MenuItemResponse.class)
                .orElseGet(() -> {
                    MenuItemResponse response = itemRepository.findByIdAndActiveTrue(id)
                            .map(MenuItemResponse::from)
                            .orElseThrow(() -> new ResourceNotFoundException("MenuItem"));
                    cache.set(cacheKey, response, RedisKeys.TTL_MENU_SECONDS);
                    return response;
                });
    }

    public Page<MenuItemResponse> searchItems(String query, Pageable pageable) {
        return itemRepository.searchAvailable(query, pageable).map(MenuItemResponse::from);
    }

    public List<MenuItemResponse> getItemsByCategory(String slug) {
        String cacheKey = RedisKeys.menuByCategory(slug);
        return cache.get(cacheKey, new TypeReference<List<MenuItemResponse>>() {})
                .orElseGet(() -> {
                    MenuCategory category = categoryRepository
                            .findBySlugAndActiveTrue(slug)
                            .orElseThrow(() -> new ResourceNotFoundException("MenuCategory"));

                    List<MenuItemResponse> result = itemRepository
                            .findAllByCategoryAndActiveTrueAndAvailableTrueOrderBySortOrderAsc(category)
                            .stream()
                            .map(MenuItemResponse::from)
                            .collect(Collectors.toList());

                    cache.set(cacheKey, result, RedisKeys.TTL_MENU_SECONDS);
                    return result;
                });
    }

    public Page<CategoryResponse> adminListCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(CategoryResponse::from);
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest req) {
        if (categoryRepository.existsBySlug(req.getSlug())) {
            throw new DuplicateResourceException("Category slug already exists: " + req.getSlug());
        }
        if (categoryRepository.existsByName(req.getName())) {
            throw new DuplicateResourceException("Category name already exists: " + req.getName());
        }
        MenuCategory saved = categoryRepository.save(
                MenuCategory.create(req.getName(), req.getSlug(), req.getDisplayOrder()));
        evictMenuCaches();
        return CategoryResponse.from(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest req) {
        MenuCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory"));

        category.setName(req.getName());
        category.setDisplayOrder(req.getDisplayOrder());
        if (req.isActive()) category.activate(); else category.deactivate();

        MenuCategory saved = categoryRepository.save(category);
        evictMenuCaches();
        return CategoryResponse.from(saved);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        MenuCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory"));
        category.deactivate();
        categoryRepository.save(category);
        evictMenuCaches();
    }


    public Page<MenuItemResponse> adminListItems(UUID categoryId, Boolean available,
                                                 Pageable pageable) {
        return itemRepository
                .findAllForAdmin(categoryId, available, pageable)
                .map(MenuItemResponse::from);
    }

    @Transactional
    public MenuItemResponse createItem(CreateMenuItemRequest req) {
        MenuCategory category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory"));

        BigDecimal price = new BigDecimal(req.getPrice());
        MenuItem item = MenuItem.create(req.getName(), price, category);
        item.setDescription(req.getDescription());
//        item.setImageUrl(imageUploadService.upload(req.getFile()));
        item.setSortOrder(req.getSortOrder());
        item.setTags(req.getTags() != null ? req.getTags() : List.of());

        MenuItem saved = itemRepository.save(item);
        evictMenuCaches();
        return MenuItemResponse.from(saved);
    }

    @Transactional
    public MenuItemResponse updateItem(UUID id, UpdateMenuItemRequest req) {
        MenuItem item = itemRepository.findById(id)
                .filter(MenuItem::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem"));

        MenuCategory category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory"));

        item.setName(req.getName());
        item.setDescription(req.getDescription());
        item.setPrice(req.getPrice());
        item.setCategory(category);
        item.setImageUrl(req.getImageUrl());
        item.setSortOrder(req.getSortOrder());
        item.setTags(req.getTags() != null ? req.getTags() : List.of());
        if (req.isAvailable()) item.markAvailable(); else item.markUnavailable();

        MenuItem saved = itemRepository.save(item);
        evictMenuCaches();
        cache.evict(RedisKeys.menuItem(id.toString()));
        return MenuItemResponse.from(saved);
    }

    @Transactional
    public MenuItemResponse toggleAvailability(UUID id, boolean available) {
        MenuItem item = itemRepository.findById(id)
                .filter(MenuItem::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem"));

        if (available) item.markAvailable(); else item.markUnavailable();
        MenuItem saved = itemRepository.save(item);

        // Evict only the specific item and the list caches
        cache.evict(RedisKeys.menuItem(id.toString()));
        evictMenuCaches();
        return MenuItemResponse.from(saved);
    }

    @Transactional
    public void deleteItem(UUID id) {
        MenuItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem"));
        item.softDelete();
        itemRepository.save(item);
        cache.evict(RedisKeys.menuItem(id.toString()));
        evictMenuCaches();
    }

    // ── Cache helpers ─────────────────────────────────────────────────────────

    /**
     * Any write to menu data invalidates all list-level caches.
     * Individual item cache is evicted explicitly where relevant.
     */
    private void evictMenuCaches() {
        cache.evict(RedisKeys.MENU_ALL_ACTIVE);
        cache.evict(RedisKeys.MENU_CATEGORIES);
        cache.evictByPattern(RedisKeys.MENU_BY_CATEGORY + "*");
    }
}

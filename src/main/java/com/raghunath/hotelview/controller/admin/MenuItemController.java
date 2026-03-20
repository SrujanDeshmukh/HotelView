package com.raghunath.hotelview.controller.admin;

import com.raghunath.hotelview.dto.admin.ApiResponse;
import com.raghunath.hotelview.dto.admin.MenuItemRequest;
import com.raghunath.hotelview.entity.MenuItem;
import com.raghunath.hotelview.service.admin.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    @PostMapping("/add")
    public ApiResponse addMenuItem(@RequestBody MenuItemRequest request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminId = auth.getName();
        String message = menuItemService.addMenuItem(request, adminId);
        return new ApiResponse(message);
    }

    @GetMapping("/allmenuitem")
    public Page<MenuItem> getAllMenuItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String hotelId = auth.getName();
        return menuItemService.getAllHotelItems(hotelId, page, size);
    }

    @GetMapping("/category")
    public List<MenuItem> getCategoryItems(@RequestParam String category){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminId = auth.getName();
        return menuItemService.getCategoryItems(adminId,category);
    }

    /**
     * PRODUCTION GRADE: SMART SEARCH
     * This replaces the old exact name search.
     * It returns a list of items matching the query (with typo tolerance).
     */
    @GetMapping("/search")
    public List<MenuItem> searchMenuItems(@RequestParam String query) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String hotelId = auth.getName();

        // Calls the Atlas Search logic in the service
        return menuItemService.searchMenuItems(hotelId, query);
    }

    // Keep this for fetching a specific item's full details by exact name if needed
    @GetMapping("/details")
    public MenuItem getMenuItemByName(@RequestParam String name) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String hotelId = auth.getName();
        return menuItemService.getMenuItemByHotelAndName(hotelId, name);
    }
}
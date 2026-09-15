package com.bikematch.bike.api;

import com.bikematch.bike.BikeCategory;
import com.bikematch.bike.ListPublicBikesService;
import com.bikematch.bike.PublicBikeSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bikes")
@Tag(name = "Catalog", description = "Public bikes approved by moderation")
@SecurityRequirements
public class BikeCatalogController {

    private final ListPublicBikesService listPublicBikesService;

    public BikeCatalogController(ListPublicBikesService listPublicBikesService) {
        this.listPublicBikesService = listPublicBikesService;
    }

    @GetMapping
    @Operation(summary = "List public bikes",
            description = "Pages of 12 bikes, optionally filtered by category.")
    @ApiResponse(responseCode = "200", description = "One page of public bikes")
    @ApiResponse(responseCode = "400", content = @Content, description = "Unknown category or invalid page number")
    public BikeCatalogResponse list(
            @Parameter(description = "ENDURO, E_ENDURO or DOWNHILL; omit it to list every category")
            @RequestParam(required = false) String category,
            @Parameter(description = "Zero-based page number")
            @RequestParam(defaultValue = "0") String page
    ) {
        return BikeCatalogResponse.from(
                listPublicBikesService.list(parseCategory(category), parsePage(page)));
    }

    private BikeCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return BikeCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Category must be ENDURO, E_ENDURO or DOWNHILL");
        }
    }

    private int parsePage(String value) {
        try {
            int page = Integer.parseInt(value);
            if (page < 0) {
                throw new IllegalArgumentException("Page must be zero or greater");
            }
            return page;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Page must be a non-negative integer");
        }
    }
}

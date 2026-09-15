package com.bikematch.moderation.api;

import com.bikematch.moderation.RemovalNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-notices")
@Tag(name = "My notices", description = "Notices for the authenticated user about bikes removed by moderation")
public class MyNoticesController {

    private final RemovalNoticeService removalNoticeService;

    public MyNoticesController(RemovalNoticeService removalNoticeService) {
        this.removalNoticeService = removalNoticeService;
    }

    @GetMapping
    @Operation(summary = "List my notices",
            description = "The caller's bikes removed by a moderator, with the reason, newest first. "
                    + "A notice stays listed until it is dismissed.")
    @ApiResponse(responseCode = "200", description = "Notices not dismissed yet")
    @ApiResponse(responseCode = "401", content = @Content, description = "Missing, invalid or expired token")
    public List<RemovalNoticeResponse> list(
            @AuthenticationPrincipal String authenticatedUserId
    ) {
        long ownerId = Long.parseLong(authenticatedUserId);

        return removalNoticeService.listPending(ownerId).stream()
                .map(RemovalNoticeResponse::from)
                .toList();
    }

    @PostMapping("/{noticeId}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Dismiss a notice",
            description = "Takes the notice out of the list. Dismissing it again changes nothing.")
    @ApiResponse(responseCode = "204", description = "Notice dismissed")
    @ApiResponse(responseCode = "401", content = @Content, description = "Missing, invalid or expired token")
    @ApiResponse(responseCode = "404", content = @Content, description = "Notice not found or addressed to another user")
    public void dismiss(
            @AuthenticationPrincipal String authenticatedUserId,
            @PathVariable long noticeId
    ) {
        removalNoticeService.dismiss(Long.parseLong(authenticatedUserId), noticeId);
    }
}

package com.aurora.commerce.content;

import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
class ContentController {

    private final ContentService contentService;

    ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/content/feed")
    ApiResponse<List<ContentService.ArticleView>> feed(@RequestParam(required = false) String channel) {
        return ApiResponse.success(contentService.feed(channel));
    }

    @GetMapping("/content/articles/{slug}")
    ApiResponse<ContentService.ArticleView> detail(@PathVariable String slug) {
        return ApiResponse.success(contentService.detail(slug));
    }

    @PostMapping("/admin/content/articles")
    ApiResponse<ContentService.ArticleView> create(@Valid @RequestBody CreateArticleRequest request) {
        return ApiResponse.success(contentService.create(request.toCommand()));
    }

    @PatchMapping("/admin/content/articles/{id}/publish")
    ApiResponse<ContentService.ArticleView> publish(@PathVariable Long id) {
        return ApiResponse.success(contentService.publish(id));
    }

    record CreateArticleRequest(
            @NotBlank @Size(max = 140) @Pattern(regexp = "^[a-z0-9-]+$") String slug,
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 500) String summary,
            @NotBlank @Size(max = 500) String coverImageUrl,
            @NotBlank @Size(max = 5000) String contentText,
            @NotBlank @Size(max = 80) String channelCode,
            boolean featured
    ) {
        ContentService.CreateArticleCommand toCommand() {
            return new ContentService.CreateArticleCommand(
                    slug, title, summary, coverImageUrl, contentText, channelCode, featured);
        }
    }
}

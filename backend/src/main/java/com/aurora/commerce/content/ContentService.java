package com.aurora.commerce.content;

import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
class ContentService {

    private final ContentArticleRepository articleRepository;

    ContentService(ContentArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    List<ArticleView> feed(String channel) {
        List<ContentArticle> articles = channel == null || channel.isBlank()
                ? articleRepository.findByStatusOrderByFeaturedDescPublishedAtDesc(ContentArticle.Status.PUBLISHED)
                : articleRepository.findByStatusAndChannelCodeOrderByFeaturedDescPublishedAtDesc(
                        ContentArticle.Status.PUBLISHED, channel.toUpperCase());
        return articles.stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    ArticleView detail(String slug) {
        return toView(articleRepository.findBySlugAndStatus(slug, ContentArticle.Status.PUBLISHED)
                .orElseThrow(this::contentNotFound));
    }

    @Transactional
    ArticleView create(CreateArticleCommand command) {
        return toView(articleRepository.save(new ContentArticle(
                command.slug(), command.title(), command.summary(), command.coverImageUrl(),
                command.contentText(), command.channelCode().toUpperCase(), command.featured())));
    }

    @Transactional
    ArticleView publish(Long id) {
        ContentArticle article = articleRepository.findById(id).orElseThrow(this::contentNotFound);
        article.publish(Instant.now());
        return toView(article);
    }

    @Transactional(readOnly = true)
    List<ArticleView> adminList() {
        return articleRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toView).toList();
    }

    private ArticleView toView(ContentArticle article) {
        return new ArticleView(
                article.id(), article.slug(), article.title(), article.summary(), article.coverImageUrl(),
                article.contentText(), article.channelCode(), article.status().name(),
                article.featured(), article.publishedAt());
    }

    private BusinessException contentNotFound() {
        return new BusinessException("CONTENT_NOT_FOUND", "内容不存在", HttpStatus.NOT_FOUND);
    }

    record CreateArticleCommand(
            String slug, String title, String summary, String coverImageUrl,
            String contentText, String channelCode, boolean featured
    ) {
    }

    record ArticleView(
            Long id, String slug, String title, String summary, String coverImageUrl,
            String contentText, String channelCode, String status, boolean featured, Instant publishedAt
    ) {
    }
}

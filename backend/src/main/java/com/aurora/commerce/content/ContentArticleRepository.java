package com.aurora.commerce.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ContentArticleRepository extends JpaRepository<ContentArticle, Long> {
    List<ContentArticle> findByStatusOrderByFeaturedDescPublishedAtDesc(ContentArticle.Status status);
    List<ContentArticle> findByStatusAndChannelCodeOrderByFeaturedDescPublishedAtDesc(
            ContentArticle.Status status, String channelCode);
    Optional<ContentArticle> findBySlugAndStatus(String slug, ContentArticle.Status status);
}

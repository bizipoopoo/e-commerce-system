INSERT INTO coupons (
    code, name, description, discount_amount, threshold_amount,
    total_limit, claimed_count, per_user_limit, status, starts_at, ends_at
) VALUES
    ('AURORA50', '新会员生活礼', '订单满 ¥299 立减 ¥50', 50.00, 299.00, 10000, 0, 1, 'ACTIVE', '2025-01-01 00:00:00', '2030-12-31 23:59:59'),
    ('HOME120', '理想家居券', '订单满 ¥1000 立减 ¥120', 120.00, 1000.00, 3000, 0, 1, 'ACTIVE', '2025-01-01 00:00:00', '2030-12-31 23:59:59'),
    ('WEEKEND30', '周末灵感券', '订单满 ¥199 立减 ¥30', 30.00, 199.00, 5000, 0, 1, 'ACTIVE', '2025-01-01 00:00:00', '2030-12-31 23:59:59');

INSERT INTO content_articles (
    slug, title, summary, cover_image_url, content_text, channel_code, status, featured, published_at
) VALUES
    ('home-light-and-wood', '让光线成为家里的第二种材质', '从窗边、木纹到一盏灯，重新理解空间里的自然层次。', 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=1200&q=85', '真正舒适的空间不需要复杂堆叠。顺着一天的光线安排阅读、休息和相聚的位置，再用温润木材接住光影，家就有了自己的呼吸。', 'LIVING', 'PUBLISHED', TRUE, '2026-08-10 09:00:00'),
    ('slow-coffee-weekend', '把周末留给一杯慢咖啡', '器具不是仪式的全部，专注的十分钟才是。', 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=1200&q=85', '称豆、研磨、注水，重复而细微的动作让时间慢下来。好的器具只做一件事：让你更容易进入这段专注。', 'RITUAL', 'PUBLISHED', TRUE, '2026-08-11 09:00:00'),
    ('city-edge-walk', '从城市边缘重新出发', '一只轻量背包，一条不用赶时间的路线。', 'https://images.unsplash.com/photo-1551632811-561732d1e306?auto=format&fit=crop&w=1200&q=85', '不必远行才算户外。找到城市与自然交界的步道，带上水、薄外套和轻便装备，用半天时间换一个更开阔的视角。', 'OUTDOOR', 'PUBLISHED', FALSE, '2026-08-12 09:00:00');

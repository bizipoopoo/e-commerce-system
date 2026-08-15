INSERT INTO categories (id, name, slug, icon, sort_order) VALUES
    (1, '今日上新', 'new-arrivals', '◌', 10),
    (2, '居家生活', 'living', '⌂', 20),
    (3, '数码精选', 'digital', '◇', 30),
    (4, '美妆个护', 'beauty', '✦', 40),
    (5, '户外运动', 'outdoor', '♧', 50),
    (6, '服饰穿搭', 'style', '◐', 60);

INSERT INTO brands (id, name, slug) VALUES
    (1, 'Aurora Home', 'aurora-home'),
    (2, 'Daily Ritual', 'daily-ritual'),
    (3, 'Sound & Light', 'sound-light'),
    (4, 'Urban Outdoor', 'urban-outdoor');

INSERT INTO products (id, category_id, brand_id, name, subtitle, description, cover_image_url, status, featured, sales_count, rating) VALUES
    (1, 2, 1, '云感人体工学休闲椅', '让每一次坐下都成为恢复', '兼顾包裹感与支撑力的人体工学休闲椅。', 'https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?auto=format&fit=crop&w=1000&q=85', 'PUBLISHED', TRUE, 862, 4.92),
    (2, 2, 2, '晨雾手冲咖啡套装', '给早晨一个从容的开始', '包含手冲壶、滤杯与分享壶的入门套装。', 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=1000&q=85', 'PUBLISHED', TRUE, 1520, 4.88),
    (3, 3, 3, '原木无线氛围音箱', '声音与光线共同构成空间', '温润原木外观与细腻声场结合的桌面音箱。', 'https://images.unsplash.com/photo-1545454675-3531b543be5d?auto=format&fit=crop&w=1000&q=85', 'PUBLISHED', TRUE, 635, 4.95),
    (4, 5, 4, '山系轻量城市双肩包', '从通勤到周末轻户外', '轻量、防泼水并具备合理收纳分区。', 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=1000&q=85', 'PUBLISHED', TRUE, 928, 4.86),
    (5, 4, 2, '雪松静心香氛蜡烛', '把森林气息留在房间', '雪松、苔藓与少量柑橘构成的木质香调。', 'https://images.unsplash.com/photo-1602874801006-e26e18e58d91?auto=format&fit=crop&w=1000&q=85', 'PUBLISHED', FALSE, 411, 4.90),
    (6, 6, 4, '柔雾羊毛混纺围巾', '温柔包裹每一个冷天', '羊毛混纺材质，轻盈保暖，适合日常搭配。', 'https://images.unsplash.com/photo-1609803384069-19f3e5a70e75?auto=format&fit=crop&w=1000&q=85', 'PUBLISHED', FALSE, 286, 4.84);

INSERT INTO product_skus (id, product_id, sku_code, name, spec_values, sale_price, market_price, status) VALUES
    (1, 1, 'AUR-CHAIR-CREAM', '云感人体工学休闲椅·米白', '{"color":"米白"}', 2499.00, 2899.00, 'ACTIVE'),
    (2, 2, 'AUR-COFFEE-SET', '晨雾手冲咖啡套装·标准版', '{"set":"标准版"}', 429.00, 529.00, 'ACTIVE'),
    (3, 3, 'AUR-SPEAKER-WOOD', '原木无线氛围音箱·胡桃木', '{"wood":"胡桃木"}', 899.00, 1099.00, 'ACTIVE'),
    (4, 4, 'AUR-BAG-GREEN', '山系轻量城市双肩包·松针绿', '{"color":"松针绿"}', 569.00, 699.00, 'ACTIVE'),
    (5, 5, 'AUR-CANDLE-CEDAR', '雪松静心香氛蜡烛·220g', '{"weight":"220g"}', 189.00, 229.00, 'ACTIVE'),
    (6, 6, 'AUR-SCARF-FOG', '柔雾羊毛混纺围巾·雾灰', '{"color":"雾灰"}', 329.00, 399.00, 'ACTIVE');

INSERT INTO banners (id, title, subtitle, image_url, link_url, position_code, sort_order) VALUES
    (1, '把好生活带回家', '从设计、质感与真实体验出发，挑选值得长久陪伴的日常好物。', 'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?auto=format&fit=crop&w=1400&q=88', '/collections/seasonal', 'HOME_HERO', 10),
    (2, '原木与光', '打造会呼吸的客厅', 'https://images.unsplash.com/photo-1549497538-303791108f95?auto=format&fit=crop&w=1200&q=85', '/stories/wood-and-light', 'HOME_FEATURE', 10);

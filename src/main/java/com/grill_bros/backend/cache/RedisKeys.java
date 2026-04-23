package com.grill_bros.backend.cache;

public final class RedisKeys {

    private RedisKeys() {}

    // ── Prefixes ─────────────────────────────────────────────────────────────
    private static final String NS = "grillbros";

    // ── Menu ─────────────────────────────────────────────────────────────────
    public static final String MENU_ALL_ACTIVE      = NS + ":menu:all-active";
    public static final String MENU_CATEGORIES      = NS + ":menu:categories";
    public static final String MENU_ITEM_PREFIX     = NS + ":menu:item:";
    public static final String MENU_BY_CATEGORY     = NS + ":menu:category:";

    public static String menuItem(String id)           { return MENU_ITEM_PREFIX + id; }
    public static String menuByCategory(String slug)   { return MENU_BY_CATEGORY + slug; }

    // ── Payment ───────────────────────────────────────────────────────────────
    public static final String PAYMENT_IDEM_PREFIX  = NS + ":payment:idem:";
    public static final String PAYMENT_STATUS_PREFIX = NS + ":payment:status:";
    public static final String PAYMENT_RATELIMIT    = NS + ":payment:ratelimit:";
    public static final String ORDER_LOCK_PREFIX    = NS + ":order:lock:";

    public static String paymentIdem(String key)       { return PAYMENT_IDEM_PREFIX + key; }
    public static String paymentStatus(String orderId) { return PAYMENT_STATUS_PREFIX + orderId; }
    public static String paymentRateLimit(String phone){ return PAYMENT_RATELIMIT + phone; }
    public static String orderLock(String orderId)     { return ORDER_LOCK_PREFIX + orderId; }

    // ── Admin Dashboard ───────────────────────────────────────────────────────
    public static final String ADMIN_STATS_TODAY    = NS + ":admin:stats:today";
    public static final String ADMIN_STATS_REVENUE  = NS + ":admin:stats:revenue:";
    public static final String ADMIN_NOTIFY_QUEUE   = NS + ":admin:notify:queue";
    public static final String ADMIN_TOKEN_BLACKLIST = NS + ":admin:token:blacklist:";

    public static String adminRevenueRange(String from, String to) {
        return ADMIN_STATS_REVENUE + from + ":" + to;
    }
    public static String tokenBlacklist(String jti) {
        return ADMIN_TOKEN_BLACKLIST + jti;
    }

    // ── TTLs (seconds) ────────────────────────────────────────────────────────
    public static final long TTL_MENU_SECONDS           = 300;    // 5 min
    public static final long TTL_PAYMENT_IDEM_SECONDS   = 86_400; // 24 h
    public static final long TTL_PAYMENT_STATUS_SECONDS = 180;    // 3 min poll window
    public static final long TTL_STATS_SECONDS          = 120;    // 2 min
    public static final long TTL_ORDER_LOCK_SECONDS     = 30;     // distributed lock
    public static final long TTL_RATELIMIT_SECONDS      = 60;     // sliding window
    public static final long TTL_NOTIFY_QUEUE_SECONDS   = 1_800;  // 30 min missed events
}

package com.npu.lms.security;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量级内存滑动窗口限流器（无外部依赖）。
 *
 * <p>P0 安全加固：用于登录、注册验证、密码找回等关键端点的双维度限流，
 * 防止暴力破解与邮件轰炸。生产环境多实例部署时建议替换为 Redis + Lua 实现。</p>
 */
@Component
public class RateLimiter {

    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    /** 清理时使用的最大窗口（1 小时），保证过期键可被回收 */
    private static final long MAX_WINDOW_MILLIS = 3_600_000L;

    /** 全量清理间隔（1 分钟） */
    private static final long CLEANUP_INTERVAL_MILLIS = 60_000L;

    private volatile long lastCleanup = 0L;

    /**
     * 尝试获取一次请求配额。
     *
     * @param key          限流键，例如 "login:ip:1.2.3.4" 或 "forgot:email:a@b.com"
     * @param maxRequests  窗口内允许的最大请求数
     * @param windowMillis 窗口时长（毫秒）
     * @return true 表示放行，false 表示超出限流
     */
    public boolean tryAcquire(String key, int maxRequests, long windowMillis) {
        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);

        Deque<Long> queue = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() >= windowMillis) {
                queue.pollFirst();
            }
            if (queue.size() >= maxRequests) {
                return false;
            }
            queue.addLast(now);
            return true;
        }
    }

    private void cleanupIfNeeded(long now) {
        if (now - lastCleanup < CLEANUP_INTERVAL_MILLIS) {
            return;
        }
        lastCleanup = now;
        buckets.entrySet().removeIf(entry -> {
            Deque<Long> queue = entry.getValue();
            synchronized (queue) {
                while (!queue.isEmpty() && now - queue.peekFirst() >= MAX_WINDOW_MILLIS) {
                    queue.pollFirst();
                }
                return queue.isEmpty();
            }
        });
    }
}

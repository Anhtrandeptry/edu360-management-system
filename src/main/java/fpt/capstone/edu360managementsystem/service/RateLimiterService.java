package fpt.capstone.edu360managementsystem.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

/**
 * Rate Limiter Service - Giới hạn số lần request trong khoảng thời gian Sử dụng
 * in-memory cache (ConcurrentHashMap) để lưu trữ
 *
 * Được dùng cho các chức năng cần bảo vệ khỏi spam/abuse: - Forgot Password
 * (chống email bomb) - OTP verification - Login attempts
 */
@Service
public class RateLimiterService {

    // Map lưu: key -> [timestamp của lần request gần nhất, số lần request trong window]
    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();

    // Cleanup old entries periodically
    private long lastCleanupTime = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL_MS = 60 * 60 * 1000; // 1 hour

    /**
     * Kiểm tra xem request có được phép không
     *
     * @param key Unique key (ví dụ: email, IP, userId)
     * @param maxAttempts Số lần request tối đa trong window
     * @param windowMs Khoảng thời gian window (milliseconds)
     * @return true nếu được phép, false nếu bị rate limit
     */
    public boolean isAllowed(String key, int maxAttempts, long windowMs) {
        cleanupIfNeeded();

        long now = System.currentTimeMillis();
        RateLimitInfo info = rateLimitMap.compute(key, (k, existing) -> {
            if (existing == null) {
                // Chưa có record, tạo mới
                return new RateLimitInfo(now, 1);
            }

            // Kiểm tra xem window đã hết hạn chưa
            if (now - existing.windowStartTime > windowMs) {
                // Window mới, reset counter
                return new RateLimitInfo(now, 1);
            }

            // Trong cùng window, tăng counter
            existing.attemptCount++;
            return existing;
        });

        return info.attemptCount <= maxAttempts;
    }

    /**
     * Lấy thời gian còn lại (giây) cho đến khi có thể request tiếp
     *
     * @param key Unique key
     * @param windowMs Khoảng thời gian window (milliseconds)
     * @return Số giây còn lại, 0 nếu có thể request ngay
     */
    public long getRemainingCooldownSeconds(String key, long windowMs) {
        RateLimitInfo info = rateLimitMap.get(key);
        if (info == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - info.windowStartTime;

        if (elapsed >= windowMs) {
            return 0;
        }

        return TimeUnit.MILLISECONDS.toSeconds(windowMs - elapsed);
    }

    /**
     * Reset rate limit cho một key cụ thể
     *
     * @param key Unique key
     */
    public void reset(String key) {
        rateLimitMap.remove(key);
    }

    /**
     * Dọn dẹp các entries đã hết hạn để tránh memory leak
     */
    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            lastCleanupTime = now;
            // Remove entries older than 24 hours
            long cutoffTime = now - (24 * 60 * 60 * 1000);
            rateLimitMap.entrySet().removeIf(entry -> entry.getValue().windowStartTime < cutoffTime);
        }
    }

    /**
     * Kiểm tra và ghi nhận login attempt thất bại Sử dụng progressive lockout:
     * càng nhiều lần thất bại, thời gian chờ càng lâu
     *
     * @param key Unique key (username hoặc IP)
     * @return số giây cần chờ nếu bị lock, 0 nếu được phép thử
     */
    public long recordFailedLoginAndGetLockoutSeconds(String key) {
        cleanupIfNeeded();

        long now = System.currentTimeMillis();
        RateLimitInfo info = rateLimitMap.compute(key, (k, existing) -> {
            if (existing == null) {
                return new RateLimitInfo(now, 1);
            }

            // Progressive lockout windows: 1min, 5min, 15min, 30min, 1hour
            long lockoutMs = getLockoutDuration(existing.attemptCount);

            // Nếu đã hết thời gian lockout, reset counter
            if (now - existing.windowStartTime > lockoutMs) {
                return new RateLimitInfo(now, 1);
            }

            existing.attemptCount++;
            return existing;
        });

        // Trả về thời gian cần chờ nếu vượt quá 5 lần thất bại
        if (info.attemptCount > 5) {
            long lockoutMs = getLockoutDuration(info.attemptCount - 1);
            long elapsed = now - info.windowStartTime;
            long remaining = lockoutMs - elapsed;
            return remaining > 0 ? TimeUnit.MILLISECONDS.toSeconds(remaining) : 0;
        }

        return 0;
    }

    /**
     * Kiểm tra xem có đang bị lockout không
     *
     * @param key Unique key
     * @return số giây còn lại nếu bị lock, 0 nếu không bị lock
     */
    public long getLoginLockoutSeconds(String key) {
        RateLimitInfo info = rateLimitMap.get(key);
        if (info == null || info.attemptCount <= 5) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long lockoutMs = getLockoutDuration(info.attemptCount - 1);
        long elapsed = now - info.windowStartTime;
        long remaining = lockoutMs - elapsed;

        return remaining > 0 ? TimeUnit.MILLISECONDS.toSeconds(remaining) : 0;
    }

    /**
     * Reset login attempts sau khi đăng nhập thành công
     *
     * @param key Unique key
     */
    public void clearLoginAttempts(String key) {
        rateLimitMap.remove(key);
    }

    /**
     * Tính thời gian lockout dựa trên số lần thất bại (progressive lockout) 5
     * lần: 1 phút 6-7 lần: 5 phút 8-9 lần: 15 phút 10-14 lần: 30 phút 15+ lần:
     * 1 giờ
     */
    private long getLockoutDuration(int failedAttempts) {
        if (failedAttempts <= 5) {
            return 60 * 1000L;           // 1 phút

                }if (failedAttempts <= 7) {
            return 5 * 60 * 1000L;       // 5 phút

                }if (failedAttempts <= 9) {
            return 15 * 60 * 1000L;      // 15 phút

                }if (failedAttempts <= 14) {
            return 30 * 60 * 1000L;     // 30 phút

                }return 60 * 60 * 1000L;                                // 1 giờ
    }

    /**
     * Inner class để lưu thông tin rate limit
     */
    private static class RateLimitInfo {

        long windowStartTime;
        int attemptCount;

        RateLimitInfo(long windowStartTime, int attemptCount) {
            this.windowStartTime = windowStartTime;
            this.attemptCount = attemptCount;
        }
    }
}

package fpt.capstone.edu360managementsystem.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.PasswordResetToken;
import fpt.capstone.edu360managementsystem.entity.User;

/**
 * Repository for password reset token operations.
 *
 * @author 360edu
 * @version 1.0
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find token by token string
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Find valid (not used, not expired) token for user
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.user = :user AND t.used = false AND t.expiryDate > :now")
    Optional<PasswordResetToken> findValidTokenByUser(User user, LocalDateTime now);

    /**
     * Invalidate all tokens for a user (mark as used)
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void invalidateAllTokensForUser(User user);

    /**
     * Delete expired tokens (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :cutoff")
    void deleteExpiredTokens(LocalDateTime cutoff);

    /**
     * Count valid tokens for user in time window (for rate limiting)
     */
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.user = :user AND t.createdAt > :since")
    long countTokensForUserSince(User user, LocalDateTime since);
}

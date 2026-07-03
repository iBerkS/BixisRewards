package com.yeditepemc.bixisrewards.model;

import com.yeditepemc.bixiscore.model.PlayerData;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * BixisCore'daki {@link PlayerData} zaman damgalarının etrafına ödül mantığı
 * ekleyen ince bir sarmalayıcı (wrapper).
 *
 * <p>Kendi başına durum tutmaz; her sorgu doğrudan alttaki {@link PlayerData}
 * üzerinden okunur (lastDaily / lastWeekly / lastMonthly / streakDays).
 * Böylece BixisCore verisiyle her zaman senkrondur.
 *
 * <p>Tüm zaman hesapları Türkiye saatine (UTC+3) göre yapılır (CLAUDE.md — Notlar).
 */
public class RewardData {

    /** Türkiye saati — ödül gün sınırı bu bölgeye göre belirlenir. */
    public static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    /** Streak, son günlük girişten bu kadar saat geçtiyse sıfırlanır. */
    public static final long STREAK_RESET_HOURS = 48L;

    /** Haftalık ödülün yeniden talep edilebilmesi için gereken gün. */
    private static final long WEEKLY_DAYS = 7L;

    /** Aylık ödülün yeniden talep edilebilmesi için gereken gün. */
    private static final long MONTHLY_DAYS = 30L;

    private final PlayerData data;

    public RewardData(PlayerData data) {
        this.data = data;
    }

    /** Sarmalanan ham BixisCore verisi. */
    public PlayerData getPlayerData() {
        return data;
    }

    /** Şu anki Türkiye yerel zamanı. */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    // ------------------------------------------------------------------
    //  Talep uygunluğu (eligibility)
    // ------------------------------------------------------------------

    /**
     * Günlük ödül alınabilir mi? Günlük ödül Türkiye takviminde her yeni gün
     * (gece yarısı sonrası) yeniden alınabilir.
     */
    public boolean canClaimDaily() {
        LocalDateTime last = data.getLastDaily();
        if (last == null) {
            return true;
        }
        LocalDate today = now().toLocalDate();
        return last.toLocalDate().isBefore(today);
    }

    /** Haftalık ödül alınabilir mi? (son alımdan >= 7 gün geçmişse) */
    public boolean canClaimWeekly() {
        return elapsedDays(data.getLastWeekly()) >= WEEKLY_DAYS;
    }

    /** Aylık ödül alınabilir mi? (son alımdan >= 30 gün geçmişse) */
    public boolean canClaimMonthly() {
        return elapsedDays(data.getLastMonthly()) >= MONTHLY_DAYS;
    }

    // ------------------------------------------------------------------
    //  Streak
    // ------------------------------------------------------------------

    /**
     * Geçerli streak. Son günlük giriş {@link #STREAK_RESET_HOURS} saatten
     * eskiyse streak fiilen sıfırlanmış sayılır (CLAUDE.md — 48 saat kuralı).
     */
    public int getEffectiveStreak() {
        LocalDateTime last = data.getLastDaily();
        if (last == null) {
            return 0;
        }
        if (Duration.between(last, now()).toHours() > STREAK_RESET_HOURS) {
            return 0;
        }
        return data.getStreakDays();
    }

    /**
     * Streak'e göre ödül çarpanı:
     * <ul>
     *   <li>7+ gün → 1.5 (%50 bonus)</li>
     *   <li>3-6 gün → 1.25 (%25 bonus)</li>
     *   <li>diğer → 1.0</li>
     * </ul>
     */
    public double getStreakBonus() {
        int streak = getEffectiveStreak();
        if (streak >= 7) {
            return 1.5;
        }
        if (streak >= 3) {
            return 1.25;
        }
        return 1.0;
    }

    // ------------------------------------------------------------------
    //  Kalan süre bilgisi (GUI için)
    // ------------------------------------------------------------------

    /** Sonraki günlük ödüle kalan yaklaşık süre. Alınabiliyorsa {@link Duration#ZERO}. */
    public Duration timeUntilDaily() {
        if (canClaimDaily()) {
            return Duration.ZERO;
        }
        LocalDateTime nextMidnight = now().toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now(), nextMidnight);
    }

    /** Sonraki haftalık ödüle kalan süre. Alınabiliyorsa {@link Duration#ZERO}. */
    public Duration timeUntilWeekly() {
        return remaining(data.getLastWeekly(), WEEKLY_DAYS);
    }

    /** Sonraki aylık ödüle kalan süre. Alınabiliyorsa {@link Duration#ZERO}. */
    public Duration timeUntilMonthly() {
        return remaining(data.getLastMonthly(), MONTHLY_DAYS);
    }

    // ------------------------------------------------------------------
    //  Yardımcılar
    // ------------------------------------------------------------------

    private long elapsedDays(LocalDateTime last) {
        if (last == null) {
            return Long.MAX_VALUE;
        }
        return Duration.between(last, now()).toDays();
    }

    private Duration remaining(LocalDateTime last, long periodDays) {
        if (last == null) {
            return Duration.ZERO;
        }
        LocalDateTime next = last.plusDays(periodDays);
        Duration remaining = Duration.between(now(), next);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}

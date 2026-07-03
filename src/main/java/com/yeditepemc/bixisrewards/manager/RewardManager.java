package com.yeditepemc.bixisrewards.manager;

import com.yeditepemc.bixiscore.BixisCorePlugin;
import com.yeditepemc.bixiscore.model.PlayerData;
import com.yeditepemc.bixisrewards.BixisRewardsPlugin;
import com.yeditepemc.bixisrewards.model.RewardData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ödül mantığının merkezî yöneticisi (singleton).
 *
 * <p>Her talep metodu şu adımları izler:
 * uygunluk kontrolü → streak bonusu uygula → BixisCore API'sini çağır →
 * zaman damgalarını güncelle → kasa tier'ı için zar at.
 *
 * <p>Coin/XP ekleme BixisCore API'si üzerinden yapılır; API bu işlemler için
 * oyuncuya kendi mesajını gönderir (bkz. BixisCoreAPI). Manager ayrıca kısa bir
 * özet mesajı gönderir.
 */
public class RewardManager {

    // ---- Günlük ----
    private static final long DAILY_COIN = 150L;
    private static final long DAILY_XP = 100L;

    // ---- Haftalık ----
    private static final long WEEKLY_COIN = 600L;
    private static final long WEEKLY_XP = 400L;

    // ---- Aylık ----
    private static final long MONTHLY_COIN = 1500L;
    private static final long MONTHLY_XP = 1000L;

    private static RewardManager instance;

    private final BixisRewardsPlugin plugin;

    public RewardManager(BixisRewardsPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static RewardManager getInstance() {
        return instance;
    }

    // ------------------------------------------------------------------
    //  Veri erişimi
    // ------------------------------------------------------------------

    /**
     * Oyuncunun ödül verisini döner. Oyuncu verisi henüz yüklenmemişse
     * (BixisCore cache'inde yoksa) {@code null}.
     */
    public RewardData getRewardData(Player player) {
        PlayerData data = BixisCorePlugin.getAPI().getPlayerData(player);
        return data == null ? null : new RewardData(data);
    }

    // ------------------------------------------------------------------
    //  Günlük
    // ------------------------------------------------------------------

    public boolean claimDaily(Player player) {
        PlayerData data = BixisCorePlugin.getAPI().getPlayerData(player);
        if (data == null) {
            player.sendMessage("§cVerin henüz yüklenmedi, birazdan tekrar dene.");
            return false;
        }
        RewardData rd = new RewardData(data);
        if (!rd.canClaimDaily()) {
            player.sendMessage("§cGünlük ödülünü zaten aldın! §7Yeni ödül gece yarısı (TR).");
            return false;
        }

        // --- Streak güncelle ---
        int newStreak = computeNewStreak(rd, data);
        data.setStreakDays(newStreak);
        double bonus = streakBonusFor(newStreak);

        // --- Ödülü ver (streak bonusu günlük coin & XP'ye uygulanır) ---
        long coins = Math.round(DAILY_COIN * bonus);
        long xp = Math.round(DAILY_XP * bonus);
        BixisCorePlugin.getAPI().addCoins(player, coins);
        BixisCorePlugin.getAPI().addXP(player, xp);

        // --- Zaman damgası + kaydet ---
        data.setLastDaily(RewardData.now());
        save(data);

        // --- Özet mesaj ---
        player.sendMessage("§6§l» Günlük Ödül Alındı!");
        player.sendMessage("§7Streak: §e" + newStreak + " gün"
                + (bonus > 1.0 ? " §a(+%" + (int) Math.round((bonus - 1.0) * 100) + " bonus)" : ""));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        // --- Garantili kasa: her günlük ödülde 1★ veya 2★ (50/50) ---
        grantChest(player, randomTier(1, 2));

        // --- 7 gün streak → haftalık ödül otomatik tetiklenir ---
        if (newStreak >= 7 && rd.canClaimWeekly()) {
            player.sendMessage("§b✦ §e7 günlük streak! §bHaftalık ödül otomatik açılıyor...");
            claimWeekly(player);
        }
        return true;
    }

    /**
     * Yeni streak değerini hesaplar. Talep uygunluğu (yeni gün) çağırandan önce
     * garanti edilir; burada yalnızca devam/sıfırlama kararı verilir.
     */
    private int computeNewStreak(RewardData rd, PlayerData data) {
        LocalDateTime last = data.getLastDaily();
        if (last == null) {
            return 1; // ilk günlük ödül
        }
        // 48 saatten uzun ara → streak sıfırlanır, baştan başlar
        if (rd.getEffectiveStreak() == 0) {
            return 1;
        }
        return data.getStreakDays() + 1;
    }

    // ------------------------------------------------------------------
    //  Haftalık
    // ------------------------------------------------------------------

    public boolean claimWeekly(Player player) {
        PlayerData data = BixisCorePlugin.getAPI().getPlayerData(player);
        if (data == null) {
            player.sendMessage("§cVerin henüz yüklenmedi, birazdan tekrar dene.");
            return false;
        }
        RewardData rd = new RewardData(data);
        if (!rd.canClaimWeekly()) {
            player.sendMessage("§cHaftalık ödülünü zaten aldın!");
            return false;
        }

        BixisCorePlugin.getAPI().addCoins(player, WEEKLY_COIN);
        BixisCorePlugin.getAPI().addXP(player, WEEKLY_XP);

        data.setLastWeekly(RewardData.now());
        save(data);

        player.sendMessage("§6§l» Haftalık Ödül Alındı!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // Garantili 3-4 yıldız kasa
        grantChest(player, randomTier(3, 4));
        return true;
    }

    // ------------------------------------------------------------------
    //  Aylık
    // ------------------------------------------------------------------

    public boolean claimMonthly(Player player) {
        PlayerData data = BixisCorePlugin.getAPI().getPlayerData(player);
        if (data == null) {
            player.sendMessage("§cVerin henüz yüklenmedi, birazdan tekrar dene.");
            return false;
        }
        RewardData rd = new RewardData(data);
        if (!rd.canClaimMonthly()) {
            player.sendMessage("§cAylık ödülünü zaten aldın!");
            return false;
        }

        BixisCorePlugin.getAPI().addCoins(player, MONTHLY_COIN);
        BixisCorePlugin.getAPI().addXP(player, MONTHLY_XP);

        data.setLastMonthly(RewardData.now());
        save(data);

        player.sendMessage("§6§l» Aylık Ödül Alındı!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);

        // Garantili 5 yıldız kasa
        grantChest(player, 5);
        return true;
    }

    // ------------------------------------------------------------------
    //  Kasa
    // ------------------------------------------------------------------

    /**
     * Oyuncuya belirtilen tier'da (1-5 yıldız) bir gizemli kutu (mystery box)
     * verir. GadgetsMenu'nün konsol komutu üzerinden çalışır; kasa vermenin
     * tek kaynağıdır.
     *
     * <p>Not: {@code /rewards admin give} tarafından da çağrıldığı için
     * {@code public} bırakıldı (aksi hâlde derleme kırılır).
     */
    public void grantChest(Player player, int tier) {
        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "gmysterybox give " + player.getName() + " 1 " + tier
        );
    }

    // ------------------------------------------------------------------
    //  Yardımcılar
    // ------------------------------------------------------------------

    /** Streak değerine karşılık gelen çarpan (1.0 / 1.25 / 1.5). */
    public static double streakBonusFor(int streak) {
        if (streak >= 7) {
            return 1.5;
        }
        if (streak >= 3) {
            return 1.25;
        }
        return 1.0;
    }

    private int randomTier(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /** BixisCore'un veritabanına asenkron kaydeder. */
    private void save(PlayerData data) {
        BixisCorePlugin.getInstance().getPlayerDataManager().savePlayer(data);
    }
}

package com.yeditepemc.bixisrewards;

import com.yeditepemc.bixisrewards.gui.RewardGUI;
import com.yeditepemc.bixisrewards.manager.RewardManager;
import com.yeditepemc.bixisrewards.listener.RewardListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * BixisRewards ana plugin sınıfı.
 *
 * <p>Günlük / haftalık / aylık ödül sistemini, streak takibini ve kademeli
 * kasa sistemini yönetir. Coin/XP işlemleri için BixisCore API'sini
 * ({@code BixisCorePlugin.getAPI()}) kullanır; bu yüzden plugin.yml içinde
 * BixisCore'a {@code depend} eder.
 */
public final class BixisRewardsPlugin extends JavaPlugin {

    private static BixisRewardsPlugin instance;

    private RewardManager rewardManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1) Manager
        this.rewardManager = new RewardManager(this);

        // 2) Event dinleyicileri
        getServer().getPluginManager().registerEvents(
                new RewardListener(this, rewardManager), this);

        getLogger().info("BixisRewards etkinleştirildi. (v" + getPluginMeta().getVersion() + ")");
    }

    @Override
    public void onDisable() {
        getLogger().info("BixisRewards devre dışı bırakıldı.");
        instance = null;
    }

    public static BixisRewardsPlugin getInstance() {
        return instance;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    // ------------------------------------------------------------------
    //  Komutlar
    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String name = command.getName().toLowerCase();

        // /gunlukodul (ve alias'ı günlüködül) → menüyü aç
        if (name.equals("gunlukodul")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cBu komut yalnızca oyuncular tarafından kullanılabilir.");
                return true;
            }
            RewardGUI.open(player);
            return true;
        }

        // /rewards admin give <oyuncu> <tier>
        if (name.equals("rewards")) {
            return handleRewardsCommand(sender, args);
        }
        return false;
    }

    private boolean handleRewardsCommand(CommandSender sender, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("admin")) {
            sender.sendMessage("§6BixisRewards §7admin komutu:");
            sender.sendMessage("§e/rewards admin give <oyuncu> <tier> §7- kasa ver (tier 1-5)");
            return true;
        }
        if (!sender.hasPermission("bixisrewards.admin")) {
            sender.sendMessage("§cBu komutu kullanma iznin yok.");
            return true;
        }
        // args: admin give <oyuncu> <tier>
        if (args.length < 4 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§cKullanım: §e/rewards admin give <oyuncu> <tier>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage("§cOyuncu bulunamadı veya çevrimdışı: §e" + args[2]);
            return true;
        }

        int tier;
        try {
            tier = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cGeçersiz tier: §e" + args[3] + " §7(1-5 arası olmalı)");
            return true;
        }
        if (tier < 1 || tier > 5) {
            sender.sendMessage("§cTier 1 ile 5 arasında olmalı.");
            return true;
        }

        rewardManager.grantChest(target, tier);
        sender.sendMessage("§a" + target.getName() + " oyuncusuna §f" + tier
                + " yıldız kasa §averildi.");
        return true;
    }
}

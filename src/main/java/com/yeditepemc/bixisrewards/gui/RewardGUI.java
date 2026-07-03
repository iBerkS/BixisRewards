package com.yeditepemc.bixisrewards.gui;

import com.yeditepemc.bixisrewards.manager.RewardManager;
import com.yeditepemc.bixisrewards.model.RewardData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 27 slotluk özel ödül menüsü. Günlük / haftalık / aylık ödüllerin durumunu
 * (hazır / alındı / kalan süre) gösterir ve tıklamayla talep imkânı sunar.
 *
 * <p>{@link InventoryHolder} olarak kendini işaretler; böylece
 * {@code RewardListener} bu envanteri güvenle tanıyabilir.
 */
public class RewardGUI implements InventoryHolder {

    public static final int SIZE = 27;
    public static final String TITLE = "§6§lÖdül Menüsü";

    public static final int SLOT_DAILY = 11;
    public static final int SLOT_WEEKLY = 13;
    public static final int SLOT_MONTHLY = 15;

    private final Inventory inventory;

    private RewardGUI() {
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Oyuncu için menüyü oluşturur, doldurur ve açar.
     * Oyuncu verisi yüklü değilse menü açılmaz.
     */
    public static void open(Player player) {
        RewardData rd = RewardManager.getInstance().getRewardData(player);
        if (rd == null) {
            player.sendMessage("§cVerin henüz yüklenmedi, birazdan tekrar dene.");
            return;
        }
        RewardGUI gui = new RewardGUI();
        gui.render(rd);
        player.openInventory(gui.inventory);
    }

    /** Menü içeriğini güncel duruma göre çizer. */
    public void render(RewardData rd) {
        ItemStack filler = simpleItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(SLOT_DAILY, buildDaily(rd));
        inventory.setItem(SLOT_WEEKLY, buildWeekly(rd));
        inventory.setItem(SLOT_MONTHLY, buildMonthly(rd));
    }

    // ------------------------------------------------------------------
    //  Ödül item'ları
    // ------------------------------------------------------------------

    private ItemStack buildDaily(RewardData rd) {
        boolean available = rd.canClaimDaily();
        int streak = rd.getEffectiveStreak();
        double bonus = rd.getStreakBonus();

        List<String> lore = new ArrayList<>();
        lore.add("§7150 coin §8+ §7100 XP");
        lore.add("§7Garantili §f1-2 yıldız kasa");
        lore.add("");
        lore.add("§7Streak: §e" + streak + " gün");
        if (bonus > 1.0) {
            lore.add("§aAktif bonus: §2+%" + (int) Math.round((bonus - 1.0) * 100));
        }
        lore.add("");
        addStatusLine(lore, available, rd.timeUntilDaily());

        return statusItem("§e§lGünlük Ödül", available, lore);
    }

    private ItemStack buildWeekly(RewardData rd) {
        boolean available = rd.canClaimWeekly();

        List<String> lore = new ArrayList<>();
        lore.add("§7600 coin §8+ §7400 XP");
        lore.add("§7Garantili §f3-4 yıldız kasa");
        lore.add("");
        addStatusLine(lore, available, rd.timeUntilWeekly());

        return statusItem("§b§lHaftalık Ödül", available, lore);
    }

    private ItemStack buildMonthly(RewardData rd) {
        boolean available = rd.canClaimMonthly();

        List<String> lore = new ArrayList<>();
        lore.add("§71500 coin §8+ §71000 XP");
        lore.add("§7Garantili §f5 yıldız kasa");
        lore.add("");
        addStatusLine(lore, available, rd.timeUntilMonthly());

        return statusItem("§d§lAylık Ödül", available, lore);
    }

    // ------------------------------------------------------------------
    //  Yardımcılar
    // ------------------------------------------------------------------

    private void addStatusLine(List<String> lore, boolean available, Duration remaining) {
        if (available) {
            lore.add("§a▶ Almak için tıkla!");
        } else {
            lore.add("§cAlındı §7— kalan: §f" + formatDuration(remaining));
        }
    }

    /** Duruma göre renkli/farklı materyalde bir item üretir. */
    private ItemStack statusItem(String name, boolean available, List<String> lore) {
        Material material = available ? Material.CHEST : Material.CLOCK;
        String prefix = available ? "§a" : "§8";
        return item(material, prefix + name, lore);
    }

    private ItemStack simpleItem(Material material, String name) {
        return item(material, name, null);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** Süreyi "2 gün 3 saat" / "5 saat 12 dk" / "9 dk" biçiminde Türkçe döner. */
    private String formatDuration(Duration d) {
        if (d == null || d.isZero() || d.isNegative()) {
            return "hazır";
        }
        long days = d.toDays();
        long hours = d.toHours() % 24;
        long minutes = d.toMinutes() % 60;

        if (days > 0) {
            return days + " gün " + hours + " saat";
        }
        if (hours > 0) {
            return hours + " saat " + minutes + " dk";
        }
        return Math.max(1, minutes) + " dk";
    }
}

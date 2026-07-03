package com.yeditepemc.bixisrewards.listener;

import com.yeditepemc.bixisrewards.BixisRewardsPlugin;
import com.yeditepemc.bixisrewards.gui.RewardGUI;
import com.yeditepemc.bixisrewards.manager.RewardManager;
import com.yeditepemc.bixisrewards.model.RewardData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Ödül sistemi event dinleyicileri:
 * <ul>
 *   <li>Girişte günlük ödül hazırsa oyuncuyu bilgilendirir.</li>
 *   <li>Ödül menüsündeki tıklamaları işler.</li>
 * </ul>
 */
public class RewardListener implements Listener {

    /** BixisCore verisinin async yüklenmesini beklemek için giriş gecikmesi (tick). */
    private static final long JOIN_NOTIFY_DELAY_TICKS = 40L; // ~2 sn

    private final BixisRewardsPlugin plugin;
    private final RewardManager manager;

    public RewardListener(BixisRewardsPlugin plugin, RewardManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // ------------------------------------------------------------------
    //  Giriş bildirimi
    // ------------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // BixisCore veriyi girişte asenkron yükler; kısa bir gecikmeden sonra kontrol et.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            RewardData rd = manager.getRewardData(player);
            if (rd != null && rd.canClaimDaily()) {
                player.sendMessage("§6✦ §eGünlük ödülün hazır! §7Açmak için §f/gunlukodül");
            }
        }, JOIN_NOTIFY_DELAY_TICKS);
    }

    // ------------------------------------------------------------------
    //  Menü tıklamaları
    // ------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof RewardGUI gui)) {
            return;
        }

        // Menüdeki tüm tıklamalar iptal — item alınamaz/taşınamaz.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        // Yalnızca üst (menü) envanterine yapılan tıklamaları işle.
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(gui.getInventory())) {
            return;
        }

        boolean acted = switch (event.getSlot()) {
            case RewardGUI.SLOT_DAILY -> manager.claimDaily(player);
            case RewardGUI.SLOT_WEEKLY -> manager.claimWeekly(player);
            case RewardGUI.SLOT_MONTHLY -> manager.claimMonthly(player);
            default -> false;
        };

        // Talep başarılıysa menüyü güncel durumla yeniden çiz.
        if (acted) {
            RewardData rd = manager.getRewardData(player);
            if (rd != null) {
                gui.render(rd);
            }
        }
    }
}

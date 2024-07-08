package sdr.breeze.simpledailyrewards;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerJoinListener implements Listener {
    private final SimpleDailyRewards plugin;
    private final DbManager dbManager;

    public PlayerJoinListener(SimpleDailyRewards plugin) {
        this.plugin = plugin;
        this.dbManager = plugin.getDbManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("enabled", true)) {
            return;
        }
        plugin.getLogger().info("Player " + event.getPlayer().getName() + " joined.");

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        long lastRewardTime = dbManager.getLastRewardTime(playerId);
        long currentTime = System.currentTimeMillis() / 1000;

        int rewardInterval = plugin.getRewardInterval();
        if (lastRewardTime == 0 || (currentTime - lastRewardTime) >= rewardInterval) {
            boolean rewardGiven = plugin.deliverDailyReward(player);
            if (rewardGiven) {
                plugin.getDbManager().setLastRewardTime(playerId, currentTime);
            }
        }
    }
}



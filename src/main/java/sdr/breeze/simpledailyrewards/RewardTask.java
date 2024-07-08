package sdr.breeze.simpledailyrewards;

import org.bukkit.scheduler.BukkitRunnable;
import java.util.UUID;
import java.util.logging.Logger;

public class RewardTask extends BukkitRunnable {
    private final SimpleDailyRewards plugin;
    private final Logger logger;

    public RewardTask(SimpleDailyRewards plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("enabled", true)) {
            return;
        }

        plugin.getServer().getOnlinePlayers().forEach(player -> {
            UUID playerId = player.getUniqueId();
            long lastRewardTime = plugin.getDbManager().getLastRewardTime(playerId);
            long currentTime = System.currentTimeMillis() / 1000;
            int rewardInterval = plugin.getRewardInterval();

            if (lastRewardTime == 0 || (currentTime - lastRewardTime) >= rewardInterval) {
                logger.info("Executing reward task for player " + player.getName());
                boolean rewardGiven = plugin.deliverDailyReward(player);
                if (rewardGiven) {
                    plugin.getDbManager().setLastRewardTime(playerId, currentTime);
                }
            }
        });
    }
}












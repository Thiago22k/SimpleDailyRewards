package sdr.breeze.simpledailyrewards;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SimpleDailyRewards extends JavaPlugin {

    private DbManager dbManager;
    private BukkitTask rewardTask;

    @Override
    public void onEnable() {
        getLogger().info("SimpleDailyRewards is already working...");

        // Initialize and load configuration
        saveDefaultConfig();
        reloadConfig();

        // Initialize database manager
        this.dbManager = new DbManager(this);
        dbManager.initializeDatabase();

        // Logging events and tasks
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        startRewardTask();

        Objects.requireNonNull(getCommand("sdr")).setExecutor(new SdrCommand(this));
        Objects.requireNonNull(getCommand("deliverdailyreward")).setExecutor(new DeliverDailyRewardCommand(this));
        Objects.requireNonNull(getCommand("sdr")).setTabCompleter(new ToggleTabCompleter());

        getLogger().info("Configuration loaded successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutdown successfully!");
        dbManager.closeConnection();
        stopRewardTask();
    }

    public DbManager getDbManager() {
        return dbManager;
    }

    public void startRewardTask() {
        if (rewardTask != null) {
            rewardTask.cancel();
        }
        rewardTask = new RewardTask(this).runTaskTimer(this, 0, 20 * 60);
    }

    public void stopRewardTask() {
        if (rewardTask != null) {
            rewardTask.cancel();
            rewardTask = null;
        }
    }

    public int getRewardInterval() {
        return getConfig().getInt("rewardInterval");
    }

    public boolean isModeEnabled(String mode) {
        return getConfig().getBoolean("modes." + mode + ".enabled");
    }

    public String getCommandToExecute() {
        return getConfig().getString("modes.command.commandToExecute");
    }

    public int getXpAmount() {
        return getConfig().getInt("modes.xp.amount");
    }

    public String getModeMessage(String mode) {
        return ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getConfig().getString("modes." + mode + ".message")));
    }

    public boolean deliverDailyReward(Player player) {
        boolean rewardGiven = false;

        if (isModeEnabled("command")) {
            if (!hasInventorySpace(player, new ItemStack(Material.DIRT))) {
                String inventoryFullMessage = getConfig().getString("inventoryFullMessage", "Your inventory is full!");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', inventoryFullMessage));
                return false;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getCommandToExecute().replace("%player%", player.getName()));
            player.sendMessage(getModeMessage("command"));
            rewardGiven = true;
        }

        if (isModeEnabled("xp")) {
            int xpAmount = getXpAmount();
            player.giveExp(xpAmount);
            player.sendMessage(getModeMessage("xp"));
            rewardGiven = true;
        }

        if (isModeEnabled("item")) {
            String itemId = getConfig().getString("modes.item.itemId");
            int itemAmount = getConfig().getInt("modes.item.amount");
            assert itemId != null;
            ItemStack itemStack = new ItemStack(Material.valueOf(itemId.toUpperCase()), itemAmount);

            if (hasInventorySpace(player, itemStack)) {
                addItemToInventory(player, itemStack);
                player.sendMessage(getModeMessage("item"));
                rewardGiven = true;
            } else {
                String inventoryFullMessage = getConfig().getString("inventoryFullMessage", "Your inventory is full. Clear some space to receive your reward!");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', inventoryFullMessage));
                return false;
            }
        }

        return rewardGiven;
    }

    public boolean hasInventorySpace(Player player, ItemStack itemStack) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.isSimilar(itemStack) && stack.getAmount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return player.getInventory().firstEmpty() != -1;
    }

    public void addItemToInventory(Player player, ItemStack itemStack) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.isSimilar(itemStack) && stack.getAmount() < stack.getMaxStackSize()) {
                int newAmount = stack.getAmount() + itemStack.getAmount();
                if (newAmount <= stack.getMaxStackSize()) {
                    stack.setAmount(newAmount);
                    return;
                } else {
                    stack.setAmount(stack.getMaxStackSize());
                    itemStack.setAmount(newAmount - stack.getMaxStackSize());
                }
            }
        }
        player.getInventory().addItem(itemStack);
    }

    // Class to handle the command /deliverDailyReward
    private static class DeliverDailyRewardCommand implements CommandExecutor {

        private final SimpleDailyRewards plugin;

        public DeliverDailyRewardCommand(SimpleDailyRewards plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

            if (!plugin.getConfig().getBoolean("enabled")) {
                String disabledRewardMessage = plugin.getConfig().getString("disabledRewardMessage", "The reward system is currently disabled.");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&' , disabledRewardMessage));
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage("Command for users only!");
                return true;
            }

            Player player = (Player) sender;

            // Add verification of the cooldown time before delivering the reward
            UUID playerId = player.getUniqueId();
            long lastRewardTime = plugin.getDbManager().getLastRewardTime(playerId);
            long currentTime = System.currentTimeMillis() / 1000;
            int rewardInterval = plugin.getRewardInterval();

            if (lastRewardTime == 0 || (currentTime - lastRewardTime) >= rewardInterval) {
                boolean rewardGiven = plugin.deliverDailyReward(player);
                if (rewardGiven) {
                    plugin.getDbManager().setLastRewardTime(playerId, currentTime);
                }
            } else {
                String noRewardMessage = plugin.getConfig().getString("noRewardMessage", "You have no rewards at the moment!");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', noRewardMessage));
            }

            return true;
        }
    }

    // Class to handle the command /sdr
    private static class SdrCommand implements CommandExecutor {

        private final SimpleDailyRewards plugin;

        public SdrCommand(SimpleDailyRewards plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Incorrect usage. Use /sdr on, off, reload or reset");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "reload":
                    if (!sender.hasPermission("sdr.reload")) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                        return true;
                    }
                    plugin.reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "SimpleDailyRewards2 reloaded successfully!");
                    break;
                case "reset":
                    if (!sender.hasPermission("sdr.reset")) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                        return true;
                    }
                    resetRewardTimes();
                    sender.sendMessage(ChatColor.GREEN + "Reward times have been reset for all online players.");
                    break;
                case "on":
                case "off":
                    new ToggleCommand(plugin).onCommand(sender, command, label, args);
                    break;
                case "time":
                    if (!sender.hasPermission("sdr.time")) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                        return true;
                    }
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.RED + "Please provide the time in seconds.");
                        return true;
                    }
                    try {
                        int newTime = Integer.parseInt(args[1]);
                        plugin.getConfig().set("rewardInterval", newTime);
                        plugin.saveConfig();
                        sender.sendMessage(ChatColor.GREEN + "Reward interval updated to " + newTime + " seconds.");
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid time. Please provide a numeric value.");
                    }
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Incorrect usage. Use /sdr on, off, reload or reset");
                    break;
            }
            return true;
        }

        private void resetRewardTimes() {
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                UUID playerId = player.getUniqueId();
                plugin.getDbManager().setLastRewardTime(playerId, 0);
            }
        }
    }

    private static class ToggleCommand implements CommandExecutor {

        private final SimpleDailyRewards plugin;

        public ToggleCommand(SimpleDailyRewards plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

            if (!sender.hasPermission("sdr.toggle")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Incorrect usage. Use /sdr on, off, reload or reset");
                return true;
            }

            if (args[0].equalsIgnoreCase("on")) {
                plugin.getConfig().set("enabled", true);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "SimpleDailyRewards has been enabled!");
            } else if (args[0].equalsIgnoreCase("off")) {
                plugin.getConfig().set("enabled", false);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.RED + "SimpleDailyRewards has been disabled!");
            } else {
                sender.sendMessage(ChatColor.RED + "Incorrect usage. Use /sdr on, off, reload or reset");
            }

            return true;
        }
    }

    private static class ToggleTabCompleter implements org.bukkit.command.TabCompleter {

        @Override
        public List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
            if (command.getName().equalsIgnoreCase("sdr")) {
                if (args.length == 1) {
                    return Arrays.asList("on", "off", "reload", "reset", "time");
                }
            }
            return null;
        }
    }

}





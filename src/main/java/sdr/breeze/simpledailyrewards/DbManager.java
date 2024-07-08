package sdr.breeze.simpledailyrewards;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbManager {
    private final SimpleDailyRewards plugin;
    private Connection connection;
    private final Logger logger;

    public DbManager(SimpleDailyRewards plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void initializeDatabase() {
        try {
            File dataFolder = new File(plugin.getDataFolder(), "data.db");
            if (!dataFolder.exists()) {
                boolean created = dataFolder.getParentFile().mkdirs();
                if (created) {
                    plugin.saveResource("data.db", false); // Save the data.db on the server
                }
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
            createTable(); // Call the method to create the table if it does not exist
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not connect to the SQLite database", e);
        }
    }

    public void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error closing the connection to the SQLite database", e);
        }
    }

    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS rewards (" +
                "player_id VARCHAR(36) PRIMARY KEY," +
                "last_reward_time INTEGER NOT NULL" +
                ");";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating the table in the SQLite database", e);
        }
    }

    public void setLastRewardTime(UUID playerId, long timestamp) {
        try {
            PreparedStatement statement = connection.prepareStatement("REPLACE INTO rewards (player_id, last_reward_time) VALUES (?, ?)");
            statement.setString(1, playerId.toString());
            statement.setLong(2, timestamp);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error setting the last reward time for player " + playerId, e);
        }
    }

    public long getLastRewardTime(UUID playerId) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT last_reward_time FROM rewards WHERE player_id = ?");
            statement.setString(1, playerId.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong("last_reward_time");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving the last reward time for player " + playerId, e);
        }
        return 0;
    }
}


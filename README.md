# SimpleDailyRewards

SimpleDailyRewards is a Minecraft plugin developed using Spigot. It provides a daily rewards system for players, allowing server administrators to configure different types of rewards such as items, commands, and experience points.

## Features

- **Daily Rewards**: Players can receive daily rewards automatically.
- **Configurable Rewards**: Rewards can be configured to include items, commands, and experience points.
- **Permissions**: Commands and reward functionality are managed through permissions.
- **Customizable Intervals**: The interval between rewards can be set in the configuration.
- **Inventory Checks**: Ensures players have space in their inventory before delivering item rewards.

## Commands

- `/sdr on`: Enable the rewards system.
- `/sdr off`: Disable the rewards system.
- `/sdr reload`: Reload the plugin configuration.
- `/sdr reset`: Reset the reward time for all online players.
- `/deliverdailyreward`: Manually deliver the daily reward to the executing player.
- `/sdr time [seconds]`: Set the reward interval time in seconds.

## Permissions

- `sdr.time`: Allow usage of all SimpleDailyRewards commands (Deprecated).
- `sdr.reload`: Allow reloading the plugin configuration.
- `sdr.toggle`: Allow enabling or disabling the reward system.
- `sdr.deliver`: Allow players to receive their reward.
- `sdr.reset`: Allow resetting the reward time of connected players.

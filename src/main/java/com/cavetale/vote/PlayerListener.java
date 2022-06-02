package com.cavetale.vote;

import com.cavetale.core.event.connect.ConnectMessageEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
final class PlayerListener implements Listener {
    private final VotePlugin plugin;

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!event.getPlayer().isOnline()) return;
                plugin.checkStoredRewards(event.getPlayer());
            }, 100L);
    }

    @EventHandler
    private void onConnectMessage(ConnectMessageEvent event) {
        if (event.getChannel().equals(VotePlugin.CONNECT_CHANNEL)) {
            UUID uuid = UUID.fromString(event.getPayload());
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) plugin.checkStoredRewards(player);
        }
    }
}

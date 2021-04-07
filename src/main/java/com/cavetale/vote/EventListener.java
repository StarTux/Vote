package com.cavetale.vote;

import com.vexsoftware.votifier.model.VotifierEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
final class EventListener implements Listener {
    final VotePlugin plugin;
    private UUID fireworkUser = null;
    private boolean fireworkDispensed = false;

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        plugin.sqlPlayerOf(event.getPlayer());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.checkStoredRewards(event.getPlayer().getUniqueId());
            }, 100L);
    }

    @EventHandler
    void onVotifier(VotifierEvent event) {
        plugin.onVote(event.getVote());
    }
}

package com.cavetale.vote;

import com.cavetale.worldmarker.MarkedItemUseEvent;
import com.vexsoftware.votifier.model.VotifierEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
final class EventListener implements Listener {
    final VotePlugin plugin;
    private UUID fireworkUser = null;

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

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        final ItemStack item;
        Player player = event.getPlayer();
        if (event.getItem().isSimilar(player.getInventory().getItemInMainHand())) {
            item = player.getInventory().getItemInMainHand();
        } else if (event.getItem().isSimilar(player.getInventory().getItemInOffHand())) {
            item = player.getInventory().getItemInMainHand();
        } else {
            return;
        }
        plugin.candy.onEat(player, item);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMarkedItemUse(MarkedItemUseEvent event) {
        if (!Fireworks.FIREWORK_ID.equals(event.getId())) return;
        if (event.getClick() != ClickType.RIGHT) return;
        fireworkUser = event.getPlayer().getUniqueId();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (fireworkUser == null) return;
        UUID uuid = fireworkUser;
        fireworkUser = null;
        if (!(event.getEntity() instanceof Firework)) return;
        Firework firework = (Firework) event.getEntity();
        if (!uuid.equals(firework.getSpawningEntity())) return;
        plugin.fireworks.onSpawn(firework);
    }
}

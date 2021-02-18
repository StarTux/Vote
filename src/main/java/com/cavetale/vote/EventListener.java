package com.cavetale.vote;

import com.cavetale.worldmarker.item.ItemMarker;
import com.vexsoftware.votifier.model.VotifierEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        switch (event.getAction()) {
        case RIGHT_CLICK_BLOCK: break;
        case RIGHT_CLICK_AIR: break;
        default: return;
        }
        if (event.useItemInHand() == Event.Result.DENY) return;
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        if (!ItemMarker.hasId(item, Fireworks.FIREWORK_ID)) return;
        fireworkUser = event.getPlayer().getUniqueId();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();
        if (!ItemMarker.hasId(item, Fireworks.FIREWORK_ID)) return;
        fireworkDispensed = true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Firework)) return;
        final Firework firework = (Firework) event.getEntity();
        if (fireworkUser != null) {
            UUID uuid = fireworkUser;
            fireworkUser = null;
            if (!uuid.equals(firework.getSpawningEntity())) return;
            plugin.fireworks.onSpawn(firework);
        } else if (fireworkDispensed) {
            fireworkDispensed = false;
            plugin.fireworks.onSpawn(firework);
        }
    }
}

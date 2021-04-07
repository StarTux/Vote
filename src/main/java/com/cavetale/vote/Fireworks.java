package com.cavetale.vote;

import lombok.RequiredArgsConstructor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
final class Fireworks {
    final VotePlugin plugin;
    BukkitRunnable showTask;
    int showTicks;

    Color randomColor() {
        return Color.fromBGR(plugin.random.nextInt(256),
                             plugin.random.nextInt(256),
                             plugin.random.nextInt(256));
    }

    public FireworkEffect.Type randomFireworkEffectType() {
        switch (plugin.random.nextInt(10)) {
        case 0: return FireworkEffect.Type.CREEPER;
        case 1: return FireworkEffect.Type.STAR;
        case 2: case 3: return FireworkEffect.Type.BALL_LARGE;
        case 4: case 5: case 6: return FireworkEffect.Type.BURST;
        case 7: case 8: case 9: return FireworkEffect.Type.BALL;
        default: return FireworkEffect.Type.BALL;
        }
    }

    FireworkMeta randomFireworkMeta() {
        return randomFireworkMeta(randomFireworkEffectType());
    }

    FireworkMeta randomFireworkMeta(FireworkEffect.Type type) {
        FireworkMeta meta = (FireworkMeta) plugin.getServer().getItemFactory()
            .getItemMeta(Material.FIREWORK_ROCKET);
        FireworkEffect.Builder builder = FireworkEffect.builder().with(type);
        int amount = type == FireworkEffect.Type.CREEPER || type == FireworkEffect.Type.STAR
            ? 1 : 3 + plugin.random.nextInt(5);
        for (int i = 0; i < amount; i += 1) {
            builder.withColor(randomColor());
            meta.addEffect(builder.build());
        }
        meta.setPower(1 + plugin.random.nextInt(2));
        return meta;
    }

    void onSpawn(Firework firework) {
        firework.setFireworkMeta(randomFireworkMeta());
    }

    boolean showTick() {
        if (showTicks > 20 * 60) return false;
        showTicks += 1;
        if (showTicks % 20 != 1) return true;
        World world = plugin.getServer().getWorld("spawn");
        if (world == null) {
            plugin.getLogger().warning("World not found: spawn");
            return false;
        }
        long time = world.getTime();
        int amount = 1 + showTicks / 400;
        FireworkEffect.Type type = randomFireworkEffectType();
        for (int i = 0; i < amount; i += 1) {
            Location loc = world.getSpawnLocation()
                .add(plugin.rnd() * 16.0,
                     16.0 + plugin.random.nextDouble() * 16,
                     plugin.rnd() * 16.0);
            Firework entity = world.spawn(loc, Firework.class, e -> {
                    e.setFireworkMeta(randomFireworkMeta(type));
                    e.setSilent(true);
                });
            entity.detonate();
        }
        return true;
    }

    boolean startShow() {
        if (showTask != null && !showTask.isCancelled()) return false;
        showTicks = 0;
        showTask = new BukkitRunnable() {
                @Override public void run() {
                    boolean res = showTick();
                    if (!res) {
                        cancel();
                        showTask = null;
                    }
                }
            };
        showTask.runTaskTimer(plugin, 1L, 1L);
        return true;
    }
}

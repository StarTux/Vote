package com.cavetale.vote;

import com.winthier.spawn.Spawn;
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
    private final VotePlugin plugin;
    private BukkitRunnable showTask;
    private int showTicks;

    private Color randomColor() {
        return Color.fromBGR(plugin.random.nextInt(256),
                             plugin.random.nextInt(256),
                             plugin.random.nextInt(256));
    }

    private FireworkEffect.Type randomFireworkEffectType() {
        switch (plugin.random.nextInt(10)) {
        case 0: return FireworkEffect.Type.CREEPER;
        case 1: return FireworkEffect.Type.STAR;
        case 2: case 3: return FireworkEffect.Type.BALL_LARGE;
        case 4: case 5: case 6: return FireworkEffect.Type.BURST;
        case 7: case 8: case 9: return FireworkEffect.Type.BALL;
        default: return FireworkEffect.Type.BALL;
        }
    }

    private FireworkMeta randomFireworkMeta() {
        return randomFireworkMeta(randomFireworkEffectType());
    }

    private FireworkMeta randomFireworkMeta(FireworkEffect.Type type) {
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

    private boolean showTick() {
        if (showTicks > 20 * 60) return false;
        showTicks += 1;
        if (showTicks % 20 != 1) return true;
        Location location = Spawn.get();
        World world = location.getWorld();
        long time = world.getTime();
        int amount = 1 + showTicks / 400;
        FireworkEffect.Type type = randomFireworkEffectType();
        for (int i = 0; i < amount; i += 1) {
            Location loc = location.clone()
                .add(rnd() * 16.0,
                     16.0 + plugin.random.nextDouble() * 16,
                     rnd() * 16.0);
            Firework entity = world.spawn(loc, Firework.class, e -> {
                    e.setFireworkMeta(randomFireworkMeta(type));
                    e.setSilent(true);
                });
            entity.detonate();
        }
        return true;
    }

    protected boolean startShow() {
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

    private double rnd() {
        return plugin.random.nextBoolean() ? plugin.random.nextDouble() : -plugin.random.nextDouble();
    }
}

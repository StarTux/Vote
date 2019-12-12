package com.cavetale.vote;

import com.cavetale.worldmarker.ItemMarker;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

@RequiredArgsConstructor
final class Fireworks {
    final VotePlugin plugin;
    public static final String FIREWORK_ID = "vote:firework";

    Color randomColor() {
        return Color.fromBGR(plugin.random.nextInt(256),
                             plugin.random.nextInt(256),
                             plugin.random.nextInt(256));
    }

    public FireworkEffect.Type randomFireworkEffectType() {
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        return types[plugin.random.nextInt(types.length)];
    }

    FireworkMeta makeFireworkMeta() {
        FireworkMeta meta = (FireworkMeta) plugin.getServer().getItemFactory()
            .getItemMeta(Material.FIREWORK_ROCKET);
        FireworkEffect.Builder builder = FireworkEffect.builder()
            .flicker(plugin.random.nextBoolean())
            .trail(plugin.random.nextBoolean())
            .with(randomFireworkEffectType())
            .withFade(randomColor());
        int amount = 1 + plugin.random.nextInt(8) + plugin.random.nextInt(8);
        for (int i = 0; i < amount; i += 1) {
            builder.withColor(randomColor());
            meta.addEffect(builder.build());
        }
        meta.setPower(2);
        return meta;
    }

    ItemStack makeFirework(@NonNull Player player) {
        ItemStack item = new ItemStack(Material.FIREWORK_ROCKET);
        FireworkMeta meta = (FireworkMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Voting Firework");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Firework with incredible");
        lore.add(ChatColor.GRAY + "magical effects!");
        lore.add("");
        lore.addAll(plugin.getThanksLore(player.getName()));
        meta.setLore(lore);
        // Hide a token effect so unused stacks stack with used ones.
        meta.addEffect(FireworkEffect.builder().withColor(Color.RED).build());
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        ItemMarker.setId(item, FIREWORK_ID);
        return item;
    }

    void giveFirework(Player player) {
        plugin.giveItem(player, makeFirework(player));
    }

    void onSpawn(Firework firework) {
        firework.setFireworkMeta(makeFireworkMeta());
    }
}

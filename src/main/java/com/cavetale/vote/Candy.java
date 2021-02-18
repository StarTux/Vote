package com.cavetale.vote;

import com.cavetale.worldmarker.item.ItemMarker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@RequiredArgsConstructor
public final class Candy {
    final VotePlugin plugin;
    public static final String CANDY_ID = "vote:candy";
    public static final int TIME = 3 * 20 * 60;
    final List<PotionEffect> effects = Arrays
        .asList(PotionEffectType.ABSORPTION.createEffect(TIME, 1),
                PotionEffectType.CONDUIT_POWER.createEffect(TIME, 0),
                PotionEffectType.DAMAGE_RESISTANCE.createEffect(TIME, 0),
                PotionEffectType.DOLPHINS_GRACE.createEffect(TIME, 0),
                PotionEffectType.FAST_DIGGING.createEffect(TIME, 0),
                PotionEffectType.FIRE_RESISTANCE.createEffect(TIME, 0),
                PotionEffectType.GLOWING.createEffect(TIME, 0),
                PotionEffectType.HEALTH_BOOST.createEffect(TIME, 2),
                PotionEffectType.HERO_OF_THE_VILLAGE.createEffect(TIME, 0),
                PotionEffectType.INCREASE_DAMAGE.createEffect(TIME, 0),
                PotionEffectType.INVISIBILITY.createEffect(TIME, 0),
                PotionEffectType.JUMP.createEffect(TIME, 0),
                PotionEffectType.JUMP.createEffect(TIME, 1),
                PotionEffectType.LUCK.createEffect(TIME, 0),
                PotionEffectType.REGENERATION.createEffect(TIME, 0),
                PotionEffectType.SATURATION.createEffect(TIME, 0),
                PotionEffectType.SLOW_FALLING.createEffect(TIME, 0),
                PotionEffectType.SPEED.createEffect(TIME, 0),
                PotionEffectType.SPEED.createEffect(TIME, 1),
                PotionEffectType.WATER_BREATHING.createEffect(TIME, 0));

    void onEat(Player player, @NonNull ItemStack item) {
        if (!ItemMarker.hasId(item, CANDY_ID)) return;
        int effectIndex = plugin.random.nextInt(effects.size());
        PotionEffect effect = effects.get(effectIndex);
        player.addPotionEffect(effect);
        player.playSound(player.getEyeLocation(),
                         Sound.BLOCK_ENCHANTMENT_TABLE_USE,
                         SoundCategory.MASTER,
                         0.25f, 2.0f);
    }

    ItemStack makeCandy(@NonNull Player player) {
        ItemStack item = new ItemStack(Material.COOKIE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Voting Candy");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Gives unobtainable potion");
        lore.add(ChatColor.GRAY + "effects when eaten.");
        lore.add("");
        lore.addAll(plugin.getThanksLore(player.getName()));
        meta.setLore(lore);
        item.setItemMeta(meta);
        ItemMarker.setId(item, CANDY_ID);
        return item;
    }

    void giveCandy(Player player) {
        plugin.giveItem(player, makeCandy(player));
    }
}

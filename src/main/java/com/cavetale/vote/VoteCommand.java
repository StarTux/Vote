package com.cavetale.vote;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class VoteCommand implements TabExecutor {
    final VotePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;
        plugin.checkStoredRewards(player);
        if (args.length > 0) {
            return onCommand(player, args[0], Arrays.copyOfRange(args, 1, args.length));
        }
        plugin.sendServiceLinks(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                               String label, String[] args) {
        if (args.length == 0) return null;
        String cmd = args[0];
        if (args.length == 1) {
            return Stream.of("hi", "firework")
                .filter(it -> it.contains(cmd))
                .collect(Collectors.toList());
        }
        return null;
    }

    private boolean onCommand(Player player, String cmd, String[] args) {
        switch (cmd) {
        case "hi": case "highscore": case "top": {
            if (args.length != 0) return false;
            plugin.sendHighscore(player);
            return true;
        }
        case "firework": case "fireworks": {
            if (args.length != 0) return false;
            plugin.database.find(SQLMonthly.class).eq("uuid", player.getUniqueId())
                .findUniqueAsync(row -> fireworkCallback(player, row));
            return true;
        }
        default:
            return false;
        }
    }

    private void fireworkCallback(Player player, SQLMonthly row) {
        if (!player.isValid()) return;
        boolean voteKing = row != null ? row.isVoteKing() : false;
        if (!voteKing) {
            player.sendMessage(text("You're not the Vote King!", RED));
            if (!player.hasPermission("vote.admin")) return;
        }
        boolean res = plugin.fireworks.startShow();
        if (!res) {
            player.sendMessage(text("Firework show already started", RED));
        } else {
            player.sendMessage(text("Firework show starting at spawn", GREEN));
        }
    }
}

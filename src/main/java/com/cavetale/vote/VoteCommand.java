package com.cavetale.vote;

import com.winthier.generic_events.GenericEvents;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

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
        voteList(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                               String label, String[] args) {
        if (args.length == 0) return null;
        String cmd = args[0];
        if (args.length == 1) {
            return Stream.of("hi", "firework")
                .filter(it -> it.startsWith(cmd))
                .collect(Collectors.toList());
        }
        return null;
    }

    void voteList(Player player) {
        plugin.sql.find(SQLMonthly.class).eq("uuid", player.getUniqueId())
            .findUniqueAsync(row -> voteListCallback(player, row));
    }

    void voteListCallback(Player player, SQLMonthly row) {
        if (!player.isValid()) return;
        SQLPlayer session = plugin.sqlPlayerOf(player);
        int votes = row != null ? row.votes : 0;
        boolean voteKing = row != null ? row.voteKing : false;
        player.sendMessage("");
        player.sendMessage("" + ChatColor.GOLD
                           + ChatColor.STRIKETHROUGH + "        "
                           + ChatColor.GOLD + "[ "
                           + ChatColor.WHITE + "Voting Links"
                           + ChatColor.GOLD + " ]"
                           + ChatColor.STRIKETHROUGH + "        ");
        player.sendMessage("" + ChatColor.GRAY + ChatColor.ITALIC
                           + "Click to vote."
                           + " Each vote yields a randomized reward.");
        player.sendMessage("" + ChatColor.GRAY + ChatColor.ITALIC
                           + "Votes help us"
                           + " get more players on the server.");
        player.sendMessage("");
        plugin.sendServiceLinks(player);
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "You voted "
                           + ChatColor.BLUE + votes
                           + ChatColor.GRAY + " times this month and "
                           + ChatColor.BLUE + session.allTimeVotes
                           + ChatColor.GRAY + " times overall.");
        if (voteKing) {
            player.sendMessage(ChatColor.GRAY + "You are the "
                               + ChatColor.GOLD + "Vote King"
                               + ChatColor.GRAY + ". Type "
                               + ChatColor.GOLD + "/vote firework"
                               + ChatColor.GRAY + " To start a firework show at spawn.");
        }
        player.sendMessage("");
    }

    boolean onCommand(Player player, String cmd, String[] args) {
        switch (cmd) {
        case "hi": case "highscore": case "top": {
            if (args.length != 0) return false;
            plugin.sql.find(SQLMonthly.class).orderByDescending("votes")
                .findListAsync(rows -> highscoreCallback(player, rows));
            return true;
        }
        case "firework": case "fireworks": {
            if (args.length != 0) return false;
            plugin.sql.find(SQLMonthly.class).eq("uuid", player.getUniqueId())
                .findUniqueAsync(row -> fireworkCallback(player, row));
            return true;
        }
        default:
            return false;
        }
    }

    void highscoreCallback(Player player, List<SQLMonthly> rows) {
        if (!player.isValid()) return;
        player.sendMessage("");
        player.sendMessage("" + ChatColor.GOLD
                           + ChatColor.STRIKETHROUGH + "        "
                           + ChatColor.GOLD + "[ "
                           + ChatColor.WHITE + "Monthly Voting Highscore"
                           + ChatColor.GOLD + " ]"
                           + ChatColor.STRIKETHROUGH + "        ");
        for (int i = 0; i < 10; i += 1) {
            if (rows.size() <= i) break;
            SQLMonthly row = rows.get(i);
            int rank = i + 1;
            String name = GenericEvents.cachedPlayerName(row.uuid);
            if (name == null) name = "N/A";
            ChatColor c = row.voteKing ? ChatColor.GOLD : ChatColor.WHITE;
            player.sendMessage("" + ChatColor.GRAY + rank + ") "
                               + ChatColor.BLUE + row.votes
                               + c + " " + name);
        }
    }

    void fireworkCallback(Player player, SQLMonthly row) {
        if (!player.isValid()) return;
        boolean voteKing = row != null ? row.voteKing : false;
        SQLPlayer session = plugin.sqlPlayerOf(player);
        if (!voteKing) {
            player.sendMessage(ChatColor.RED + "You're not the Vote King!");
            if (!player.hasPermission("vote.admin")) return;
        }
        boolean res = plugin.fireworks.startShow();
        if (!res) {
            player.sendMessage(ChatColor.RED + "Firework show already started.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Firework show starting at spawn.");
        }
    }
}

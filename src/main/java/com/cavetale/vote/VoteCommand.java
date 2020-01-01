package com.cavetale.vote;

import com.winthier.generic_events.GenericEvents;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        SQLPlayer session = plugin.sqlPlayerOf(player);
        player.sendMessage(ChatColor.GRAY + "You voted "
                           + ChatColor.BLUE + session.monthlyVotes
                           + ChatColor.GRAY + " times this month and "
                           + ChatColor.BLUE + session.allTimeVotes
                           + ChatColor.GRAY + " times overall.");
        if (session.tag.voteKing) {
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
            List<SQLPlayer> rows = new ArrayList<>(plugin.sqlPlayers.values());
            Collections.sort(rows, SQLPlayer.HIGHSCORE);
            player.sendMessage("");
            player.sendMessage("" + ChatColor.GOLD
                               + ChatColor.STRIKETHROUGH + "        "
                               + ChatColor.GOLD + "[ "
                               + ChatColor.WHITE + "Monthly Voting Highscore"
                               + ChatColor.GOLD + " ]"
                               + ChatColor.STRIKETHROUGH + "        ");
            for (int i = 0; i < 10; i += 1) {
                if (rows.size() <= i) break;
                SQLPlayer row = rows.get(i);
                int rank = i + 1;
                String name = GenericEvents.cachedPlayerName(row.uuid);
                if (name == null) name = "N/A";
                ChatColor c = row.tag.voteKing ? ChatColor.GOLD : ChatColor.WHITE;
                player.sendMessage("" + ChatColor.GRAY + rank + ") "
                                   + ChatColor.BLUE + row.monthlyVotes
                                   + c + " " + name);
            }
            player.sendMessage("");
            return true;
        }
        case "firework": case "fireworks": {
            if (args.length != 0) return false;
            SQLPlayer session = plugin.sqlPlayerOf(player);
            if (!session.tag.voteKing) {
                player.sendMessage(ChatColor.RED + "You're not the Vote King");
                if (!player.hasPermission("vote.admin")) return true;
            }
            boolean res = plugin.fireworks.startShow();
            if (!res) {
                player.sendMessage(ChatColor.RED + "Firework show already started.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Firework show starting at spawn.");
            }
            return true;
        }
        default:
            return false;
        }
    }
}

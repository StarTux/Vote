package com.cavetale.vote;

import com.winthier.generic_events.GenericEvents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class VoteCommand implements CommandExecutor {
    final VotePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;
        plugin.checkStoredRewards(player);
        if (args.length == 0) {
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
            player.sendMessage("");
            plugin.sendServiceLinks(player);
            player.sendMessage("");
            SQLPlayer session = plugin.sqlPlayerOf(player);
            player.sendMessage(ChatColor.GRAY + "You voted "
                               + ChatColor.BLUE + session.monthlyVotes
                               + ChatColor.GRAY + " times this month and "
                               + ChatColor.BLUE + session.allTimeVotes
                               + ChatColor.GRAY + " times overall.");
            player.sendMessage("");
            return true;
        } else if (args.length == 1 && args[0].equals("hi")) {
            List<SQLPlayer> rows = new ArrayList<>(plugin.sqlPlayers.values());
            Collections.sort(rows, (a, b) -> Integer.compare(b.monthlyVotes,
                                                             a.monthlyVotes));
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
                player.sendMessage("" + ChatColor.GRAY + rank + ") "
                                   + ChatColor.BLUE + row.monthlyVotes
                                   + ChatColor.WHITE + " " + name);
            }
            player.sendMessage("");
            return true;
        }
        return false;
    }
}

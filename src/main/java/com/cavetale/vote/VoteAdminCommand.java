package com.cavetale.vote;

import com.vexsoftware.votifier.model.Vote;
import com.winthier.generic_events.GenericEvents;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class VoteAdminCommand implements CommandExecutor {
    final VotePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        String[] argl = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0]) {
        case "reload": return reloadCommand(sender, argl);
        case "service": case "services":
            return serviceCommand(sender, argl);
        case "reward": return rewardCommand(sender, argl);
        case "simulate": return simulateCommand(sender, argl);
        case "cleartimes": return clearTimesCommand(sender, argl);
        case "resetmonth": return resetMonthCommand(sender, argl);
        case "king": return voteKingCommand(sender, argl);
        case "addvotes": return addVotesCommand(sender, argl);
        case "addtotal": return addTotalCommand(sender, argl);
        default: return false;
        }
    }

    boolean reloadCommand(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.loadDatabases();
        sender.sendMessage("Databases reloaded.");
        return true;
    }

    boolean rewardCommand(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) return false;
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found: " + args[0]);
            return true;
        }
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                return false;
            }
            if (amount < 1) return false;
        }
        plugin.giveReward(target, amount);
        sender.sendMessage("Reward x" + amount + " given to "
                           + target.getName() + ".");
        return true;
    }

    boolean simulateCommand(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        String name = args[0];
        String service = args[1];
        Vote vote = new Vote(service, name, "localhost",
                             "" + Instant.now().getEpochSecond());
        sender.sendMessage("Simulating vote for " + name
                           + " from " + service + "...");
        plugin.onVote(vote);
        return true;
    }

    boolean clearTimesCommand(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found: " + args[0]);
            return true;
        }
        SQLPlayer session = plugin.sqlPlayerOf(target);
        session.tag.lastVotes.clear();
        plugin.save(session);
        sender.sendMessage("Service validation times cleared for "
                           + target.getName() + ".");
        return true;
    }

    boolean serviceCommand(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        switch (args[0]) {
        case "list": {
            if (args.length != 1) return false;
            sender.sendMessage(plugin.sqlServices.size() + " services:");
            for (SQLService service : plugin.sqlServices) {
                sender.sendMessage("- name: " + service.name);
                sender.sendMessage("  url: " + service.url);
                sender.sendMessage("  displayName: " + service.displayName);
            }
            return true;
        }
        case "add": {
            if (args.length < 4) return false;
            String name = args[1];
            String url = args[2];
            StringBuilder sb = new StringBuilder(args[3]);
            for (int i = 4; i < args.length; i += 1) {
                sb.append(" ").append(args[i]);
            }
            String displayName = sb.toString();
            SQLService service = new SQLService(name, displayName, url);
            plugin.sql.save(service);
            plugin.sqlServices.add(service);
            sender.sendMessage("Service added: " + name + ", " + url + ", "
                               + plugin.colorize(displayName));
            return true;
        }
        case "remove": {
            if (args.length != 2) return false;
            SQLService service = plugin.findService(args[1]);
            if (service == null) {
                sender.sendMessage("Service not found: " + args[1] + "!");
                return true;
            }
            plugin.sql.delete(service);
            plugin.sqlServices.remove(service);
            sender.sendMessage("Service removed: " + service.name);
            return true;
        }
        default:
            return false;
        }
    }

    boolean resetMonthCommand(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage("Triggering a month reset...");
        plugin.state.nextResetTime = 0L;
        plugin.checkTime();
        return true;
    }

    boolean voteKingCommand(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String name = args[0];
        UUID uuid = GenericEvents.cachedPlayerUuid(name);
        if (uuid == null) {
            sender.sendMessage("Player not found: " + name);
            return true;
        }
        SQLMonthly row = plugin.sql.find(SQLMonthly.class)
            .eq("uuid", uuid).findUnique();
        if (row == null) row = new SQLMonthly(uuid);
        row.voteKing = !row.voteKing;
        plugin.sql.save(row);
        if (row.voteKing) {
            sender.sendMessage("Player was made Vote King: " + name);
        } else {
            sender.sendMessage("Player lost Vote King status: " + name);
        }
        return true;
    }

    boolean addVotesCommand(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) return false;
        String name = args[0];
        UUID uuid = GenericEvents.cachedPlayerUuid(name);
        if (uuid == null) {
            sender.sendMessage("Player not found: " + name);
            return true;
        }
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                return false;
            }
        }
        SQLPlayer session = plugin.sqlPlayerOf(uuid);
        plugin.addVotes(session, amount);
        sender.sendMessage("Added " + amount + " vote(s) to " + name + ".");
        return true;
    }

    boolean addTotalCommand(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) return false;
        String name = args[0];
        UUID uuid = GenericEvents.cachedPlayerUuid(name);
        if (uuid == null) {
            sender.sendMessage("Player not found: " + name);
            return true;
        }
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                return false;
            }
        }
        SQLPlayer session = plugin.sqlPlayerOf(uuid);
        session.allTimeVotes += amount;
        plugin.save(session);
        sender.sendMessage("Adjusted total votes of " + name + " by " + amount + ".");
        return true;
    }
}

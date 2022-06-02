package com.cavetale.vote;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.winthier.playercache.PlayerCache;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class VoteAdminCommand extends AbstractCommand<VotePlugin> {
    protected VoteAdminCommand(final VotePlugin plugin) {
        super(plugin, "voteadmin");
    }

    @Override
    protected void onEnable() {
        CommandNode playerNode = rootNode.addChild("player")
            .description("Player subcommands");
        playerNode.addChild("reward").arguments("<player> [amount]")
            .description("Trigger player reward")
            .completers(PlayerCache.NAME_COMPLETER,
                        CommandArgCompleter.integer(i -> i > 0))
            .senderCaller(this::playerReward);
        playerNode.addChild("simulate").arguments("<name> <service>")
            .description("Simulate vote")
            .completers(PlayerCache.NAME_COMPLETER,
                        CommandArgCompleter.EMPTY)
            .senderCaller(this::playerSimulate);
        playerNode.addChild("add").arguments("<player> [amount]")
            .description("Add votes (and rewards) to player")
            .completers(PlayerCache.NAME_COMPLETER,
                        CommandArgCompleter.integer(i -> i != 0))
            .senderCaller(this::playerAdd);
        playerNode.addChild("check").denyTabCompletion()
            .description("Run a check for outstanding rewards")
            .senderCaller(this::playerCheck);
        playerNode.addChild("trophies").arguments("<year> <month>")
            .description("Award trophies after the fact")
            .completers(CommandArgCompleter.integer(i -> i > 2012),
                        CommandArgCompleter.integer(i -> i >= 1 && i <= 12))
            .senderCaller(this::playerTrophies);
        CommandNode serviceNode = rootNode.addChild("service")
            .description("Service subcommands");
        serviceNode.addChild("list").denyTabCompletion()
            .description("List services")
            .senderCaller(this::serviceList);
        serviceNode.addChild("add").arguments("<name> <url> <display>")
            .description("Add service")
            .completers(CommandArgCompleter.EMPTY,
                        CommandArgCompleter.EMPTY,
                        CommandArgCompleter.EMPTY,
                        CommandArgCompleter.REPEAT)
            .senderCaller(this::serviceAdd);
        serviceNode.addChild("remove").arguments("<name>")
            .description("Remove service")
            .completers(CommandArgCompleter.EMPTY)
            .senderCaller(this::serviceRemove);
    }

    private boolean playerReward(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) return false;
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            throw new CommandWarn("Player not found: " + args[0]);
        }
        final int amount = args.length >= 2
            ? CommandArgCompleter.requireInt(args[1], i -> i > 0)
            : 1;
        plugin.giveReward(target, amount);
        sender.sendMessage(text("Rewarded " + target.getName() + " " + amount + " time(s)", AQUA));
        return true;
    }

    private boolean playerSimulate(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        String service = args[1];
        plugin.onVote(service, target.name, "localhost", "" + Instant.now().getEpochSecond());
        sender.sendMessage(text("Simulating vote for " + target.name + " from " + service + "...", AQUA));
        return true;
    }

    private boolean playerAdd(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        int amount = args.length >= 2
            ? CommandArgCompleter.requireInt(args[1], i -> i != 0)
            : 1;
        plugin.addVotes(target.uuid, amount, () -> {
                sender.sendMessage(text("Adjusted votes of " + target.name + " by " + amount, AQUA));
            });
        return true;
    }

    private void playerCheck(CommandSender sender) {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.checkStoredRewards(player);
            count += 1;
        }
        sender.sendMessage(text("Ran " + count + " check(s)", AQUA));
    }

    private boolean playerTrophies(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        int year = CommandArgCompleter.requireInt(args[0], i ->  i > 2012);
        int month = CommandArgCompleter.requireInt(args[1], i -> i >= 1 && i <= 12);
        plugin.database.find(SQLLog.class)
            .eq("valid", true)
            .findListAsync(logs -> {
                    GregorianCalendar calendar = new GregorianCalendar();
                    Map<UUID, Integer> scores = new HashMap<>();
                    for (SQLLog log : logs) {
                        calendar.setTime(log.getTime());
                        if (year != calendar.get(Calendar.YEAR)) continue;
                        if (month != calendar.get(Calendar.MONTH) + 1) continue;
                        scores.put(log.getUserId(), scores.getOrDefault(log.getUserId(), 0) + 1);
                    }
                    int count = plugin.reward(year, month, scores);
                    sender.sendMessage(text(count + " players awarded", AQUA));
                });
        return true;
    }

    private void serviceList(CommandSender sender) {
        plugin.database.find(SQLService.class).findListAsync(rows -> {
                sender.sendMessage(text(rows.size() + " service(s)", AQUA));
                for (SQLService row : rows) {
                    sender.sendMessage(text("- name: " + row.getName(), YELLOW));
                    sender.sendMessage(text("  enabled: " + row.isEnabled(), YELLOW));
                    sender.sendMessage(text("  priority: " + row.getPriority(), YELLOW));
                    sender.sendMessage(text("  url: " + row.getUrl(), YELLOW));
                    sender.sendMessage(text("  displayName: " + row.getDisplayName(), YELLOW));
                }
            });
    }

    private boolean serviceAdd(CommandSender sender, String[] args) {
        if (args.length < 3) return false;
        final String name = args[0];
        final String url = args[1];
        final String displayName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        final SQLService service = new SQLService(name, displayName, url);
        plugin.database.saveAsync(service, res -> {
                if (res != 0) {
                    sender.sendMessage(text("Service added: " + service, AQUA));
                } else {
                    sender.sendMessage(text("Failed: " + service, RED));
                }
            });
        return true;
    }

    private boolean serviceRemove(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String name = args[0];
        plugin.database.find(SQLService.class)
            .eq("name", name)
            .deleteAsync(res -> {
                    if (res != 0) {
                        sender.sendMessage(text("Service removed: " + name, AQUA));
                    } else {
                        sender.sendMessage(text("Service not found: " + name, RED));
                    }
                });
        return true;
    }
}

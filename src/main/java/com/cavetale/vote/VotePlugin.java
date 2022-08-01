package com.cavetale.vote;

import com.cavetale.core.command.RemotePlayer;
import com.cavetale.core.connect.Connect;
import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.connect.ServerCategory;
import com.cavetale.fam.trophy.Highscore;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import com.winthier.playercache.PlayerCache;
import com.winthier.sql.SQLDatabase;
import java.text.DateFormatSymbols;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.openUrl;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class VotePlugin extends JavaPlugin {
    protected static final String CONNECT_CHANNEL = "vote:player_vote";
    protected final SQLDatabase database = new SQLDatabase(this);
    protected final Random random = ThreadLocalRandom.current();
    protected final Fireworks fireworks = new Fireworks(this);
    protected final VoteAdminCommand adminCommand = new VoteAdminCommand(this);
    protected final VoteCommand voteCommand = new VoteCommand(this);
    private Timer timer;

    @Override
    public void onEnable() {
        database.registerTables(List.of(SQLLog.class,
                                        SQLPlayer.class,
                                        SQLMonthly.class,
                                        SQLService.class,
                                        SQLLastVote.class));
        database.createAllTables();
        adminCommand.enable();
        getCommand("vote").setExecutor(voteCommand);
        if (NetworkServer.current() == NetworkServer.HUB) {
            timer = new Timer(this);
            timer.enable();
            getLogger().info("Timer enabled");
        } else {
            getLogger().info("Timer disabled");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("Votifier")) {
            new VotifierListener(this).enable();
            getLogger().info("Votifier enabled");
        } else {
            getLogger().info("Votifier disabled");
        }
        if (ServerCategory.current().isSurvival()) {
            new PlayerListener(this).enable();
        }
    }

    public void giveReward(@NonNull Player player, final int amount) {
        TextComponent.Builder announcement = Component.text().color(WHITE)
            .clickEvent(runCommand("/vote"))
            .hoverEvent(showText(Component.text("/vote", GREEN)));
        for (int i = 0; i < amount; i += 1) {
            Mytems mytems = random.nextBoolean()
                ? Mytems.VOTE_CANDY
                : Mytems.VOTE_FIREWORK;
            mytems.giveItemStack(player, 1);
            announcement.append(mytems.component);
        }
        player.playSound(player.getLocation(),
                         Sound.ENTITY_PLAYER_LEVELUP,
                         SoundCategory.MASTER,
                         0.25f, 2.0f);
        if (amount > 1) {
            announcement
                .append(Component.text(" " + player.getName() + " received a reward. Thanks for voting ", GREEN))
                .append(Component.text("" + amount, WHITE))
                .append(Component.text(" times!", GREEN));
        } else {
            announcement
                .append(Component.text(" " + player.getName() + " received a reward. Thanks for voting!", GREEN));
        }
        if (ServerCategory.current() == ServerCategory.SURVIVAL_TEST) {
            player.sendMessage(text("Sending message privately!", DARK_GRAY, ITALIC));
            player.sendMessage(announcement);
        } else {
            for (RemotePlayer target : Connect.get().getRemotePlayers()) {
                target.sendMessage(announcement);
            }
        }
    }

    public void onVote(final String service, final String username, final String address, final String timestamp) {
        if (service == null || username == null) return;
        final PlayerCache player = PlayerCache.forName(username);
        database.find(SQLService.class).eq("name", service).findUniqueAsync(serviceRow -> {
                final boolean valid = serviceRow != null && serviceRow.enabled && player != null;
                Instant now = Instant.now();
                SQLLog log = new SQLLog(username, player.uuid, new Date(), valid, service, address, timestamp);
                database.insert(log);
                getLogger().info("Vote from " + username + " via " + service + " valid=" + valid);
                if (!valid) return;
                // Warn about mismatch
                database.find(SQLLastVote.class)
                    .eq("player", player.uuid)
                    .eq("service", service).findUniqueAsync(lastVoteRow -> {
                            if (lastVoteRow == null || lastVoteRow.canVote(now)) return;
                            getLogger().warning("Vote timing mismatch:"
                                                + " service=" + service
                                                + " last=" + lastVoteRow.getLastVote()
                                                + " now=" + now
                                                + " next=" + lastVoteRow.getNextVote());
                        });
                database.saveAsync(new SQLLastVote(player.uuid, service), Set.of("time"), null);
                addVotes(player.uuid, 1, () -> {
                        Connect.get().broadcastMessageToAll(CONNECT_CHANNEL, player.uuid.toString());
                    });
            });
    }

    /**
     * Add votes to player and monthly.
     */
    public void addVotes(UUID uuid, int amount, Runnable callback) {
        database.update(SQLPlayer.class)
            .add("allTimeVotes", amount)
            .add("storedRewards", amount)
            .where(c -> c.eq("uuid", uuid))
            .async(res -> {
                    if (res != 0) return;
                    database.insertAsync(new SQLPlayer(uuid, amount), null);
                });
        database.update(SQLMonthly.class)
            .add("votes", amount)
            .where(c -> c.eq("uuid", uuid))
            .async(res -> {
                    if (res != 0) {
                        callback.run();
                    } else {
                        database.insertAsync(new SQLMonthly(uuid, amount), res2 -> callback.run());
                    }
                });
    }

    public void sendServiceLinks(Player player) {
        final UUID uuid = player.getUniqueId();
        database.scheduleAsyncTask(() -> {
                List<SQLService> serviceRows = database.find(SQLService.class).findList();
                SQLPlayer playerRow = database.find(SQLPlayer.class).eq("uuid", uuid).findUnique();
                SQLMonthly monthlyRow = database.find(SQLMonthly.class).eq("uuid", uuid).findUnique();
                Map<String, SQLLastVote> lastVoteRows = new HashMap<>();
                for (SQLLastVote lastVoteRow : database.find(SQLLastVote.class).eq("player", uuid).findList()) {
                    lastVoteRows.put(lastVoteRow.getService(), lastVoteRow);
                }
                Bukkit.getScheduler().runTask(this, () -> listCallback(player, serviceRows,
                                                                       playerRow, monthlyRow, lastVoteRows));
            });
    }

    private void listCallback(Player player,
                              List<SQLService> services, SQLPlayer playerRow, SQLMonthly monthlyRow,
                              Map<String, SQLLastVote> lastVoteRows) {
        Instant now = Instant.now();
        final int totalVotes = playerRow != null ? playerRow.getAllTimeVotes() : 0;
        final int monthlyVotes = monthlyRow != null ? monthlyRow.getVotes() : 0;
        final boolean voteKing = monthlyRow != null ? monthlyRow.isVoteKing() : false;
        List<ComponentLike> lines = new ArrayList<>();
        lines.add(empty());
        lines.add(join(noSeparators(),
                       text("        ", GOLD, STRIKETHROUGH),
                       text("[ ", GOLD),
                       text("Voting Links", WHITE),
                       text(" ]", GOLD),
                       text("        ", GOLD, STRIKETHROUGH)));
        lines.add(text("Click to vote. Each vote yields a randomized reward.", GRAY, ITALIC));
        lines.add(text("Votes help us get more players on the server.", GRAY, ITALIC));
        lines.add(empty());
        for (SQLService service : services) {
            if (!service.enabled) continue;
            SQLLastVote lastVoteRow = lastVoteRows.get(service.getName());
            final boolean can = lastVoteRow != null
                ? lastVoteRow.canVote(now)
                : true;
            TextComponent.Builder line = text().content("  ");
            if (!can) {
                line.append(text().color(WHITE)
                            .content("(")
                            .append(Component.text("\u2714", GREEN))
                            .append(Component.text(")")));
            } else {
                line.append(Component.text("(  )", GRAY));
            }
            line.append(space());
            line.append(text(service.getDisplayName(), (can ? GREEN : GRAY)));
            line.insertion(service.getUrl());
            line.hoverEvent(showText(Component.text(service.getUrl(), AQUA, UNDERLINED)));
            line.clickEvent(openUrl(service.getUrl()));
            if (!can) { // lastVoteRow != null!
                line.append(Component.space());
                long cooldown = Math.max(0L, lastVoteRow.getNextVote().getEpochSecond() - now.getEpochSecond());
                line.append(Component.text("(in " + timeFormat(cooldown) + ")", GRAY, ITALIC));
            }
            lines.add(line);
        }
        lines.add(empty());
        lines.add(join(noSeparators(),
                       text("You voted "),
                       text(monthlyVotes, GREEN),
                       text(" times this month and "),
                       text(totalVotes),
                       text(" times overall."))
                  .color(GRAY));
        if (voteKing) {
            String cmd = "/vote firework";
            lines.add(join(noSeparators(),
                           text("You are the "),
                           text("Vote King", GOLD),
                           text(". Type "),
                           text("/vote firework", GOLD),
                           text(" To start a firework show at spawn."))
                      .color(GRAY)
                      .hoverEvent(showText(text(cmd, GOLD)))
                      .clickEvent(runCommand(cmd)));
        }
        player.sendMessage(join(separator(newline()), lines));
    }

    protected void sendHighscore(Player player) {
        database.find(SQLMonthly.class).findListAsync(rows -> highscoreCallback(player, rows));
    }

    private void highscoreCallback(Player player, List<SQLMonthly> rows) {
        if (!player.isOnline()) return;
        Map<UUID, Integer> scores = new HashMap<>();
        for (SQLMonthly row : rows) {
            scores.put(row.getUuid(), row.getVotes());
        }
        List<Component> lines = new ArrayList<>();
        lines.add(empty());
        lines.add(join(noSeparators(),
                       text("        ", GOLD, STRIKETHROUGH),
                       text("[ ", GOLD),
                       text("Voting Highscore", WHITE),
                       text(" ]", GOLD),
                       text("        ", GOLD, STRIKETHROUGH)));
        lines.addAll(Highscore.sidebar(Highscore.of(scores)));
        player.sendMessage(join(separator(newline()), lines));
    }

    protected String timeFormat(long seconds) {
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes %= 60L;
        seconds %= 60L;
        return String.format(hours + "h" + " " + minutes + "m");
    }

    protected void checkStoredRewards(Player player) {
        if (!ServerCategory.current().isSurvival()) return;
        database.find(SQLPlayer.class)
            .eq("uuid", player.getUniqueId())
            .findUniqueAsync(row -> {
                    if (!player.isOnline()) return;
                    if (row == null) return;
                    int amount = row.getStoredRewards();
                    if (amount <= 0) return;
                    database.update(SQLPlayer.class)
                        .row(row)
                        .atomic("storedRewards", 0)
                        .async(updateCount -> {
                                if (updateCount == 0) return;
                                giveReward(player, amount);
                            });
                });
    }

    protected int reward(int year, int month, Map<UUID, Integer> scores) {
        String monthTxt = DateFormatSymbols.getInstance().getMonths()[month - 1];
        String txt = "Voting in " + monthTxt + " " + year;
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, month == 12 ? year + 1 : year);
        cal.set(Calendar.MONTH, (month % 12));
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return Highscore.reward(scores,
                                "vote_monthly",
                                TrophyCategory.VOTE,
                                text(txt, GOLD),
                                hi -> ("You voted " + hi.score + " time" + (hi.score == 1 ? "" : "s")
                                       + " in the month of " + monthTxt + " " + year),
                                row -> row.setTime(cal.getTime()));
    }
}

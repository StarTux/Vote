package com.cavetale.vote;

import com.cavetale.mytems.Mytems;
import com.google.gson.Gson;
import com.vexsoftware.votifier.model.Vote;
import com.winthier.generic_events.GenericEvents;
import com.winthier.sql.SQLDatabase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class VotePlugin extends JavaPlugin {
    // Utility
    SQLDatabase sql;
    EventListener eventListener = new EventListener(this);
    static Gson gson;
    static final long DAY_SECONDS = 60L * 60L * 24L;
    Json json = new Json(this);
    Random random = new Random();
    // Rewards
    Fireworks fireworks = new Fireworks(this);
    // Databases
    final Map<UUID, SQLPlayer> sqlPlayers = new HashMap<>();
    final List<SQLService> sqlServices = new ArrayList<>();
    // Commands
    VoteAdminCommand adminCommand = new VoteAdminCommand(this);
    VoteCommand voteCommand = new VoteCommand(this);
    // State
    static final String STATE_PATH = "state.json";
    State state;

    @Override
    public void onEnable() {
        gson = new Gson();
        sql = new SQLDatabase(this);
        sql.registerTables(SQLLog.class,
                           SQLPlayer.class,
                           SQLMonthly.class,
                           SQLService.class);
        sql.createAllTables();
        getServer().getPluginManager().registerEvents(eventListener, this);
        getCommand("voteadmin").setExecutor(adminCommand);
        getCommand("vote").setExecutor(voteCommand);
        loadDatabases();
        state = json.load(STATE_PATH, State.class);
        if (state == null) {
            state = new State();
            state.setCurrentMonth();
            json.save(STATE_PATH, state, true);
        }
        getServer().getScheduler().runTaskTimer(this, this::checkTime, 1200L, 1200L);
    }

    void loadDatabases() {
        sqlPlayers.clear();
        for (SQLPlayer row : sql.find(SQLPlayer.class).findList()) {
            row.unpack();
            sqlPlayers.put(row.uuid, row);
        }
        sqlServices.clear();
        for (SQLService row : sql.find(SQLService.class).findList()) {
            sqlServices.add(row);
        }
        Collections.sort(sqlServices, (b, a) -> Integer.compare(a.getPriority(), b.getPriority()));
    }

    SQLPlayer sqlPlayerOf(@NonNull UUID uuid) {
        SQLPlayer row = sqlPlayers.get(uuid);
        if (row == null) {
            row = new SQLPlayer(uuid);
            save(row);
            sqlPlayers.put(uuid, row);
        }
        return row;
    }

    SQLPlayer sqlPlayerOf(@NonNull Player player) {
        return sqlPlayerOf(player.getUniqueId());
    }

    SQLService findService(@NonNull String name) {
        for (SQLService row : sqlServices) {
            if (name.equals(row.name)) return row;
        }
        return null;
    }

    void save(SQLPlayer row) {
        row.pack();
        if (isEnabled()) {
            sql.saveAsync(row, null);
        } else {
            sql.save(row);
        }
    }

    void giveReward(@NonNull Player player, final int amount) {
        for (int i = 0; i < amount; i += 1) {
            if (random.nextBoolean()) {
                Mytems.VOTE_CANDY.giveItemStack(player, 1);
            } else {
                Mytems.VOTE_FIREWORK.giveItemStack(player, 1);
            }
        }
        player.playSound(player.getEyeLocation(),
                         Sound.ENTITY_PLAYER_LEVELUP,
                         SoundCategory.MASTER,
                         0.25f, 2.0f);
        ChatColor c = ChatColor.WHITE;
        String ann = amount > 1
            ? player.getName() + " received a reward. Thanks for voting "
            + ChatColor.BLUE + amount + c + " times!"
            : player.getName() + " received a reward. Thanks for voting!";
        ComponentBuilder cb = new ComponentBuilder(ann).color(c);
        cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vote"));
        cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                TextComponent.fromLegacyText(c + "/vote")));
        for (Player target : getServer().getOnlinePlayers()) {
            target.spigot().sendMessage(cb.create());
        }
    }

    boolean onVote(@NonNull Vote vote) {
        getLogger().info(vote.toString());
        final String service = vote.getServiceName();
        if (service == null) return false;
        final String username = vote.getUsername();
        if (username == null) return false;
        final String address = vote.getAddress();
        final String timestamp = vote.getTimeStamp();
        final Player player = getServer().getPlayerExact(username);
        final UUID uuid = player != null
            ? player.getUniqueId()
            : GenericEvents.cachedPlayerUuid(username);
        final String name;
        if (player != null) {
            name = player.getName();
        } else if (uuid != null) {
            String tmp = GenericEvents.cachedPlayerName(uuid);
            name = tmp != null ? tmp : username;
        } else {
            name = username;
        }
        // Check validity (no other vote within 24h)
        final SQLPlayer session = uuid != null ? sqlPlayerOf(uuid) : null;
        SQLService serviceRow = findService(service);
        final boolean valid = session != null && serviceRow != null && serviceRow.enabled;
        long now = Instant.now().getEpochSecond();
        SQLLog log = new SQLLog(name, uuid, new Date(now * 1000L), valid,
                                service, address, timestamp);
        sql.saveAsync(log, null);
        getLogger().info("Vote from " + name + " via " + service + " valid=" + valid);
        if (!valid) return false; // Last exit
        // Warn about mismatch
        if (!session.canVote(service, now)) {
            getLogger().warning("Vote timing mismatch:"
                                + " service=" + service
                                + " last=" + new Date(session.getLastVoteEpoch(service) * 1000L)
                                + " now=" + new Date(now * 1000L)
                                + " next=" + new Date(session.getNextVote(service) * 1000L));
        }
        session.setLastVoteEpoch(service, now);
        addVotes(session, 1); // Saves session implicitly
        return true;
    }

    /**
     * Saves session implicitly. Gives out rewards if player is online
     * or adds to storedRewards.
     */
    void addVotes(SQLPlayer session, int amount) {
        String tableName = sql.getTable(SQLMonthly.class).getTableName();
        String statement = "INSERT INTO `" + tableName + "`"
            + " (`uuid`, `votes`, `vote_king`)"
            + " VALUES ('" + session.uuid + "', " + amount + ", 0)"
            + " ON DUPLICATE KEY UPDATE `votes` = `votes` + " + amount;
        sql.executeUpdateAsync(statement, null);
        session.allTimeVotes += amount;
        final int total;
        final Player player = getServer().getPlayer(session.uuid);
        if (player != null) {
            total = amount + session.storedRewards;
            session.storedRewards = 0;
        } else {
            total = 0;
            session.storedRewards += amount;
        }
        save(session);
        if (player != null && total > 0) {
            giveReward(player, total);
        }
    }

    static String colorize(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    void sendServiceLinks(Player player) {
        SQLPlayer session = sqlPlayerOf(player);
        long now = Instant.now().getEpochSecond();
        long yesterday = now - DAY_SECONDS;
        ComponentBuilder cb;
        for (SQLService service : sqlServices) {
            if (!service.enabled) continue;
            long lastVote = session.getLastVoteEpoch(service.name);
            boolean can = session.canVote(service.name, now);
            cb = new ComponentBuilder("  ");
            if (!can) {
                cb.append(ChatColor.WHITE + "("
                          + ChatColor.GREEN + "\u2714"
                          + ChatColor.WHITE + ")");
            } else {
                cb.append("(  )").color(ChatColor.GRAY);
            }
            cb.append(" ").reset();
            cb.append(colorize(service.displayName));
            cb.color(can ? ChatColor.YELLOW : ChatColor.GRAY);
            cb.insertion(service.url);
            BaseComponent[] tooltip = TextComponent
                .fromLegacyText("" + ChatColor.BLUE
                                + ChatColor.UNDERLINE
                                + service.url);
            cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip));
            cb.event(new ClickEvent(ClickEvent.Action.OPEN_URL, service.url));
            if (!can) {
                cb.append(" ").reset();
                long next = session.getNextVote(service.name);
                cb.append("(in " + timeFormat(next - now) + ")")
                    .color(ChatColor.GRAY).italic(true);
            }
            player.spigot().sendMessage(cb.create());
        }
    }

    String timeFormat(long seconds) {
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%02d:%02d", hours % 60, minutes % 60);
    }

    void checkTime() {
        if (System.currentTimeMillis() <= state.nextResetTime) return;
        int oldMonth = state.currentMonth;
        state.setCurrentMonth();
        json.save(STATE_PATH, state, true);
        getLogger().info("Rolling over the month: " + oldMonth
                         + " -> " + state.currentMonth);
        List<SQLMonthly> rows = sql.find(SQLMonthly.class)
            .orderByDescending("votes").findList();
        boolean king = false;
        UUID kingUuid = null;
        String kingName = null;
        SQLMonthly kingRow = rows.isEmpty() ? null : rows.get(0);
        if (kingRow != null) {
            king = true;
            kingUuid = kingRow.uuid;
            kingName = GenericEvents.cachedPlayerName(kingRow.uuid);
            getLogger().info("New vote king: " + kingName + ".");
        }
        sql.find(SQLMonthly.class).delete();
        if (kingUuid != null) {
            SQLMonthly row = new SQLMonthly(kingUuid);
            row.voteKing = true;
            sql.save(row);
            String cmd = "titles unlockset " + kingName + " VoteKing";
            getLogger().info("Issuing command: " + cmd);
            getServer().dispatchCommand(getServer().getConsoleSender(), cmd);
        }
    }

    void checkStoredRewards(UUID uuid) {
        Player player = getServer().getPlayer(uuid);
        if (player == null) return;
        checkStoredRewards(player);
    }

    void checkStoredRewards(Player player) {
        SQLPlayer session = sqlPlayerOf(player);
        int amount = session.storedRewards;
        if (amount <= 0) return;
        session.storedRewards = 0;
        save(session);
        giveReward(player, amount);
    }

    double rnd() {
        return random.nextBoolean() ? random.nextDouble() : -random.nextDouble();
    }
}

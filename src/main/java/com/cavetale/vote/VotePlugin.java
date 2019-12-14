package com.cavetale.vote;

import com.google.gson.Gson;
import com.vexsoftware.votifier.model.Vote;
import com.winthier.generic_events.GenericEvents;
import com.winthier.sql.SQLDatabase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class VotePlugin extends JavaPlugin {
    // Utility
    SQLDatabase sql;
    EventListener eventListener = new EventListener(this);
    static Gson gson;
    static final long DAY_SECONDS = 60L * 60L * 23L;
    Json json = new Json(this);
    Random random = new Random();
    // Rewards
    Fireworks fireworks = new Fireworks(this);
    Candy candy = new Candy(this);
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
        sql.registerTables(SQLLog.class, SQLPlayer.class, SQLService.class);
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

    @Override
    public void onDisable() {
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
            // Simplified
            if (random.nextBoolean()) {
                giveItem(player, candy.makeCandy(player));
            } else {
                giveItem(player, fireworks.makeFirework(player));
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

    void giveItem(@NonNull Player player, @NonNull ItemStack item) {
        for (ItemStack drop : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getEyeLocation(), drop);
        }
    }

    List<String> getThanksLore(@NonNull String name) {
        return Arrays.
            asList(ChatColor.WHITE + "Thank you for voting, "
                   + ChatColor.GOLD + name
                   + ChatColor.WHITE + "!",
                   ChatColor.WHITE + "Your vote helps us get more",
                   ChatColor.WHITE + "players on the server.",
                   "",
                   ChatColor.WHITE + "Sincerely, "
                   + ChatColor.GRAY + ChatColor.ITALIC + "cavetale.com");
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
        final boolean valid;
        final SQLPlayer session;
        long now = Instant.now().getEpochSecond();
        if (uuid != null) {
            session = sqlPlayerOf(uuid);
            long yesterday = now - DAY_SECONDS;
            long lastVote = session.getLastVoteEpoch(service);
            valid = lastVote < yesterday;
        } else {
            session = null;
            valid = false;
        }
        SQLLog log = new SQLLog(name, uuid, new Date(), valid,
                                service, address, timestamp);
        sql.saveAsync(log, null);
        if (!valid) return false;
        getLogger().info("Vote from " + name + " via " + service + ".");
        session.setLastVoteEpoch(service, now);
        session.monthlyVotes += 1;
        session.allTimeVotes += 1;
        final int totalRewards;
        if (player != null) {
            totalRewards = 1 + session.storedRewards;
            session.storedRewards = 0;
        } else {
            totalRewards = 0;
            session.storedRewards += 1;
        }
        save(session);
        if (player == null) return true;
        giveReward(player, totalRewards);
        return true;
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
            long lastVote = session.getLastVoteEpoch(service.name);
            boolean has = lastVote > yesterday;
            cb = new ComponentBuilder("  ");
            if (has) {
                cb.append(ChatColor.WHITE + "["
                          + ChatColor.GOLD + "x"
                          + ChatColor.WHITE + "]");
            } else {
                cb.append("[ ]").color(ChatColor.GRAY);
            }
            cb.append(" ").reset();
            cb.append(colorize(service.displayName)).color(ChatColor.YELLOW);
            cb.insertion(service.url);
            if (!has) {
                BaseComponent[] tooltip = TextComponent
                    .fromLegacyText("" + ChatColor.BLUE
                                    + ChatColor.UNDERLINE
                                    + service.url);
                cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip));
                cb.event(new ClickEvent(ClickEvent.Action.OPEN_URL, service.url));
            }
            if (has) {
                cb.append(" ").reset();
                long ago = now - lastVote;
                cb.append(agoFormat(ago)).color(ChatColor.GRAY).italic(true);
            }
            player.spigot().sendMessage(cb.create());
        }
    }

    String agoFormat(long seconds) {
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("(%02d:%02d ago)", hours % 60, minutes % 60);
    }

    void checkTime() {
        if (System.currentTimeMillis() <= state.nextResetTime) return;
        int oldMonth = state.currentMonth;
        state.setCurrentMonth();
        json.save(STATE_PATH, state, true);
        getLogger().info("Rolling over the month: " + oldMonth
                         + " -> " + state.currentMonth);
        for (SQLPlayer row : sqlPlayers.values()) {
            row.monthlyVotes = 0;
        }
        sql.saveAsync(new ArrayList<>(sqlPlayers.values()), null);
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
}

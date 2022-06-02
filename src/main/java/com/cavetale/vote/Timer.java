package com.cavetale.vote;

import com.cavetale.core.util.Json;
import com.winthier.playercache.PlayerCache;
import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;

@RequiredArgsConstructor
final class Timer {
    private static final String STATE_PATH = "state.json";
    private final VotePlugin plugin;
    private State state;
    private long lastSpawnWorldTime = -1; // New years
    private boolean newYearsLogged = false;
    private File file;

    protected void enable() {
        plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(), STATE_PATH);
        state = Json.load(file, State.class);
        if (state == null) {
            state = new State();
            state.setCurrentMonth();
            Json.save(file, state, true);
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkTime, 1200L, 1200L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkNewYears, 0L, 20L * 5L);
    }

    protected void checkTime() {
        if (System.currentTimeMillis() <= state.nextResetTime) return;
        int oldMonth = state.currentMonth;
        int oldYear = state.currentYear;
        state.setCurrentMonth();
        Json.save(file, state, true);
        plugin.getLogger().info("Rolling over the month: " + oldMonth
                                + " -> " + state.currentMonth);
        List<SQLMonthly> rows = plugin.database.find(SQLMonthly.class)
            .orderByDescending("votes").findList();
        plugin.database.find(SQLMonthly.class).delete();
        if (rows.isEmpty()) return;
        int votes = rows.get(0).getVotes();
        final List<UUID> kings = new ArrayList<>();
        for (SQLMonthly row : rows) {
            if (row.getVotes() < votes) break;
            kings.add(row.getUuid());
        }
        for (UUID king : kings) {
            SQLMonthly newRow = new SQLMonthly(king, 0);
            newRow.setVoteKing(true);
            plugin.database.save(newRow);
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
                Map<UUID, Integer> scores = new HashMap<>();
                for (SQLMonthly row : rows) {
                    scores.put(row.getUuid(), row.getVotes());
                }
                int count = plugin.reward(oldYear, oldMonth, scores);
                plugin.getLogger().info("Awarded " + count + " players");
                List<String> names = new ArrayList<>();
                for (UUID king : kings) {
                    names.add(PlayerCache.nameForUuid(king));
                }
                plugin.getLogger().info("Vote King: " + names);
                for (String name : names) {
                    final String cmd = "titles unlockset " + name + " VoteKing";
                    plugin.getLogger().info("Issuing command: " + cmd);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            });
    }

    protected void checkNewYears() {
        LocalDate localDate = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toLocalDate();
        Month month = localDate.getMonth();
        long day = localDate.getDayOfMonth();
        if (!(month == Month.DECEMBER && day == 31) && !(month == Month.JANUARY && day == 1)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
                World spawnWorld = Bukkit.getWorld("spawn");
                if (spawnWorld == null) return;
                if (!newYearsLogged) {
                    plugin.getLogger().info("We have determined it is time for the New Year's spawn firework!");
                    newYearsLogged = true;
                }
                long spawnWorldTime = spawnWorld.getTime();
                final long midnight = 18000;
                if (lastSpawnWorldTime >= 0 && lastSpawnWorldTime < midnight && spawnWorldTime >= midnight) {
                    plugin.getLogger().info("Starting New Year's spawn midnight fireworks show");
                    plugin.fireworks.startShow();
                }
                lastSpawnWorldTime = spawnWorldTime;
            });
    }
}

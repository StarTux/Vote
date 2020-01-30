package com.cavetale.vote;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Table(name = "players",
       uniqueConstraints = @UniqueConstraint(columnNames = "uuid"))
@Getter @Setter
public final class SQLPlayer {
    @Id
    Integer id;
    @Column(nullable = false)
    UUID uuid;
    // @Column(nullable = false)
    // int monthlyVotes = 0;
    @Column(nullable = false)
    int allTimeVotes = 0;
    @Column(nullable = false)
    int storedRewards = 0;
    @Column(nullable = true, length = 2048)
    String json;
    transient Tag tag;
    static final long HOUR = 60L * 60L;
    static final long DAY = 60L * 60L * 24L;

    static final class Tag {
        Map<String, Long> lastVotes = new HashMap<>(); // <Service, Epoch>
    }

    public SQLPlayer() { }

    SQLPlayer(@NonNull final UUID uuid) {
        this.uuid = uuid;
        tag = new Tag();
    }

    void pack() {
        if (tag == null) {
            json = null;
            return;
        }
        json = VotePlugin.gson.toJson(tag);
    }

    void unpack() {
        if (json == null) {
            tag = null;
            return;
        }
        tag = VotePlugin.gson.fromJson(json, Tag.class);
    }

    long getLastVoteEpoch(@NonNull String service) {
        Long value = tag.lastVotes.get(service);
        return value == null ? 0L : value;
    }

    void setLastVoteEpoch(@NonNull String service, long epoch) {
        tag.lastVotes.put(service, epoch);
    }

    /**
     * @return Epoch (seconds)
     */
    long getNextVote(@NonNull String serviceName) {
        long last = getLastVoteEpoch(serviceName);
        if (last == 0) return 0;
        switch (serviceName) {
        case "PlanetMinecraft.com":
            return (last / DAY + 1) * DAY
                + 5 * HOUR; // Daily. Probably inaccurate.
        case "MCSL": // minecraft-server-list.com
            return last + 24 * HOUR; // Way off!
        case "MinecraftServers.org":
            return last + 24 * HOUR; // Way off!
        case "TopG.org":
            return last + 12 * HOUR; // Nailed it!
        case "Minecraft-MP.com":
            return (last / DAY + 1) * DAY + 5 * HOUR;
        default:
            return (last / DAY + 1) * DAY;
        }
    }

    boolean canVote(@NonNull String serviceName, final long now) {
        return getNextVote(serviceName) <= now;
    }
}

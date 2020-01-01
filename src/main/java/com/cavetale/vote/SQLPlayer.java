package com.cavetale.vote;

import java.util.Comparator;
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
    @Column(nullable = false)
    int monthlyVotes = 0;
    @Column(nullable = false)
    int allTimeVotes = 0;
    @Column(nullable = false)
    int storedRewards = 0;
    @Column(nullable = true, length = 2048)
    String json;
    transient Tag tag;

    static final Comparator<SQLPlayer> HIGHSCORE = (a, b) -> {
        int res = Integer.compare(b.monthlyVotes,
                                  a.monthlyVotes);
        if (res != 0) return res;
        return Integer.compare(b.allTimeVotes,
                               a.allTimeVotes);
    };

    static final class Tag {
        Map<String, Long> lastVotes = new HashMap<>(); // <Service, Epoch>
        boolean voteKing; //
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
}

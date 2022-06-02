package com.cavetale.vote;

import com.winthier.sql.SQLRow;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

@Data @Table(name = "last_votes",
             uniqueConstraints = @UniqueConstraint(columnNames = {"player", "service"}))
public final class SQLLastVote implements SQLRow {
    private static final long HOUR = 60L * 60L;
    private static final long DAY = 60L * 60L * 24L;

    @Id
    private Integer id;

    @Column(nullable = false)
    private UUID player;

    @Column(nullable = false, length = 40)
    private String service;

    @Column(nullable = false)
    private Date time;

    public SQLLastVote() { }

    public SQLLastVote(final UUID player, final String service) {
        this.player = player;
        this.service = service;
        this.time = new Date();
    }

    /**
     * @return Epoch (seconds)
     */
    public Instant getNextVote() {
        long last = getLastVote().getEpochSecond();
        switch (service) {
        case "PlanetMinecraft.com":
            // Daily. Probably inaccurate.
            return Instant.ofEpochSecond((last / DAY + 1) * DAY + 5 * HOUR);
        case "MCSL":
            // minecraft-server-list.com
            // Way off!
            return Instant.ofEpochSecond(last + 24 * HOUR);
        case "MinecraftServers.org":
            // Way off!
            return Instant.ofEpochSecond(last + 24 * HOUR);
        case "TopG.org":
            // Nailed it!
            return Instant.ofEpochSecond(last + 12 * HOUR);
        case "Minecraft-MP.com":
            return Instant.ofEpochSecond((last / DAY + 1) * DAY + 5 * HOUR);
        default:
            return Instant.ofEpochSecond((last / DAY + 1) * DAY);
        }
    }

    public Instant getLastVote() {
        return time.toInstant();
    }

    public boolean canVote(final Instant now) {
        return getNextVote().isBefore(now);
    }
}

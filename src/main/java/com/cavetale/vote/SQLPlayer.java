package com.cavetale.vote;

import com.winthier.sql.SQLRow;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NonNull;

@Data @Table(name = "players",
             uniqueConstraints = @UniqueConstraint(columnNames = "uuid"))
public final class SQLPlayer implements SQLRow {
    @Id
    private Integer id;

    @Column(nullable = false)
    private UUID uuid;

    @Column(nullable = false)
    private int allTimeVotes;

    @Column(nullable = false)
    private int storedRewards;

    public SQLPlayer() { }

    SQLPlayer(@NonNull final UUID uuid, final int votes) {
        this.uuid = uuid;
        this.allTimeVotes = votes;
        this.storedRewards = votes;
    }
}

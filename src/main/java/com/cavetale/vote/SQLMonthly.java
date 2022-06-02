package com.cavetale.vote;

import com.winthier.sql.SQLRow;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data @Table(name = "monthly")
public final class SQLMonthly implements SQLRow {
    @Id
    private Integer id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false)
    private int votes = 0;

    @Column(nullable = false)
    private boolean voteKing;

    public SQLMonthly() { }

    public SQLMonthly(final UUID uuid, final int votes) {
        this.uuid = uuid;
        this.votes = votes;
    }
}

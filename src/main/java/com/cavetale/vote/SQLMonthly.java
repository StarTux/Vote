package com.cavetale.vote;

import com.winthier.sql.SQLRow;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Table(name = "monthly",
       uniqueConstraints = @UniqueConstraint(columnNames = "uuid"))
@Getter @Setter
public final class SQLMonthly implements SQLRow {
    @Id
    Integer id;
    @Column(nullable = false)
    UUID uuid;
    @Column(nullable = false)
    int votes = 0;
    @Column(nullable = false)
    boolean voteKing;

    public SQLMonthly() { }

    SQLMonthly(final UUID uuid) {
        this.uuid = uuid;
    }
}

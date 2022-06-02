package com.cavetale.vote;

import com.winthier.sql.SQLRow;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import lombok.NonNull;

@Data @Table(name = "services")
public final class SQLService implements SQLRow {
    @Id
    Integer id;
    @Column(nullable = false, length = 40, unique = true)
    String name;
    @Column(nullable = false)
    String displayName;
    @Column(nullable = false)
    String url;
    @Column
    boolean enabled;
    @Column(nullable = false)
    int priority = 0;

    public SQLService() { }

    SQLService(@NonNull final String name,
               @NonNull final String displayName,
               @NonNull final String url) {
        this.name = name;
        this.displayName = displayName;
        this.url = url;
        enabled = true;
    }
}

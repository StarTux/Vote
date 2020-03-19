package com.cavetale.vote;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Table(name = "services")
@Getter @Setter
public final class SQLService {
    @Id
    Integer id;
    @Column(nullable = false)
    String name;
    @Column(nullable = false)
    String displayName;
    @Column(nullable = false)
    String url;
    @Column
    boolean enabled;

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

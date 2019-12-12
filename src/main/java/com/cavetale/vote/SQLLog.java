package com.cavetale.vote;

import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Table(name = "log")
@Getter @Setter
public final class SQLLog {
    @Id
    Integer id;
    @Column(nullable = false, length = 16)
    String user;
    @Column(nullable = true)
    UUID userId;
    @Column(nullable = false)
    Date time;
    @Column(nullable = false)
    boolean valid; // timing ok
    @Column(nullable = false, length = 255)
    String service;
    @Column(nullable = true, length = 255)
    String address;
    @Column(nullable = true, length = 255)
    String timestamp;

    public SQLLog() { }

    SQLLog(@NonNull final String user,
           final UUID userId,
           @NonNull final Date time,
           final boolean valid,
           @NonNull final String service,
           final String address,
           final String timestamp) {
        this.user = user;
        this.userId = userId;
        this.time = time;
        this.valid = valid;
        this.service = service;
        this.address = address;
        this.timestamp = timestamp;
    }
}

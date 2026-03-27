package com.ticketing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;
}

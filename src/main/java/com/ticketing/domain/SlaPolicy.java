package com.ticketing.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "sla_policies")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
public class SlaPolicy extends BaseEntity {


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Ticket.Priority priority;

    @Column(name = "first_response_sec", nullable = false)
    private int firstResponseSec;

    @Column(name = "resolution_sec", nullable = false)
    private int resolutionSec;
}

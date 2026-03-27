package com.ticketing.dto;
import lombok.Data;
@Data
public class AddNoteRequest {
    private String body;
    private boolean internal;
}

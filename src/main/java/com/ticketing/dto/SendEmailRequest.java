package com.ticketing.dto;

import lombok.Data;

@Data
public class SendEmailRequest {
    private String subject;
    private String body;
}

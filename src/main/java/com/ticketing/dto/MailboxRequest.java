package com.ticketing.dto;

import lombok.Data;

@Data
public class MailboxRequest {
    private String  name;
    private String  mailboxAddress;   // e.g. support@yourcompany.com
    private String  folderName;       // default: Inbox
    private boolean active;
    private String  defaultTeam;
}

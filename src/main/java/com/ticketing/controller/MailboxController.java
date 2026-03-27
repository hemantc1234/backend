package com.ticketing.controller;

import com.ticketing.domain.MailboxConfig;
import com.ticketing.dto.MailboxRequest;
import com.ticketing.repository.MailboxConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/mailboxes")
@RequiredArgsConstructor
public class MailboxController {

    private final MailboxConfigRepository mailboxRepo;

    @GetMapping
    public List<MailboxConfig> list() {
        return mailboxRepo.findAll();
    }

    @PostMapping
    public ResponseEntity<MailboxConfig> create(@RequestBody MailboxRequest req) {
        MailboxConfig mb = MailboxConfig.builder()
                .name(req.getName())
                .mailboxAddress(req.getMailboxAddress())
                .folderName(req.getFolderName() != null ? req.getFolderName() : "Inbox")
                .active(req.isActive())
                .defaultTeam(req.getDefaultTeam())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(mailboxRepo.save(mb));
    }

    @PutMapping("/{id}")
    public MailboxConfig update(@PathVariable String id, @RequestBody MailboxRequest req) {
        MailboxConfig mb = mailboxRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Mailbox not found: " + id));
        mb.setName(req.getName());
        mb.setMailboxAddress(req.getMailboxAddress());
        if (req.getFolderName() != null) mb.setFolderName(req.getFolderName());
        mb.setActive(req.isActive());
        mb.setDefaultTeam(req.getDefaultTeam());
        return mailboxRepo.save(mb);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        mailboxRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

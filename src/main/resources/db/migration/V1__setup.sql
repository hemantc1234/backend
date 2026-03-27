-- =============================================================================
-- V1__setup.sql  —  SecureNow Helpdesk complete database setup
-- Schema, reference data, and demo data in one file.
-- Flyway runs this once automatically on first startup.
-- =============================================================================

-- ── Tables ────────────────────────────────────────────────────────────────────

CREATE TABLE agents (
    id          CHAR(36)     NOT NULL PRIMARY KEY,
    username    VARCHAR(80)  NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    full_name   VARCHAR(150) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    team        VARCHAR(100),
    role        ENUM('AGENT','SENIOR_AGENT','MANAGER') NOT NULL DEFAULT 'AGENT',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mailbox_configs (
    id              CHAR(36)     NOT NULL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    mailbox_address VARCHAR(255) NOT NULL,
    folder_name     VARCHAR(100) NOT NULL DEFAULT 'Inbox',
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    default_team    VARCHAR(100),
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sla_policies (
    id                  CHAR(36)  NOT NULL PRIMARY KEY,
    priority            ENUM('LOW','MEDIUM','HIGH','URGENT') NOT NULL UNIQUE,
    first_response_sec  INT       NOT NULL,
    resolution_sec      INT       NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE routing_rules (
    id              CHAR(36)     NOT NULL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    rule_type       ENUM('KEYWORD','DOMAIN','VIP','DEFAULT') NOT NULL,
    match_value     VARCHAR(500),
    match_field     ENUM('SUBJECT','BODY','SUBJECT_OR_BODY','SENDER') NOT NULL DEFAULT 'SUBJECT_OR_BODY',
    target_team     VARCHAR(100),
    target_agent_id CHAR(36),
    priority        INT          NOT NULL DEFAULT 100,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tickets (
    id                CHAR(36)     NOT NULL PRIMARY KEY,
    subject           VARCHAR(500) NOT NULL,
    description       MEDIUMTEXT,
    sender_email      VARCHAR(255) NOT NULL,
    sender_name       VARCHAR(255),
    status            ENUM('OPEN','IN_PROGRESS','ON_HOLD','CLOSED') NOT NULL DEFAULT 'OPEN',
    priority          ENUM('LOW','MEDIUM','HIGH','URGENT')          NOT NULL DEFAULT 'MEDIUM',
    assigned_to       VARCHAR(100),
    assigned_agent_id CHAR(36),
    mailbox_id        CHAR(36),
    overdue           BOOLEAN      NOT NULL DEFAULT FALSE,
    due_date          DATETIME(3)  NULL,
    first_replied_at  DATETIME(3),
    resolved_at       DATETIME(3),
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_ticket_status   ON tickets(status);
CREATE INDEX idx_ticket_priority ON tickets(priority);
CREATE INDEX idx_ticket_created  ON tickets(created_at DESC);
CREATE INDEX idx_ticket_assigned ON tickets(assigned_to);
CREATE INDEX idx_ticket_due_date ON tickets(due_date);
CREATE INDEX idx_ticket_overdue  ON tickets(overdue);

CREATE TABLE ticket_messages (
    id           CHAR(36)     NOT NULL PRIMARY KEY,
    ticket_id    CHAR(36)     NOT NULL,
    message_id   VARCHAR(500) NOT NULL UNIQUE,
    in_reply_to  VARCHAR(500),
    body         MEDIUMTEXT,
    sender_email VARCHAR(255),
    direction    ENUM('INBOUND','OUTBOUND') NOT NULL DEFAULT 'INBOUND',
    internal     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_msg_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_msg_ticket     ON ticket_messages(ticket_id);
CREATE INDEX idx_msg_message_id ON ticket_messages(message_id);
CREATE INDEX idx_msg_reply_to   ON ticket_messages(in_reply_to);

CREATE TABLE ticket_audit_events (
    id         CHAR(36)     NOT NULL PRIMARY KEY,
    ticket_id  CHAR(36)     NOT NULL,
    actor      VARCHAR(150) NOT NULL,
    event_type ENUM('TICKET_CREATED','STATUS_CHANGED','PRIORITY_CHANGED','ASSIGNED',
                    'REPLY_ADDED','NOTE_ADDED','SLA_BREACHED','SPAM_FILTERED') NOT NULL,
    old_value  VARCHAR(500),
    new_value  VARCHAR(500),
    note       TEXT,
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_audit_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_audit_ticket ON ticket_audit_events(ticket_id, created_at DESC);

CREATE TABLE spam_filter_log (
    id           CHAR(36)     NOT NULL PRIMARY KEY,
    message_id   VARCHAR(500),
    sender_email VARCHAR(255),
    subject      VARCHAR(500),
    reason       VARCHAR(255),
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE processed_graph_messages (
    message_id   VARCHAR(500) NOT NULL PRIMARY KEY,
    mailbox_id   CHAR(36)     NOT NULL,
    processed_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pgm_mailbox ON processed_graph_messages(mailbox_id, processed_at DESC);

-- ── Reference data ────────────────────────────────────────────────────────────

INSERT INTO sla_policies (id, priority, first_response_sec, resolution_sec) VALUES
  (UUID(), 'URGENT',  1800,   14400),
  (UUID(), 'HIGH',    3600,   28800),
  (UUID(), 'MEDIUM', 14400,   86400),
  (UUID(), 'LOW',    86400,  259200);

INSERT INTO routing_rules (id, name, rule_type, match_value, match_field, target_team, priority, active) VALUES
  (UUID(), 'Enterprise domain', 'DOMAIN',  '@enterprise.com',                       'SENDER',          'Enterprise Support', 5,   TRUE),
  (UUID(), 'Billing keywords',  'KEYWORD', 'payment,invoice,billing,refund,charge', 'SUBJECT_OR_BODY', 'Finance Team',       10,  TRUE),
  (UUID(), 'Tech support bugs', 'KEYWORD', 'error,bug,crash,exception,not working', 'SUBJECT_OR_BODY', 'Tech Team',          20,  TRUE),
  (UUID(), 'Sales inquiries',   'KEYWORD', 'pricing,quote,demo,trial,license',      'SUBJECT_OR_BODY', 'Sales Team',         30,  TRUE),
  (UUID(), 'Fallback',          'DEFAULT', '*',                                     'SUBJECT_OR_BODY', 'General Queue',      999, TRUE);

INSERT INTO mailbox_configs (id, name, mailbox_address, folder_name, active, default_team) VALUES
  (UUID(), 'Main Support Inbox', 'supporttesting@securenow.in', 'Inbox', true, 'General Queue');

-- ── Demo agents  (password = Agent@1234) ─────────────────────────────────────

INSERT INTO agents (id, username, email, full_name, password, team, role, active) VALUES
  (UUID(), 'sarah', 'sarah@example.com', 'Sarah Johnson', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Tech Team',          'SENIOR_AGENT', TRUE),
  (UUID(), 'devp',  'dev@example.com',   'Dev Patel',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Tech Team',          'AGENT',        TRUE),
  (UUID(), 'james', 'james@example.com', 'James Wilson',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Finance Team',       'AGENT',        TRUE),
  (UUID(), 'priya', 'priya@example.com', 'Priya Patel',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'General Queue',      'AGENT',        TRUE),
  (UUID(), 'marco', 'marco@example.com', 'Marco Rossi',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Sales Team',         'AGENT',        TRUE),
  (UUID(), 'lisa',  'lisa@example.com',  'Lisa Chen',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Enterprise Support', 'SENIOR_AGENT', TRUE);

-- ── Demo tickets ──────────────────────────────────────────────────────────────

INSERT INTO tickets (id, subject, description, sender_email, sender_name, status, priority, assigned_to, overdue, due_date, created_at, updated_at) VALUES
  ('tic-0001-0000-0000-0000-000000000001', 'Cannot log in to my account',
   'I keep getting Invalid credentials error since this morning.',
   'alice@example.com', 'Alice Thompson', 'OPEN', 'HIGH', 'Tech Team',
   TRUE,  NOW() + INTERVAL 2 DAY, NOW() - INTERVAL 2 HOUR,  NOW() - INTERVAL 2 HOUR),

  ('tic-0002-0000-0000-0000-000000000002', 'Invoice #4820 — payment failed',
   'Submitted payment three days ago but it still shows unpaid.',
   'billing@acme.com', 'Bob Martinez', 'IN_PROGRESS', 'URGENT', 'Finance Team',
   FALSE, NOW() + INTERVAL 1 DAY, NOW() - INTERVAL 5 HOUR,  NOW() - INTERVAL 1 HOUR),

  ('tic-0003-0000-0000-0000-000000000003', 'Feature request: export tickets to CSV',
   'Export the ticket list to CSV for weekly reports.',
   'carol@startup.io', 'Carol White', 'OPEN', 'LOW', 'General Queue',
   FALSE, NULL,              NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 2 DAY),

  ('tic-0004-0000-0000-0000-000000000004', 'App crashes uploading files over 10MB',
   'Happens on both Chrome and Firefox. Version 2.4.1.',
   'dev@techcorp.com', 'David Kim', 'IN_PROGRESS', 'HIGH', 'Tech Team',
   FALSE, NOW() + INTERVAL 3 DAY, NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 3 HOUR),

  ('tic-0005-0000-0000-0000-000000000005', 'Need pricing for enterprise plan',
   'Team of 200 people interested in switching providers.',
   'procurement@bigco.com', 'Emma Davis', 'OPEN', 'MEDIUM', 'Sales Team',
   FALSE, NOW() + INTERVAL 7 DAY, NOW() - INTERVAL 6 HOUR,  NOW() - INTERVAL 6 HOUR),

  ('tic-0006-0000-0000-0000-000000000006', 'Dashboard loading 30+ seconds',
   'Slow since Monday update. 50k records in account.',
   'ops@enterprise.com', 'Frank Miller', 'ON_HOLD', 'HIGH', 'Enterprise Support',
   TRUE,  NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 3 DAY),

  ('tic-0007-0000-0000-0000-000000000007', 'How do I set up two-factor auth?',
   'Cannot find the 2FA option in settings.',
   'grace@example.com', 'Grace Lee', 'CLOSED', 'LOW', 'General Queue',
   FALSE, NULL,              NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 3 DAY),

  ('tic-0008-0000-0000-0000-000000000008', 'Refund — duplicate charge on March invoice',
   'Charged twice. TXN-8821 and TXN-8834 both on March 3rd.',
   'henry@example.com', 'Henry Brown', 'CLOSED', 'HIGH', 'Finance Team',
   FALSE, NULL,              NOW() - INTERVAL 7 DAY,  NOW() - INTERVAL 5 DAY),

  ('tic-0009-0000-0000-0000-000000000009', 'API 500 error on /v2/reports endpoint',
   '/v2/reports returns HTTP 500. Affects production pipeline.',
   'api@techpartner.com', 'Ivan Petrov', 'IN_PROGRESS', 'URGENT', 'Tech Team',
   FALSE, NOW() + INTERVAL 1 DAY, NOW() - INTERVAL 18 HOUR, NOW() - INTERVAL 30 MINUTE),

  ('tic-0010-0000-0000-0000-000000000010', 'Request for custom branding options',
   'Want to customise logo per client for white-label.',
   'agency@creativeagency.com', 'Julia Santos', 'OPEN', 'LOW', 'Sales Team',
   FALSE, NULL,              NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 1 DAY),

  ('tic-0011-0000-0000-0000-000000000011', 'Slow response on bulk email import',
   'Importing 500+ contacts causes timeouts.',
   'it@bigorg.com', 'Kevin Zhang', 'OPEN', 'MEDIUM', 'Tech Team',
   FALSE, NOW() + INTERVAL 4 DAY, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR);

-- ── Demo messages ─────────────────────────────────────────────────────────────

INSERT INTO ticket_messages (id, ticket_id, message_id, body, sender_email, direction, internal, created_at) VALUES
  (UUID(), 'tic-0001-0000-0000-0000-000000000001', 'msg-001-in',
   'I keep getting Invalid credentials since this morning. I have not changed my password.',
   'alice@example.com', 'INBOUND', FALSE, NOW() - INTERVAL 2 HOUR),

  (UUID(), 'tic-0002-0000-0000-0000-000000000002', 'msg-002-in',
   'Submitted payment for invoice #4820 three days ago. Still shows unpaid.',
   'billing@acme.com', 'INBOUND', FALSE, NOW() - INTERVAL 5 HOUR),

  (UUID(), 'tic-0002-0000-0000-0000-000000000002', 'msg-002-reply',
   'Hi Bob, I found the transaction. Escalating to payments team now.',
   'james@example.com', 'OUTBOUND', FALSE, NOW() - INTERVAL 1 HOUR),

  (UUID(), 'tic-0004-0000-0000-0000-000000000004', 'msg-004-in',
   'App crashes every time I upload a file larger than 10MB.',
   'dev@techcorp.com', 'INBOUND', FALSE, NOW() - INTERVAL 1 DAY),

  (UUID(), 'tic-0004-0000-0000-0000-000000000004', 'msg-004-note',
   'Reproduced. Nginx upload limit is 10MB. Checking with infra to increase.',
   'sarah@example.com', 'OUTBOUND', TRUE, NOW() - INTERVAL 3 HOUR),

  (UUID(), 'tic-0006-0000-0000-0000-000000000006', 'msg-006-in',
   'Dashboard takes 30+ seconds to load since Monday update.',
   'ops@enterprise.com', 'INBOUND', FALSE, NOW() - INTERVAL 3 DAY),

  (UUID(), 'tic-0006-0000-0000-0000-000000000006', 'msg-006-reply',
   'Hi Frank, can you send a HAR file from your browser while the dashboard loads?',
   'lisa@example.com', 'OUTBOUND', FALSE, NOW() - INTERVAL 2 DAY),

  (UUID(), 'tic-0007-0000-0000-0000-000000000007', 'msg-007-in',
   'I want to enable 2FA but cannot find it in settings.',
   'grace@example.com', 'INBOUND', FALSE, NOW() - INTERVAL 4 DAY),

  (UUID(), 'tic-0007-0000-0000-0000-000000000007', 'msg-007-reply',
   'Hi Grace! Go to Settings → Security → Two-Factor Authentication and click Enable.',
   'priya@example.com', 'OUTBOUND', FALSE, NOW() - INTERVAL 3 DAY),

  (UUID(), 'tic-0009-0000-0000-0000-000000000009', 'msg-009-in',
   '/v2/reports returns HTTP 500. /v1/reports still works. Affects production.',
   'api@techpartner.com', 'INBOUND', FALSE, NOW() - INTERVAL 18 HOUR),

  (UUID(), 'tic-0009-0000-0000-0000-000000000009', 'msg-009-reply',
   'Hi Ivan, schema change broke v2. Hotfix deploying in 2 hours.',
   'sarah@example.com', 'OUTBOUND', FALSE, NOW() - INTERVAL 30 MINUTE);

-- ── Audit events ──────────────────────────────────────────────────────────────

INSERT INTO ticket_audit_events (id, ticket_id, actor, event_type, old_value, new_value, note, created_at) VALUES
  (UUID(), 'tic-0001-0000-0000-0000-000000000001', 'SYSTEM', 'TICKET_CREATED',   NULL,          'OPEN',        'Created from inbound email',         NOW() - INTERVAL 2 HOUR),
  (UUID(), 'tic-0001-0000-0000-0000-000000000001', 'SYSTEM', 'SLA_BREACHED',     NULL,          'overdue',     'No first response within SLA',       NOW() - INTERVAL 30 MINUTE),
  (UUID(), 'tic-0002-0000-0000-0000-000000000002', 'SYSTEM', 'TICKET_CREATED',   NULL,          'OPEN',        'Created from inbound email',         NOW() - INTERVAL 5 HOUR),
  (UUID(), 'tic-0002-0000-0000-0000-000000000002', 'james',  'STATUS_CHANGED',   'OPEN',        'IN_PROGRESS', NULL,                                 NOW() - INTERVAL 4 HOUR),
  (UUID(), 'tic-0003-0000-0000-0000-000000000003', 'SYSTEM', 'TICKET_CREATED',   NULL,          'OPEN',        'Created from inbound email',         NOW() - INTERVAL 2 DAY),
  (UUID(), 'tic-0004-0000-0000-0000-000000000004', 'SYSTEM', 'TICKET_CREATED',   NULL,          'OPEN',        'Created from inbound email',         NOW() - INTERVAL 1 DAY),
  (UUID(), 'tic-0004-0000-0000-0000-000000000004', 'sarah',  'STATUS_CHANGED',   'OPEN',        'IN_PROGRESS', NULL,                                 NOW() - INTERVAL 20 HOUR),
  (UUID(), 'tic-0004-0000-0000-0000-000000000004', 'sarah',  'PRIORITY_CHANGED', 'MEDIUM',      'HIGH',        'Severity increased after reproducing',NOW() - INTERVAL 19 HOUR),
  (UUID(), 'tic-0005-0000-0000-0000-000000000005', 'SYSTEM', 'TICKET_CREATED',   NULL,          'OPEN',        'Created from inbound email',         NOW() - INTERVAL 6 HOUR),
  (UUID(), 'tic-0006-0000-0000-0000-000000000006', 'SYSTEM', 'TICKET_CREATED',   NULL,          'OPEN',        'Created from inbound email',         NOW() - INTERVAL 3 DAY),
  (UUID(), 'tic-0006-0000-0000-0000-000000000006', 'lisa',   'STATUS_CHANGED',   'IN_PROGRESS', 'ON_HOLD',     'Waiting for HAR file from customer', NOW() - INTERVAL 2 DAY),
  (UUID(), 'tic-0006-0000-0000-0000-000000000006', 'SYSTEM', 'SLA_BREACHED',     NULL,          'overdue',     'No resolution within SLA',           NOW() - INTERVAL 2 DAY),
  (UUID(), 'tic-0007-0000-0000-0000-000000000007', 'SYSTEM', 'TICKET_CREATED',   NULL,          'OPEN',        'Created from inbound email',         NOW() - INTERVAL 4 DAY),
  (UUID(), 'tic-0007-0000-0000-0000-000000000007', 'priya',  'STATUS_CHANGED',   'OPEN',        'IN_PROGRESS', NULL,                                 NOW() - INTERVAL 4 DAY),
  (UUID(), 'tic-0007-0000-0000-0000-000000000007', 'priya',  'STATUS_CHANGED',   'IN_PROGRESS', 'CLOSED',      'Customer confirmed resolved',        NOW() - INTERVAL 3 DAY),
  (UUID(), 'tic-0008-0000-0000-0000-000000000008', 'SYSTEM', 'TICKET_CREATED',   NULL,          'OPEN',        'Created from inbound email',         NOW() - INTERVAL 7 DAY),
  (UUID(), 'tic-0008-0000-0000-0000-000000000008', 'james',  'STATUS_CHANGED',   'OPEN',        'IN_PROGRESS', NULL,                                 NOW() - INTERVAL 6 DAY),
  (UUID(), 'tic-0008-0000-0000-0000-000000000008', 'james',  'STATUS_CHANGED',   'IN_PROGRESS', 'CLOSED',      'Refund processed',                   NOW() - INTERVAL 5 DAY),
  (UUID(), 'tic-0009-0000-0000-0000-000000000009', 'SYSTEM', 'TICKET_CREATED',   NULL,          'OPEN',        'Created from inbound email',         NOW() - INTERVAL 18 HOUR),
  (UUID(), 'tic-0009-0000-0000-0000-000000000009', 'sarah',  'STATUS_CHANGED',   'OPEN',        'IN_PROGRESS', NULL,                                 NOW() - INTERVAL 17 HOUR),
  (UUID(), 'tic-0009-0000-0000-0000-000000000009', 'sarah',  'ASSIGNED',         'Tech Team',   'sarah',       'Self-assigned',                      NOW() - INTERVAL 17 HOUR),
  (UUID(), 'tic-0010-0000-0000-0000-000000000010', 'SYSTEM', 'TICKET_CREATED',   NULL,          'OPEN',        'Created from inbound email',         NOW() - INTERVAL 1 DAY),
  (UUID(), 'tic-0011-0000-0000-0000-000000000011', 'SYSTEM', 'TICKET_CREATED',   NULL,          'OPEN',        'Created from inbound email',         NOW() - INTERVAL 3 HOUR);

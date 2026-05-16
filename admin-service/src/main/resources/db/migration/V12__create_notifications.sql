CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(100) NOT NULL,
    target_url VARCHAR(500) NULL,
    read_status BOOLEAN NOT NULL DEFAULT FALSE,
    global_notification BOOLEAN NOT NULL DEFAULT FALSE,
    reference_key VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    read_at DATETIME NULL,
    expires_at DATETIME NULL,
    CONSTRAINT uk_notifications_reference_key UNIQUE (reference_key)
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at);
CREATE INDEX idx_notifications_global_created ON notifications (global_notification, created_at);
CREATE INDEX idx_notifications_expires_at ON notifications (expires_at);

CREATE TABLE notification_reads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at DATETIME NOT NULL,
    CONSTRAINT fk_notification_reads_notification
        FOREIGN KEY (notification_id) REFERENCES notifications (id) ON DELETE CASCADE,
    CONSTRAINT uk_notification_reads_notification_user UNIQUE (notification_id, user_id)
);

CREATE INDEX idx_notification_reads_user ON notification_reads (user_id);

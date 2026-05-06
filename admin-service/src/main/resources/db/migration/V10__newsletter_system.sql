CREATE TABLE IF NOT EXISTS newsletter_subscribers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    subscribed_at DATETIME NOT NULL,
    unsubscribed_at DATETIME NULL,
    source VARCHAR(100) NULL,
    INDEX idx_newsletter_subscribers_active (active)
);

CREATE TABLE IF NOT EXISTS newsletter_campaigns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    button_text VARCHAR(100) NULL,
    button_url VARCHAR(500) NULL,
    total_recipients INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    sent_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_newsletter_campaigns_created_at (created_at)
);

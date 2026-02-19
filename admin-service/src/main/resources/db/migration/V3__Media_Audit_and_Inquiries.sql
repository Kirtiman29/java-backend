  CREATE TABLE design_media (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      design_id BIGINT NOT NULL,
      asset_uuid VARCHAR(128) NOT NULL,
      asset_type VARCHAR(50),
      media_role VARCHAR(50) DEFAULT 'GALLERY',
      sort_order INT DEFAULT 0,
      CONSTRAINT FK_MEDIA_DESIGN FOREIGN KEY (design_id) REFERENCES designs(id) ON DELETE CASCADE
  );

  CREATE TABLE design_deletion_records (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      design_id BIGINT NOT NULL,
      order_id BIGINT,
      deleted_by VARCHAR(100) NOT NULL,
      deleted_assets TEXT,
      design_identifier VARCHAR(100),
      deleted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );

  CREATE TABLE contact_messages (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      name VARCHAR(150) NOT NULL,
      email VARCHAR(150) NOT NULL,
      message TEXT NOT NULL,
      status VARCHAR(30) NOT NULL DEFAULT 'NEW',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  );

  CREATE TABLE jobs (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      title VARCHAR(255) NOT NULL,
      slug VARCHAR(255) NOT NULL UNIQUE,
      description TEXT NOT NULL,
      location VARCHAR(255),
      experience_level VARCHAR(100),
      job_type VARCHAR(50),
      status VARCHAR(20) DEFAULT 'OPEN',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  );

  CREATE TABLE job_applications (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      job_id BIGINT NOT NULL,
      full_name VARCHAR(255) NOT NULL,
      email VARCHAR(255) NOT NULL,
      phone VARCHAR(20),
      resume_asset_uuid VARCHAR(128),
      applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      CONSTRAINT FK_APP_JOB FOREIGN KEY (job_id) REFERENCES jobs(id)
  );

  CREATE TABLE homepage_banners (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      subtitle VARCHAR(255),
      title VARCHAR(255),
      description TEXT,
      cta_text VARCHAR(100),
      cta_url VARCHAR(255),
      background_image_url VARCHAR(512),
      theme VARCHAR(50),
      start_date DATE,
      end_date DATE,
      active BOOLEAN DEFAULT FALSE,
      priority INT DEFAULT 0,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  );
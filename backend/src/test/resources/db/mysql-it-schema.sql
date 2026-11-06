-- Only executed through the guarded disposable-container datasource.
USE eventcampus_it;
CREATE TABLE roles (
    id_role INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

-- ============================================================
-- 2. USERS
-- ============================================================

CREATE TABLE users (
    id_user INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(191) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_role INT NOT NULL,

    CONSTRAINT fk_users_role
        FOREIGN KEY (id_role)
        REFERENCES roles(id_role)
) ENGINE=InnoDB;

-- ============================================================
-- 3. CATEGORIES
-- ============================================================

CREATE TABLE categories (
    id_category INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

-- ============================================================
-- 4. EVENTS
-- ============================================================

CREATE TABLE events (
    id_event INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    image VARCHAR(500),
    event_date DATE NOT NULL,
    event_time TIME NOT NULL,
    location VARCHAR(255) NOT NULL,
    capacity INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    id_creator INT NOT NULL,
    id_category INT NOT NULL,

    CONSTRAINT fk_events_creator
        FOREIGN KEY (id_creator)
        REFERENCES users(id_user),

    CONSTRAINT fk_events_category
        FOREIGN KEY (id_category)
        REFERENCES categories(id_category),

    CONSTRAINT chk_events_capacity
        CHECK (capacity > 0)
) ENGINE=InnoDB;

-- ============================================================
-- 5. REGISTRATIONS
-- ============================================================

CREATE TABLE registrations (
    id_registration INT AUTO_INCREMENT PRIMARY KEY,
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
    id_user INT NOT NULL,
    id_event INT NOT NULL,

    CONSTRAINT fk_registrations_user
        FOREIGN KEY (id_user)
        REFERENCES users(id_user),

    CONSTRAINT fk_registrations_event
        FOREIGN KEY (id_event)
        REFERENCES events(id_event),

    CONSTRAINT uq_registration_user_event
        UNIQUE (id_user, id_event)
) ENGINE=InnoDB;

-- ============================================================
-- 6. QR CODES
-- ============================================================

CREATE TABLE qr_codes (
    id_qr_code INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(191) NOT NULL UNIQUE,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_registration INT NOT NULL UNIQUE,

    CONSTRAINT fk_qr_codes_registration
        FOREIGN KEY (id_registration)
        REFERENCES registrations(id_registration)
) ENGINE=InnoDB;

-- ============================================================
-- 7. ATTENDANCES
-- ============================================================

CREATE TABLE attendances (
    id_attendance INT AUTO_INCREMENT PRIMARY KEY,
    present BOOLEAN NOT NULL DEFAULT FALSE,
    scanned_at DATETIME NULL,
    id_registration INT NOT NULL UNIQUE,

    CONSTRAINT fk_attendances_registration
        FOREIGN KEY (id_registration)
        REFERENCES registrations(id_registration)
) ENGINE=InnoDB;

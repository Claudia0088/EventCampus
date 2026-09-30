USE eventcampus;

-- ============================================================
-- 1. ROLES
-- ============================================================

CREATE TABLE roles (
    id_role INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

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
);

-- ============================================================
-- 3. CATEGORIES
-- ============================================================

CREATE TABLE categories (
    id_category INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

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
);

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
);

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
);

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
);

-- ============================================================
-- 8. MODERATIONS
-- ============================================================

CREATE TABLE moderations (
    id_moderation INT AUTO_INCREMENT PRIMARY KEY,
    decision VARCHAR(30) NOT NULL,
    comment TEXT,
    decided_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_event INT NOT NULL,
    id_moderator INT NOT NULL,

    CONSTRAINT fk_moderations_event
        FOREIGN KEY (id_event)
        REFERENCES events(id_event),

    CONSTRAINT fk_moderations_moderator
        FOREIGN KEY (id_moderator)
        REFERENCES users(id_user)
);

-- ============================================================
-- 9. NOTIFICATIONS
-- ============================================================

CREATE TABLE notifications (
    id_notification INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_user INT NOT NULL,

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (id_user)
        REFERENCES users(id_user)
);

-- ============================================================
-- 10. FAVORITES
-- ============================================================

CREATE TABLE favorites (
    id_favorite INT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_user INT NOT NULL,
    id_event INT NOT NULL,

    CONSTRAINT fk_favorites_user
        FOREIGN KEY (id_user)
        REFERENCES users(id_user),

    CONSTRAINT fk_favorites_event
        FOREIGN KEY (id_event)
        REFERENCES events(id_event),

    CONSTRAINT uq_favorite_user_event
        UNIQUE (id_user, id_event)
);

-- ============================================================
-- 11. REVIEWS
-- ============================================================

CREATE TABLE reviews (
    id_review INT AUTO_INCREMENT PRIMARY KEY,
    rating INT NOT NULL,
    comment TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_user INT NOT NULL,
    id_event INT NOT NULL,

    CONSTRAINT fk_reviews_user
        FOREIGN KEY (id_user)
        REFERENCES users(id_user),

    CONSTRAINT fk_reviews_event
        FOREIGN KEY (id_event)
        REFERENCES events(id_event),

    CONSTRAINT uq_review_user_event
        UNIQUE (id_user, id_event),

    CONSTRAINT chk_review_rating
        CHECK (rating BETWEEN 1 AND 5)
);

-- ============================================================
-- 12. EVENT IMAGES
-- ============================================================

CREATE TABLE event_images (
    id_event_image INT AUTO_INCREMENT PRIMARY KEY,
    image_url VARCHAR(500) NOT NULL,
    caption VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_event INT NOT NULL,

    CONSTRAINT fk_event_images_event
        FOREIGN KEY (id_event)
        REFERENCES events(id_event)
);

-- ============================================================
-- 13. WAITLISTS
-- ============================================================

CREATE TABLE waitlists (
    id_waitlist INT AUTO_INCREMENT PRIMARY KEY,
    position INT NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    id_user INT NOT NULL,
    id_event INT NOT NULL,

    CONSTRAINT fk_waitlists_user
        FOREIGN KEY (id_user)
        REFERENCES users(id_user),

    CONSTRAINT fk_waitlists_event
        FOREIGN KEY (id_event)
        REFERENCES events(id_event),

    CONSTRAINT uq_waitlist_user_event
        UNIQUE (id_user, id_event)
);

-- ============================================================
-- DONNEES DE BASE : ROLES
-- ============================================================

INSERT INTO roles (name, description) VALUES
('USER', 'Utilisateur standard'),
('MODERATOR', 'Utilisateur pouvant modérer les événements'),
('ADMIN', 'Administrateur de l application');

-- ============================================================
-- DONNEES DE BASE : CATEGORIES
-- ============================================================

INSERT INTO categories (name, description) VALUES
('Sport', 'Événements sportifs'),
('Gaming', 'Jeux vidéo et compétitions'),
('Culture', 'Événements culturels'),
('Formation', 'Formations et conférences'),
('Soirée', 'Événements et soirées étudiantes');
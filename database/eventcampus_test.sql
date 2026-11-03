-- EventCampus : donnees fictives de test uniquement. Voir README-test.md.
-- Importer ce fichier entier dans une seule session, sans ignorer les erreurs.
-- Aucun USE : chaque table cible explicitement eventcampus_test.
-- Refuse une base preexistante, meme vide. Aucun remplacement ni migration.
-- Les gardes protegent aussi les instructions suivantes si l'import continue.
SET @ec_test_created = 0;
SET @ec_test_absent = NOT EXISTS (SELECT 1 FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = 'eventcampus_test');
CREATE DATABASE eventcampus_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @ec_test_created = (@ec_test_absent = 1 AND ROW_COUNT() = 1);

SET @ec_test_sql = IF(@ec_test_created = 1, 'CREATE TABLE eventcampus_test.roles (
    id_role INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'CREATE TABLE eventcampus_test.users (
    id_user INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(191) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT ''ACTIVE'',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_role INT NOT NULL,

    CONSTRAINT fk_users_role
        FOREIGN KEY (id_role)
        REFERENCES eventcampus_test.roles(id_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'CREATE TABLE eventcampus_test.categories (
    id_category INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'CREATE TABLE eventcampus_test.events (
    id_event INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    image VARCHAR(500),
    event_date DATE NOT NULL,
    event_time TIME NOT NULL,
    location VARCHAR(255) NOT NULL,
    capacity INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT ''EN_ATTENTE'',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    id_creator INT NOT NULL,
    id_category INT NOT NULL,

    CONSTRAINT fk_events_creator
        FOREIGN KEY (id_creator)
        REFERENCES eventcampus_test.users(id_user),

    CONSTRAINT fk_events_category
        FOREIGN KEY (id_category)
        REFERENCES eventcampus_test.categories(id_category),

    CONSTRAINT chk_events_capacity
        CHECK (capacity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'CREATE TABLE eventcampus_test.registrations (
    id_registration INT AUTO_INCREMENT PRIMARY KEY,
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT ''REGISTERED'',
    id_user INT NOT NULL,
    id_event INT NOT NULL,

    CONSTRAINT fk_registrations_user
        FOREIGN KEY (id_user)
        REFERENCES eventcampus_test.users(id_user),

    CONSTRAINT fk_registrations_event
        FOREIGN KEY (id_event)
        REFERENCES eventcampus_test.events(id_event),

    CONSTRAINT uq_registration_user_event
        UNIQUE (id_user, id_event)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'CREATE TABLE eventcampus_test.qr_codes (
    id_qr_code INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(191) NOT NULL UNIQUE,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_registration INT NOT NULL UNIQUE,

    CONSTRAINT fk_qr_codes_registration
        FOREIGN KEY (id_registration)
        REFERENCES eventcampus_test.registrations(id_registration)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'CREATE TABLE eventcampus_test.attendances (
    id_attendance INT AUTO_INCREMENT PRIMARY KEY,
    present BOOLEAN NOT NULL DEFAULT FALSE,
    scanned_at DATETIME NULL,
    id_registration INT NOT NULL UNIQUE,

    CONSTRAINT fk_attendances_registration
        FOREIGN KEY (id_registration)
        REFERENCES eventcampus_test.registrations(id_registration)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'CREATE TABLE eventcampus_test.moderations (
    id_moderation INT AUTO_INCREMENT PRIMARY KEY,
    decision VARCHAR(30) NOT NULL,
    comment TEXT,
    decided_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_event INT NOT NULL,
    id_moderator INT NOT NULL,

    CONSTRAINT fk_moderations_event
        FOREIGN KEY (id_event)
        REFERENCES eventcampus_test.events(id_event),

    CONSTRAINT fk_moderations_moderator
        FOREIGN KEY (id_moderator)
        REFERENCES eventcampus_test.users(id_user)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'CREATE TABLE eventcampus_test.notifications (
    id_notification INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_user INT NOT NULL,

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (id_user)
        REFERENCES eventcampus_test.users(id_user)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'CREATE TABLE eventcampus_test.favorites (
    id_favorite INT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_user INT NOT NULL,
    id_event INT NOT NULL,

    CONSTRAINT fk_favorites_user
        FOREIGN KEY (id_user)
        REFERENCES eventcampus_test.users(id_user),

    CONSTRAINT fk_favorites_event
        FOREIGN KEY (id_event)
        REFERENCES eventcampus_test.events(id_event),

    CONSTRAINT uq_favorite_user_event
        UNIQUE (id_user, id_event)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'CREATE TABLE eventcampus_test.reviews (
    id_review INT AUTO_INCREMENT PRIMARY KEY,
    rating INT NOT NULL,
    comment TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_user INT NOT NULL,
    id_event INT NOT NULL,

    CONSTRAINT fk_reviews_user
        FOREIGN KEY (id_user)
        REFERENCES eventcampus_test.users(id_user),

    CONSTRAINT fk_reviews_event
        FOREIGN KEY (id_event)
        REFERENCES eventcampus_test.events(id_event),

    CONSTRAINT uq_review_user_event
        UNIQUE (id_user, id_event),

    CONSTRAINT chk_review_rating
        CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'CREATE TABLE eventcampus_test.event_images (
    id_event_image INT AUTO_INCREMENT PRIMARY KEY,
    image_url VARCHAR(500) NOT NULL,
    caption VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_event INT NOT NULL,

    CONSTRAINT fk_event_images_event
        FOREIGN KEY (id_event)
        REFERENCES eventcampus_test.events(id_event)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'CREATE TABLE eventcampus_test.waitlists (
    id_waitlist INT AUTO_INCREMENT PRIMARY KEY,
    position INT NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT ''WAITING'',
    id_user INT NOT NULL,
    id_event INT NOT NULL,

    CONSTRAINT fk_waitlists_user
        FOREIGN KEY (id_user)
        REFERENCES eventcampus_test.users(id_user),

    CONSTRAINT fk_waitlists_event
        FOREIGN KEY (id_event)
        REFERENCES eventcampus_test.events(id_event),

    CONSTRAINT uq_waitlist_user_event
        UNIQUE (id_user, id_event)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'INSERT INTO eventcampus_test.roles (name, description) VALUES
(''USER'', ''Utilisateur standard''),
(''MODERATOR'', ''Utilisateur pouvant modérer les événements''),
(''ADMIN'', ''Administrateur de l application'')', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'INSERT INTO eventcampus_test.categories (name, description) VALUES
(''Sport'', ''Événements sportifs''),
(''Gaming'', ''Jeux vidéo et compétitions''),
(''Culture'', ''Événements culturels''),
(''Formation'', ''Formations et conférences''),
(''Soirée'', ''Événements et soirées étudiantes'')', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'INSERT INTO eventcampus_test.users (id_user, first_name, last_name, email, password, status, created_at, id_role) VALUES
(1, ''Admin'', ''Demo'', ''admin@eventcampus.test'', ''$2a$10$fPc76fsrK1IoJwOITmXTtO/GZNrYFzXs6T7785UBjiRrvWUxHq7VS'', ''ACTIVE'', CURRENT_TIMESTAMP, 3),
(2, ''Moderator'', ''Demo'', ''moderator@eventcampus.test'', ''$2a$10$VrCXL/an15PoCDg29C//1OgpCe82bjN/jOhoRjxHc58V9YA6IKzhm'', ''ACTIVE'', CURRENT_TIMESTAMP, 2),
(3, ''User'', ''Demo'', ''user@eventcampus.test'', ''$2a$10$PoREz0GTwni.7UhguVnB8OVLeFuZJ4kyMe.4kJm7qh.M4Xh8ZhJt.'', ''ACTIVE'', CURRENT_TIMESTAMP, 1)', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_sql = IF(@ec_test_created = 1, 'INSERT INTO eventcampus_test.events (title, description, event_date, event_time, location, capacity, status, id_creator, id_category) VALUES
(''Demo - Atelier a approuver'', ''Evenement fictif pour tester une approbation.'', DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), ''14:00:00'', ''Campus fictif - Salle A'', 20, ''EN_ATTENTE'', 3, 4),
(''Demo - Rencontre a refuser'', ''Evenement fictif pour tester un refus.'', DATE_ADD(CURRENT_DATE, INTERVAL 31 DAY), ''15:00:00'', ''Campus fictif - Salle B'', 30, ''EN_ATTENTE'', 3, 3),
(''Demo - Tournoi publie'', ''Evenement fictif ouvert aux inscriptions.'', DATE_ADD(CURRENT_DATE, INTERVAL 45 DAY), ''16:00:00'', ''Campus fictif - Salle C'', 40, ''PUBLIE'', 3, 2),
(''Demo - Proposition refusee'', ''Evenement fictif deja refuse.'', DATE_ADD(CURRENT_DATE, INTERVAL 46 DAY), ''17:00:00'', ''Campus fictif - Salle D'', 15, ''REFUSE'', 3, 1)', 'SELECT ''Import bloque : base non creee par cette session.'' AS message');
PREPARE ec_test_stmt FROM @ec_test_sql;
EXECUTE ec_test_stmt;
DEALLOCATE PREPARE ec_test_stmt;

SET @ec_test_created = 0;
SET @ec_test_absent = NULL;
SET @ec_test_sql = NULL;

-- Utilisateurs de l'application. Le mot de passe n'est jamais stocké en clair : seulement son empreinte BCrypt.
CREATE TABLE app_user (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,   -- ADMIN (import/suppression) ou USER (consultation et chat)
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

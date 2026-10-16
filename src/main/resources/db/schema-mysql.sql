-- ---------------------------------------------------------------------------
-- Esquema de la API de franquicias - dialecto MySQL 8 (perfil "mysql").
--
-- Unico cambio respecto a db/schema-h2.sql: MySQL no admite
-- "CREATE INDEX IF NOT EXISTS" ni "IF NOT EXISTS" sobre indices, asi que los
-- indices se decluran dentro del CREATE TABLE. Todas las tablas usan
-- "CREATE TABLE IF NOT EXISTS", de modo que el script es idempotente y se puede
-- ejecutar en cada arranque (SQL_INIT_MODE=always).
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS franchises (
    id       CHAR(36)     NOT NULL,
    name     VARCHAR(120) NOT NULL,
    name_key VARCHAR(120) NOT NULL,
    CONSTRAINT pk_franchises PRIMARY KEY (id),
    CONSTRAINT uq_franchises_name_key UNIQUE (name_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS branches (
    id           CHAR(36)     NOT NULL,
    franchise_id CHAR(36)     NOT NULL,
    name         VARCHAR(120) NOT NULL,
    name_key     VARCHAR(120) NOT NULL,
    CONSTRAINT pk_branches PRIMARY KEY (id),
    CONSTRAINT uq_branches_franchise_name UNIQUE (franchise_id, name_key),
    CONSTRAINT fk_branches_franchise FOREIGN KEY (franchise_id)
        REFERENCES franchises (id) ON DELETE CASCADE,
    INDEX ix_branches_franchise (franchise_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS products (
    id        CHAR(36)     NOT NULL,
    branch_id CHAR(36)     NOT NULL,
    name      VARCHAR(120) NOT NULL,
    name_key  VARCHAR(120) NOT NULL,
    stock     INT          NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_branch_name UNIQUE (branch_id, name_key),
    CONSTRAINT ck_products_stock CHECK (stock >= 0),
    CONSTRAINT fk_products_branch FOREIGN KEY (branch_id)
        REFERENCES branches (id) ON DELETE CASCADE,
    INDEX ix_products_branch (branch_id),
    INDEX ix_products_branch_stock (branch_id, stock)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

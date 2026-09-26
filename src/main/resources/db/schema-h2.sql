-- ---------------------------------------------------------------------------
-- Esquema de la API de franquicias - dialecto H2 (perfil por defecto y tests).
--
-- H2 acepta "CREATE INDEX IF NOT EXISTS", que MySQL no, por eso el DDL esta
-- duplicado en db/schema-mysql.sql con la unica diferencia del bloque de indices.
-- El resto (tablas, claves, constraints) es identico en ambos motores.
--
-- Los identificadores los genera la aplicacion (UUID v4), por eso no hay
-- AUTO_INCREMENT y el DDL es idempotente: se puede ejecutar en cada arranque.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS franchises (
    id       CHAR(36)     NOT NULL,
    name     VARCHAR(120) NOT NULL,
    name_key VARCHAR(120) NOT NULL,
    CONSTRAINT pk_franchises PRIMARY KEY (id),
    CONSTRAINT uq_franchises_name_key UNIQUE (name_key)
);

CREATE TABLE IF NOT EXISTS branches (
    id           CHAR(36)     NOT NULL,
    franchise_id CHAR(36)     NOT NULL,
    name         VARCHAR(120) NOT NULL,
    name_key     VARCHAR(120) NOT NULL,
    CONSTRAINT pk_branches PRIMARY KEY (id),
    CONSTRAINT uq_branches_franchise_name UNIQUE (franchise_id, name_key),
    CONSTRAINT fk_branches_franchise FOREIGN KEY (franchise_id)
        REFERENCES franchises (id) ON DELETE CASCADE
);

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
        REFERENCES branches (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_branches_franchise ON branches (franchise_id);
CREATE INDEX IF NOT EXISTS ix_products_branch ON products (branch_id);
CREATE INDEX IF NOT EXISTS ix_products_branch_stock ON products (branch_id, stock);

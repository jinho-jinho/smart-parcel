-- =========================================================
-- 0) 타입/함수 정리 (선택)
-- =========================================================
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN DROP TYPE user_role; END IF;
  IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'input_type') THEN DROP TYPE input_type; END IF;
  IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'verification_purpose') THEN DROP TYPE verification_purpose; END IF;
EXCEPTION WHEN undefined_object THEN NULL;
END $$;

-- 트리거 함수가 기존에 있으면 제거
DROP FUNCTION IF EXISTS fill_snapshots() CASCADE;

-- =========================================================
-- 1) ENUM 타입
-- =========================================================
CREATE TYPE user_role            AS ENUM ('MANAGER', 'STAFF');
CREATE TYPE input_type           AS ENUM ('TEXT', 'COLOR');
CREATE TYPE verification_purpose AS ENUM ('SIGNUP', 'RESET_PASSWORD');

-- =========================================================
-- 2) users
-- =========================================================
CREATE TABLE users (
                       id          BIGSERIAL     PRIMARY KEY,
                       email       VARCHAR(100)  NOT NULL,
                       name        VARCHAR(100)  NOT NULL,
                       password    VARCHAR(255)  NOT NULL,
                       biz_number  VARCHAR(20),
                       role        user_role     NOT NULL DEFAULT 'STAFF',
                       created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                       manager_id  BIGINT,
                       CONSTRAINT fk_users_manager
                           FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL,
                       CONSTRAINT uq_users_email UNIQUE (email)
);

-- =========================================================
-- 3) sorting_groups  (활성 그룹은 하나만)
-- =========================================================
CREATE TABLE sorting_groups (
                                id         BIGSERIAL    PRIMARY KEY,
                                group_name VARCHAR(50)  NOT NULL,
                                updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                enabled    BOOLEAN      NOT NULL DEFAULT FALSE,
                                manager_id BIGINT       NOT NULL,
                                CONSTRAINT fk_groups_manager
                                    FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX ux_only_one_enabled_group
    ON sorting_groups ((enabled))
  WHERE enabled = TRUE;

CREATE INDEX idx_groups_manager_id ON sorting_groups(manager_id);

-- =========================================================
-- 4) chutes
-- =========================================================
CREATE TABLE chutes (
                        id         BIGSERIAL    PRIMARY KEY,
                        chute_name VARCHAR(50)  NOT NULL,
                        servo_deg  SMALLINT     NOT NULL,
                        created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                        CONSTRAINT uq_chute UNIQUE (servo_deg, chute_name)
);

-- =========================================================
-- 5) sorting_rules
-- =========================================================
CREATE TABLE sorting_rules (
                               id          BIGSERIAL    PRIMARY KEY,
                               rule_name   VARCHAR(50)  NOT NULL,
                               input_type  input_type   NOT NULL,
                               input_value VARCHAR(50)  NOT NULL,
                               created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                               group_id    BIGINT       NOT NULL,
                               item_name   VARCHAR(100) NOT NULL,
                               CONSTRAINT fk_rules_group
                                   FOREIGN KEY (group_id) REFERENCES sorting_groups(id) ON DELETE CASCADE
);

CREATE INDEX idx_rules_group_id ON sorting_rules(group_id);
CREATE INDEX idx_rules_input_type_value ON sorting_rules(input_type, input_value);

-- =========================================================
-- 6) rule_chutes (N:M)
-- =========================================================
CREATE TABLE rule_chutes (
                             id       BIGSERIAL PRIMARY KEY,
                             rule_id  BIGINT    NOT NULL,
                             chute_id BIGINT    NOT NULL,
                             CONSTRAINT fk_rc_rule  FOREIGN KEY (rule_id)  REFERENCES sorting_rules(id) ON DELETE CASCADE,
                             CONSTRAINT fk_rc_chute FOREIGN KEY (chute_id) REFERENCES chutes(id)        ON DELETE CASCADE,
                             CONSTRAINT uq_rule_chute UNIQUE (rule_id, chute_id)
);

CREATE INDEX idx_rc_rule_id  ON rule_chutes(rule_id);
CREATE INDEX idx_rc_chute_id ON rule_chutes(chute_id);

-- =========================================================
-- 7) sorting_history  (스냅샷: 그룹명, 라인명만)
-- =========================================================
CREATE TABLE sorting_history (
                                 id            BIGSERIAL    PRIMARY KEY,
                                 image_url     VARCHAR(512) NOT NULL,
                                 processed_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- 스냅샷(표시용 값 고정)
                                 sorting_group_name_snapshot VARCHAR(50) NOT NULL,
                                 chute_name_snapshot         VARCHAR(50) NOT NULL,

    -- 기준 FK
                                 manager_id BIGINT NOT NULL,
                                 group_id   BIGINT,
                                 chute_id   BIGINT,

                                 CONSTRAINT fk_hist_manager FOREIGN KEY (manager_id) REFERENCES users(id)          ON DELETE SET NULL,
                                 CONSTRAINT fk_hist_group   FOREIGN KEY (group_id)   REFERENCES sorting_groups(id) ON DELETE SET NULL,
                                 CONSTRAINT fk_hist_chute   FOREIGN KEY (chute_id)   REFERENCES chutes(id)         ON DELETE SET NULL
);

CREATE INDEX idx_hist_processed_at ON sorting_history(processed_at);
CREATE INDEX idx_hist_chute_id     ON sorting_history(chute_id);
CREATE INDEX idx_hist_manager_id   ON sorting_history(manager_id);

-- =========================================================
-- 8) error_logs  (스냅샷: 그룹명, 라인명만 / servo_deg_snapshot 제거)
-- =========================================================
CREATE TABLE error_logs (
                            id           BIGSERIAL    PRIMARY KEY,
                            error_code   VARCHAR(50)  NOT NULL,
                            occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                            image_url    VARCHAR(512),

    -- 스냅샷
                            sorting_group_name_snapshot VARCHAR(50),
                            chute_name_snapshot         VARCHAR(50),

    -- 기준 FK (NULL 허용)
                            manager_id BIGINT NOT NULL,
                            group_id   BIGINT,
                            chute_id   BIGINT,

                            CONSTRAINT fk_err_manager FOREIGN KEY (manager_id) REFERENCES users(id)          ON DELETE SET NULL,
                            CONSTRAINT fk_err_group   FOREIGN KEY (group_id)   REFERENCES sorting_groups(id) ON DELETE SET NULL,
                            CONSTRAINT fk_err_chute   FOREIGN KEY (chute_id)   REFERENCES chutes(id)         ON DELETE SET NULL
);

CREATE INDEX idx_error_code       ON error_logs(error_code);
CREATE INDEX idx_error_occurred   ON error_logs(occurred_at);
CREATE INDEX idx_error_manager_id ON error_logs(manager_id);

-- =========================================================
-- 9) user_notifications
-- =========================================================
CREATE TABLE user_notifications (
                                    id                BIGSERIAL   PRIMARY KEY,
                                    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    is_read           BOOLEAN     NOT NULL DEFAULT FALSE,
                                    error_log_id      BIGINT      NOT NULL,
                                    recipient_user_id BIGINT      NOT NULL,
                                    CONSTRAINT fk_un_error     FOREIGN KEY (error_log_id)      REFERENCES error_logs(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_un_recipient FOREIGN KEY (recipient_user_id)  REFERENCES users(id)     ON DELETE CASCADE
);

CREATE INDEX idx_un_read_recipient ON user_notifications(recipient_user_id, is_read);

-- =========================================================
-- 10) email_verifications
--     - 스프링 엔티티(@Index, @UniqueConstraint) 이름 매칭
--     - purpose: ENUM(verification_purpose)
-- =========================================================
CREATE TABLE email_verifications (
                                     id         BIGSERIAL            PRIMARY KEY,
                                     email      VARCHAR(255)         NOT NULL,
                                     purpose    verification_purpose NOT NULL,
                                     code       VARCHAR(10)          NOT NULL,
                                     expires_at TIMESTAMPTZ          NOT NULL,
                                     verified   BOOLEAN              NOT NULL DEFAULT FALSE,
                                     CONSTRAINT ux_email_purpose UNIQUE (email, purpose)
);

-- 스프링 @Index(name="idx_email_purpose", columnList="email, purpose")
CREATE INDEX idx_email_purpose ON email_verifications(email, purpose);

-- =========================================================
-- 11) 스냅샷 자동 채움 트리거 (servo_deg_snapshot 없이)
-- =========================================================
CREATE OR REPLACE FUNCTION fill_snapshots() RETURNS trigger AS $$
BEGIN
  IF NEW.group_id IS NOT NULL THEN
SELECT g.group_name
INTO NEW.sorting_group_name_snapshot
FROM sorting_groups g
WHERE g.id = NEW.group_id;
END IF;

  IF NEW.chute_id IS NOT NULL THEN
SELECT c.chute_name
INTO NEW.chute_name_snapshot
FROM chutes c
WHERE c.id = NEW.chute_id;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_history_snap
    BEFORE INSERT ON sorting_history
    FOR EACH ROW EXECUTE FUNCTION fill_snapshots();

CREATE TRIGGER trg_error_snap
    BEFORE INSERT ON error_logs
    FOR EACH ROW EXECUTE FUNCTION fill_snapshots();

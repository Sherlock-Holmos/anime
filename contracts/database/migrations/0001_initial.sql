BEGIN;

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    display_name VARCHAR(80) NOT NULL,
    avatar_url TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'user'
        CHECK (role IN ('user', 'maintainer')),
    status VARCHAR(20) NOT NULL DEFAULT 'active'
        CHECK (status IN ('active', 'suspended', 'deleted')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CHECK (btrim(display_name) <> ''),
    CHECK ((status = 'deleted') = (deleted_at IS NOT NULL))
);

CREATE TABLE external_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL CHECK (provider = 'bangumi'),
    provider_user_id VARCHAR(100) NOT NULL,
    provider_username VARCHAR(100) NOT NULL,
    access_token_encrypted BYTEA NOT NULL,
    refresh_token_encrypted BYTEA,
    expires_at TIMESTAMPTZ,
    scopes TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (provider, provider_user_id),
    UNIQUE (user_id, provider),
    CHECK (btrim(provider_user_id) <> ''),
    CHECK (btrim(provider_username) <> '')
);

CREATE TABLE oauth_requests (
    id UUID PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL UNIQUE,
    state_hash BYTEA NOT NULL UNIQUE,
    callback_uri TEXT NOT NULL,
    pending_action_id VARCHAR(128),
    app_ticket_hash BYTEA UNIQUE,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('pending', 'callback_received', 'ticket_issued', 'consumed', 'expired')),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CHECK (callback_uri LIKE 'https://%')
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id UUID NOT NULL,
    token_hash BYTEA NOT NULL UNIQUE,
    device_label VARCHAR(100),
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID REFERENCES refresh_tokens(id) ON DELETE SET NULL,
    CHECK (expires_at > issued_at)
);

CREATE INDEX refresh_tokens_user_active_idx
    ON refresh_tokens (user_id, expires_at DESC)
    WHERE revoked_at IS NULL;
CREATE INDEX refresh_tokens_family_idx ON refresh_tokens (family_id);

CREATE TABLE subjects (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bgm_id BIGINT NOT NULL UNIQUE CHECK (bgm_id > 0),
    subject_type VARCHAR(20) NOT NULL
        CHECK (subject_type IN ('tv', 'web', 'ova', 'movie', 'other')),
    name TEXT NOT NULL,
    name_cn TEXT,
    summary TEXT,
    air_date DATE,
    end_date DATE,
    air_status VARCHAR(20) NOT NULL
        CHECK (air_status IN ('announced', 'airing', 'finished', 'unknown')),
    episode_count INTEGER CHECK (episode_count IS NULL OR episode_count >= 0),
    image_url TEXT,
    backdrop_url TEXT,
    source_score NUMERIC(4, 2)
        CHECK (source_score IS NULL OR source_score BETWEEN 0 AND 10),
    source_votes INTEGER NOT NULL DEFAULT 0 CHECK (source_votes >= 0),
    source_rating_distribution JSONB,
    raw_data JSONB NOT NULL,
    source_updated_at TIMESTAMPTZ,
    synced_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (btrim(name) <> ''),
    CHECK (end_date IS NULL OR air_date IS NULL OR end_date >= air_date),
    CHECK (source_votes <> 0 OR source_score IS NULL),
    CHECK (jsonb_typeof(raw_data) = 'object'),
    CHECK (
        source_rating_distribution IS NULL
        OR jsonb_typeof(source_rating_distribution) = 'object'
    )
);

CREATE INDEX subjects_air_date_idx ON subjects (air_date);
CREATE INDEX subjects_air_status_idx ON subjects (air_status, bgm_id);
CREATE INDEX subjects_type_idx ON subjects (subject_type, bgm_id);
CREATE INDEX subjects_name_trgm_idx ON subjects USING GIN (name gin_trgm_ops);
CREATE INDEX subjects_name_cn_trgm_idx ON subjects USING GIN (name_cn gin_trgm_ops);

CREATE TABLE subject_aliases (
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    alias TEXT NOT NULL,
    normalized_alias TEXT NOT NULL,
    alias_type VARCHAR(20) NOT NULL
        CHECK (alias_type IN ('cn', 'jp', 'romanized', 'other')),
    PRIMARY KEY (subject_id, normalized_alias),
    CHECK (btrim(alias) <> ''),
    CHECK (btrim(normalized_alias) <> '')
);

CREATE INDEX subject_aliases_normalized_trgm_idx
    ON subject_aliases USING GIN (normalized_alias gin_trgm_ops);

CREATE TABLE episodes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bgm_id BIGINT NOT NULL UNIQUE CHECK (bgm_id > 0),
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    episode_type VARCHAR(20) NOT NULL
        CHECK (episode_type IN ('main', 'special', 'opening', 'ending', 'other')),
    number NUMERIC(8, 2),
    sort_order INTEGER NOT NULL,
    name TEXT,
    name_cn TEXT,
    air_date DATE,
    air_status VARCHAR(20) NOT NULL
        CHECK (air_status IN ('unreleased', 'aired', 'delayed', 'unknown')),
    duration_seconds INTEGER CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    raw_data JSONB NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (jsonb_typeof(raw_data) = 'object')
);

CREATE INDEX episodes_subject_sort_idx
    ON episodes (subject_id, episode_type, sort_order, id);

CREATE TABLE characters (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bgm_id BIGINT NOT NULL UNIQUE CHECK (bgm_id > 0),
    name TEXT NOT NULL,
    image_url TEXT,
    raw_data JSONB NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (btrim(name) <> ''),
    CHECK (jsonb_typeof(raw_data) = 'object')
);

CREATE TABLE persons (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bgm_id BIGINT NOT NULL UNIQUE CHECK (bgm_id > 0),
    name TEXT NOT NULL,
    image_url TEXT,
    raw_data JSONB NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (btrim(name) <> ''),
    CHECK (jsonb_typeof(raw_data) = 'object')
);

CREATE TABLE subject_characters (
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    character_id BIGINT NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    relation_label VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (subject_id, character_id, relation_label)
);

CREATE TABLE character_actors (
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    character_id BIGINT NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    person_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (subject_id, character_id, person_id)
);

CREATE TABLE subject_persons (
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    person_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    role_label VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (subject_id, person_id, role_label)
);

CREATE TABLE subject_relations (
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    related_subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    relation_kind VARCHAR(30) NOT NULL
        CHECK (
            relation_kind IN (
                'prequel', 'sequel', 'same_world', 'alternative',
                'character', 'summary', 'other'
            )
        ),
    relation_label VARCHAR(100) NOT NULL,
    PRIMARY KEY (subject_id, related_subject_id, relation_kind),
    CHECK (subject_id <> related_subject_id)
);

CREATE TABLE subject_stats (
    subject_id BIGINT PRIMARY KEY REFERENCES subjects(id) ON DELETE CASCADE,
    collection_count INTEGER NOT NULL DEFAULT 0 CHECK (collection_count >= 0),
    comment_count INTEGER NOT NULL DEFAULT 0 CHECK (comment_count >= 0),
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE user_subject_collections (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('wish', 'watching', 'completed', 'on_hold', 'dropped')),
    watched_episodes INTEGER NOT NULL DEFAULT 0 CHECK (watched_episodes >= 0),
    note VARCHAR(300),
    private BOOLEAN NOT NULL DEFAULT FALSE,
    source VARCHAR(20) NOT NULL
        CHECK (source IN ('anime', 'bangumi_import')),
    version BIGINT NOT NULL DEFAULT 1 CHECK (version >= 1),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, subject_id)
);

CREATE INDEX user_subject_collections_page_idx
    ON user_subject_collections (user_id, status, updated_at DESC, subject_id DESC);

CREATE TABLE user_subject_sync_state (
    user_id UUID NOT NULL,
    subject_id BIGINT NOT NULL,
    local_version BIGINT NOT NULL DEFAULT 1 CHECK (local_version >= 1),
    last_synced_local_version BIGINT NOT NULL DEFAULT 0
        CHECK (last_synced_local_version >= 0),
    remote_snapshot_hash VARCHAR(64),
    dirty_fields TEXT[] NOT NULL DEFAULT '{}',
    sync_status VARCHAR(20) NOT NULL
        CHECK (
            sync_status IN (
                'synced', 'pending', 'pushing', 'retry_wait',
                'auth_required', 'conflict', 'blocked'
            )
        ),
    next_retry_at TIMESTAMPTZ,
    last_sync_at TIMESTAMPTZ,
    last_error_code VARCHAR(50),
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, subject_id),
    FOREIGN KEY (user_id, subject_id)
        REFERENCES user_subject_collections(user_id, subject_id)
        ON DELETE CASCADE,
    CHECK (last_synced_local_version <= local_version)
);

CREATE TABLE sync_outbox (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    operation VARCHAR(30) NOT NULL
        CHECK (operation IN ('collection', 'progress', 'delete')),
    target_version BIGINT NOT NULL CHECK (target_version >= 1),
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('pending', 'processing', 'retry_wait', 'done', 'blocked')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    available_at TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ,
    locked_by VARCHAR(100),
    last_error_code VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (jsonb_typeof(payload) = 'object'),
    CHECK ((locked_at IS NULL) = (locked_by IS NULL))
);

CREATE INDEX sync_outbox_claim_idx
    ON sync_outbox (status, available_at, created_at)
    WHERE status IN ('pending', 'retry_wait');
CREATE INDEX sync_outbox_subject_idx
    ON sync_outbox (user_id, subject_id, operation, status);

CREATE TABLE sync_conflicts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    local_version BIGINT NOT NULL CHECK (local_version >= 1),
    field_name VARCHAR(50) NOT NULL
        CHECK (field_name IN ('status', 'watched_episodes', 'note')),
    local_value JSONB NOT NULL,
    remote_value JSONB NOT NULL,
    remote_snapshot_hash VARCHAR(64),
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('unresolved', 'keep_local', 'use_remote', 'dismissed')),
    detected_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    resolved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    CHECK (
        (status = 'unresolved' AND resolved_at IS NULL)
        OR (status <> 'unresolved' AND resolved_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX sync_conflicts_unresolved_field_idx
    ON sync_conflicts (user_id, subject_id, field_name)
    WHERE status = 'unresolved';

CREATE TABLE sync_runs (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    job_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('running', 'succeeded', 'partial', 'failed', 'cancelled')),
    cursor TEXT,
    request_count INTEGER NOT NULL DEFAULT 0 CHECK (request_count >= 0),
    success_count INTEGER NOT NULL DEFAULT 0 CHECK (success_count >= 0),
    failure_count INTEGER NOT NULL DEFAULT 0 CHECK (failure_count >= 0),
    rate_limited_count INTEGER NOT NULL DEFAULT 0 CHECK (rate_limited_count >= 0),
    error_summary VARCHAR(500),
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    CHECK (finished_at IS NULL OR finished_at >= started_at)
);

CREATE TABLE comments (
    id UUID PRIMARY KEY,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES comments(id) ON DELETE SET NULL,
    content VARCHAR(300) NOT NULL,
    spoiler BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('visible', 'pending', 'hidden', 'deleted')),
    moderation_reason VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CHECK (btrim(content) <> ''),
    CHECK ((status = 'deleted') = (deleted_at IS NOT NULL))
);

CREATE INDEX comments_subject_newest_idx
    ON comments (subject_id, created_at DESC, id DESC)
    WHERE status = 'visible';
CREATE INDEX comments_subject_oldest_idx
    ON comments (subject_id, created_at, id)
    WHERE status = 'visible';
CREATE INDEX comments_user_idx ON comments (user_id, created_at DESC);

CREATE TABLE comment_reports (
    id UUID PRIMARY KEY,
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    reporter_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason_code VARCHAR(20) NOT NULL
        CHECK (reason_code IN ('spam', 'harassment', 'spoiler', 'illegal', 'other')),
    details VARCHAR(300),
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('open', 'resolved', 'dismissed', 'withdrawn')),
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    resolved_by UUID REFERENCES users(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX comment_reports_one_active_idx
    ON comment_reports (comment_id, reporter_user_id)
    WHERE status IN ('open', 'resolved', 'dismissed');

CREATE TABLE idempotency_keys (
    scope_key VARCHAR(160) NOT NULL,
    principal_id UUID REFERENCES users(id) ON DELETE CASCADE,
    route_key VARCHAR(120) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash BYTEA NOT NULL,
    response_status SMALLINT,
    response_body JSONB,
    resource_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (scope_key, route_key, idempotency_key),
    CHECK (btrim(scope_key) <> ''),
    CHECK (char_length(idempotency_key) BETWEEN 16 AND 128),
    CHECK (expires_at > created_at),
    CHECK (
        (response_status IS NULL AND response_body IS NULL)
        OR (response_status BETWEEN 200 AND 599 AND response_body IS NOT NULL)
    )
);

CREATE INDEX idempotency_keys_expiry_idx ON idempotency_keys (expires_at);

COMMIT;

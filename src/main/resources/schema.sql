-- Teams Table
CREATE TABLE IF NOT EXISTS teams (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    tag TEXT NOT NULL,
    owner_uuid TEXT NOT NULL,

    home_world TEXT,
    home_x REAL,
    home_y REAL,
    home_z REAL,

    created_at INTEGER NOT NULL
);

-- Team Members Table
CREATE TABLE IF NOT EXISTS team_members (
    id TEXT PRIMARY KEY,
    team_id INTEGER NOT NULL,
    player_uuid TEXT NOT NULL,
    role TEXT NOT NULL, -- MEMBER, MOD, ADMIN
    FOREIGN KEY(team_id) REFERENCES teams(id)
);

-- Audit Log Table
CREATE TABLE IF NOT EXISTS audit_log (
    id TEXT PRIMARY KEY,
    team_id INTEGER NOT NULL,
    actor_uuid TEXT NOT NULL,
    action TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    info TEXT,
    FOREIGN KEY(team_id) REFERENCES teams(id)
);

-- Team Ban List
CREATE TABLE IF NOT EXISTS team_bans (
    team_id TEXT NOT NULL,
    player_uuid TEXT NOT NULL,
    executor_uuid TEXT NOT NULL,
    reason TEXT,
    expires_at INTEGER,
    executed_at INTEGER NOT NULL,
    PRIMARY KEY (team_id, player_uuid)
);

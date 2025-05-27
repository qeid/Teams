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

CREATE TABLE IF NOT EXISTS team_members (
    team_id TEXT NOT NULL,
    player_uuid TEXT NOT NULL,
    role TEXT NOT NULL,
    PRIMARY KEY (team_id, player_uuid),
    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
);



CREATE TABLE IF NOT EXISTS team_bans (
    team_id TEXT NOT NULL,
    player_uuid TEXT NOT NULL,
    executor_uuid TEXT NOT NULL,
    reason TEXT,
    expires_at INTEGER,
    executed_at INTEGER NOT NULL,
    PRIMARY KEY (team_id, player_uuid),
    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS team_invites (
    player_uuid TEXT NOT NULL,
    team_id TEXT NOT NULL,
    invited_at INTEGER NOT NULL,
    PRIMARY KEY (player_uuid, team_id),
    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS team_audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    team_id TEXT NOT NULL,
    executor_uuid TEXT NOT NULL,
    action TEXT NOT NULL,
    info TEXT,
    timestamp INTEGER NOT NULL,
    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
);

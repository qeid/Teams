package discord.qeid.model;

import java.util.UUID;

public record AuditLogEntry(
    int id,
    String teamId,
    UUID executor,
    String action,
    String info,
    long timestamp
) {}


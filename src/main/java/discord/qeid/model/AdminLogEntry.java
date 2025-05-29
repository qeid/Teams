package discord.qeid.model;

import java.util.UUID;

public record AdminLogEntry(
    int id,
    UUID executor,
    String executorName,
    String action,
    String teamName,
    String targetName,
    String reason,
    long timestamp
) {}

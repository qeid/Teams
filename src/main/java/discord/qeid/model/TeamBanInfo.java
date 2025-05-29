package discord.qeid.model;

import java.util.UUID;

public record TeamBanInfo(UUID player, UUID executor, String reason, long expiresAt, long executedAt) {}



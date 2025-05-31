package discord.qeid.model;

import java.util.UUID;
/**
 * Represents an entry in the admin log.
 * This record is used to store information about actions taken by administrators
 * on teams, including the executor, action performed, target team or player,
 * reason for the action, and the timestamp of when the action occurred.
 */

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

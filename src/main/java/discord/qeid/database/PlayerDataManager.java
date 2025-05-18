package discord.qeid.database;

import java.util.*;

public class PlayerDataManager {

    private final Map<UUID, Set<String>> pendingInvites = new HashMap<>();

    public void addInvite(UUID target, String teamId) {
        pendingInvites.computeIfAbsent(target, k -> new HashSet<>()).add(teamId);
    }

    public boolean hasInvite(UUID target, String teamId) {
        return pendingInvites.getOrDefault(target, Collections.emptySet()).contains(teamId);
    }

    public void removeInvite(UUID target, String teamId) {
        Set<String> invites = pendingInvites.get(target);
        if (invites != null) {
            invites.remove(teamId);
            if (invites.isEmpty()) {
                pendingInvites.remove(target);
            }
        }
    }

    public Set<String> getInvites(UUID target) {
        return pendingInvites.getOrDefault(target, Collections.emptySet());
    }

    public void clear(UUID target) {
        pendingInvites.remove(target);
    }
}

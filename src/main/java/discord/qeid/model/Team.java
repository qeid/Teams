package discord.qeid.model;

import java.util.Set;
import java.util.UUID;

public class Team {

    private final String id;
    private final String name;
    private final String tag;
    private final UUID owner;


    private final Set<UUID> admins;
    private final Set<UUID> mods;
    private final Set<UUID> members;
    private final long createdAt;

    public Team(String id, String name, String tag, UUID owner,
                Set<UUID> admins, Set<UUID> mods, Set<UUID> members, long createdAt) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.owner = owner;
        this.admins = admins;
        this.mods = mods;
        this.members = members;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTag() { return tag; }
    public UUID getOwner() { return owner; }

    public Set<UUID> getAdmins() { return admins; }
    public Set<UUID> getMods() { return mods; }
    public Set<UUID> getMembers() { return members; }
    public long getCreatedAt() { return createdAt; }
}

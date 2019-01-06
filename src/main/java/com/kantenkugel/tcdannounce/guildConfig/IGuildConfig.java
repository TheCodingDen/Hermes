package com.kantenkugel.tcdannounce.guildConfig;

import gnu.trove.set.TLongSet;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public interface IGuildConfig {
    long getGuildId();

    boolean isAnnouncer(Member member);

    boolean isAnnouncerRole(Role role);

    default Set<Role> getAnnouncerRoles(Guild guild) {
        return Arrays.stream(getAnnouncerRoleIds().toArray()).mapToObj(guild::getRoleById).collect(Collectors.toSet());
    }

    TLongSet getAnnouncerRoleIds();

    boolean isAnnouncementRole(Role role);

    default Set<Role> getAnnouncementRoles(Guild guild) {
        return Arrays.stream(getAnnouncementRoleIds().toArray()).mapToObj(guild::getRoleById).collect(Collectors.toSet());
    }

    TLongSet getAnnouncementRoleIds();

    boolean isSubscriptionsEnabled();

    void addAnnouncerRole(Role r);

    void addAnnouncementRole(Role r);

    void removeAnnouncerRole(Role r);

    void removeAnnouncementRole(Role r);

    void setSubscriptionsEnabled(boolean subscriptionsEnabled);

    void update();

    void copyFromConfig(IGuildConfig other);
}

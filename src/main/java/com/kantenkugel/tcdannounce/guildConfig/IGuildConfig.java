package com.kantenkugel.tcdannounce.guildConfig;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

import java.util.List;

public interface IGuildConfig {
    long getGuildId();

    boolean isAnnouncer(Member member);

    boolean isAnnouncerRole(Role role);

    List<Role> getAnnouncerRoles(Guild guild);

    boolean isAnnouncementRole(Role role);

    List<Role> getAnnouncementRoles(Guild guild);

    boolean isSubscriptionsEnabled();

    void addAnnouncerRole(Role r);

    void addAnnouncementRole(Role r);

    void removeAnnouncerRole(Role r);

    void removeAnnouncementRole(Role r);

    void setSubscriptionsEnabled(boolean subscriptionsEnabled);

    void update();
}

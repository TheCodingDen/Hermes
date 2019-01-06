package com.kantenkugel.tcdannounce.guildConfig;

import gnu.trove.set.TLongSet;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Individual Guild config for a specific Guild. Created and retrieved by some {@link IGuildConfigProvider}.
 */
public interface IGuildConfig {
    /**
     * Retrieves the (final) Guild id of this config.
     *
     * @return  The Guild id for this config
     */
    long getGuildId();

    /**
     * Checks whether or not the given Member is a configured announcer according to this config.
     * This is normally done by checking if the Member has at least one of the configured announcer Roles.
     *
     * Announcers are able to use the announce command.
     *
     * @param   member
     *          The Member to check announcer status for
     * @return  Whether or not the given Member is an announcer
     */
    default boolean isAnnouncer(@NotNull Member member) {
        return member.getRoles().stream().anyMatch(this::isAnnouncerRole);
    }

    /**
     * Checks whether or not the given Role is a configured announcer Role according to this config.
     *
     * Announcers are able to use the announce command.
     *
     * @param   role
     *          The Role to check announcer status for
     * @return  Whether or not the given Role is an announcer Role
     */
    boolean isAnnouncerRole(@NotNull Role role);

    /**
     * Retrieves a Set of all currently configured announcer Roles.
     * Since the IGuildConfig should (and is very likely) only storing ids instead of actual Role references,
     * this method needs a Guild reference as parameter to look up Role instances.
     *
     * @param   guild
     *          The Guild reference used to get actual Role objects from ids
     * @return  All currently configured announcer Roles
     */
    @NotNull
    default Set<Role> getAnnouncerRoles(@NotNull Guild guild) {
        return Arrays.stream(getAnnouncerRoleIds().toArray()).mapToObj(guild::getRoleById).collect(Collectors.toSet());
    }

    /**
     * Retrieves the Set of announcer Role ids.
     *
     * @return  All currently configured announcer Role ids
     */
    @NotNull
    TLongSet getAnnouncerRoleIds();

    /**
     * Checks whether or not the given Role is a configured announcement Role according to this config.
     *
     * Announcement Roles can be joined via sub command (if enabled) and are able to be targeted by the announce command.
     *
     * @param   role
     *          The Role to check announcement status for
     * @return  Whether or not the given Role is an announcement Role
     */
    boolean isAnnouncementRole(@NotNull Role role);

    /**
     * Retrieves a Set of all currently configured announcement Roles.
     * Since the IGuildConfig should (and is very likely) only storing ids instead of actual Role references,
     * this method needs a Guild reference as parameter to look up Role instances.
     *
     * @param   guild
     *          The Guild reference used to get actual Role objects from ids
     * @return  All currently configured announcement Roles
     */
    @NotNull
    default Set<Role> getAnnouncementRoles(@NotNull Guild guild) {
        return Arrays.stream(getAnnouncementRoleIds().toArray()).mapToObj(guild::getRoleById).collect(Collectors.toSet());
    }

    /**
     * Retrieves the Set of announcement Role ids.
     *
     * @return  All currently configured announcement Role ids
     */
    @NotNull
    TLongSet getAnnouncementRoleIds();

    /**
     * Checks whether or not subscriptions are enabled.
     * When subscriptions are enabled, users can join announcement roles via the sub command.
     *
     * @return  Whether or not subscriptions are enabled
     */
    boolean isSubscriptionsEnabled();

    /**
     * Adds a new Role to the announcer Roles.
     * The method should not persist this change automatically. Changes are persisted via {@link #update()}.
     *
     * @param   role
     *          The Role to add to the announcer Roles
     */
    void addAnnouncerRole(@NotNull Role role);

    /**
     * Adds a new Role to the announcement Roles.
     * The method should not persist this change automatically. Changes are persisted via {@link #update()}.
     *
     * @param   role
     *          The Role to add to the announcement Roles
     */
    void addAnnouncementRole(@NotNull Role role);

    /**
     * Removes a Role from the announcer Roles.
     * The method should not persist this change automatically. Changes are persisted via {@link #update()}.
     *
     * @param   role
     *          The Role to remove from the announcer Roles
     */
    void removeAnnouncerRole(@NotNull Role role);

    /**
     * Removes a Role from the announcement Roles.
     * The method should not persist this change automatically. Changes are persisted via {@link #update()}.
     *
     * @param   role
     *          The Role to remove from the announcement Roles
     */
    void removeAnnouncementRole(@NotNull Role role);

    /**
     * Enables or disables subscriptions.
     * The method should not persist this change automatically. Changes are persisted via {@link #update()}.
     *
     * @param   subscriptionsEnabled
     *          Whether or not subscriptions should be enabled
     */
    void setSubscriptionsEnabled(boolean subscriptionsEnabled);

    /**
     * Triggers an update to the underlying persistence layer and therefore saves all previously made, unsaved changes.
     */
    void update();

    /**
     * Copies settings from a given IGuildConfig object to this one.
     * Only used during migration so far.
     *
     * @param   other
     *          The IGuildConfig to copy settings from
     */
    void copyFromConfig(@NotNull IGuildConfig other);
}

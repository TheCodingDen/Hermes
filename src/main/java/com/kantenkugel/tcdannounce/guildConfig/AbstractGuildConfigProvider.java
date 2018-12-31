package com.kantenkugel.tcdannounce.guildConfig;

import com.kantenkugel.common.FixedSizeCache;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class AbstractGuildConfigProvider<T extends IGuildConfig> implements IGuildConfigProvider {
    private static final int DEFAULT_CACHE_SIZE = 10;

    protected final Map<Long, T> configCache;

    public AbstractGuildConfigProvider() {
        this(DEFAULT_CACHE_SIZE);
    }

    public AbstractGuildConfigProvider(int cacheSize) {
        if(cacheSize == 0)
            configCache = null;
        else if(cacheSize > 0)
            configCache = new FixedSizeCache<>(cacheSize);
        else
            configCache = new HashMap<>();
    }

    @Override
    public @NotNull IGuildConfig getConfigForGuild(Guild g) {
        long id = g.getIdLong();
        if(configCache == null) {
            T config = getConfig(id);
            return config == null ? createConfig(id) : config;
        }
        return configCache.computeIfAbsent(id, key -> {
            T config = getConfig(id);
            return config == null ? createConfig(id) : config;
        });
    }

    protected abstract T createConfig(long guildId);
    protected abstract T getConfig(long guildId);

    public static abstract class AbstractGuildConfig implements IGuildConfig {
        protected final long guildId;
        protected TLongSet announcerRoles = new TLongHashSet();
        protected TLongSet announcementRoles = new TLongHashSet();
        protected boolean subscriptionsEnabled = false;

        protected AbstractGuildConfig(long guildId) {
            this.guildId = guildId;
        }

        protected AbstractGuildConfig(long guildId, TLongSet announcerRoles, TLongSet announcementRoles, boolean subscriptionsEnabled) {
            this(guildId);
            this.announcerRoles = announcerRoles;
            this.announcementRoles = announcementRoles;
            this.subscriptionsEnabled = subscriptionsEnabled;
        }

        @Override
        public long getGuildId() {
            return guildId;
        }

        @Override
        public boolean isAnnouncer(Member member) {
            return member.getRoles().stream().anyMatch(this::isAnnouncerRole);
        }

        @Override
        public boolean isAnnouncerRole(Role role) {
            return announcerRoles.contains(role.getIdLong());
        }

        @Override
        public List<Role> getAnnouncerRoles(Guild guild) {
            return getRoleSublist(guild, announcerRoles);
        }

        @Override
        public boolean isAnnouncementRole(Role role) {
            return announcementRoles.contains(role.getIdLong());
        }

        @Override
        public List<Role> getAnnouncementRoles(Guild guild) {
            return getRoleSublist(guild, announcementRoles);
        }

        @Override
        public boolean isSubscriptionsEnabled() {
            return subscriptionsEnabled;
        }

        @Override
        public void addAnnouncerRole(Role r) {
            announcerRoles.add(r.getIdLong());
        }

        @Override
        public void addAnnouncementRole(Role r) {
            announcementRoles.add(r.getIdLong());
        }

        @Override
        public void removeAnnouncerRole(Role r) {
            announcerRoles.remove(r.getIdLong());
        }

        @Override
        public void removeAnnouncementRole(Role r) {
            announcementRoles.remove(r.getIdLong());
        }

        @Override
        public void setSubscriptionsEnabled(boolean subscriptionsEnabled) {
            this.subscriptionsEnabled = subscriptionsEnabled;
        }

        private static List<Role> getRoleSublist(Guild guild, TLongSet ids) {
            List<Role> roles = new ArrayList<>(ids.size());
            ids.forEach(id -> {
                Role roleById = guild.getRoleById(id);
                if(roleById != null)
                    roles.add(roleById);
                return true;
            });
            return roles;
        }
    }

}


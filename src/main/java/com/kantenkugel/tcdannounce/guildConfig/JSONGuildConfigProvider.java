package com.kantenkugel.tcdannounce.guildConfig;

import com.kantenkugel.tcdannounce.Utils;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public class JSONGuildConfigProvider implements IGuildConfigProvider {
    private static final Logger LOG = LoggerFactory.getLogger(JSONGuildConfigProvider.class);
    private static final String DEFAULT_CONFIG_PATH = "guildSettings.json";
    private final Path settingsPath;

    private final TLongObjectMap<JSONGuildConfig> settings = new TLongObjectHashMap<>(10);
    //cached json object for easier re-construction on write
    private JSONObject settingsObj;

    @Override
    public IGuildConfig getConfigForGuild(Guild guild) {
        if(settings.containsKey(guild.getIdLong())) {
            return settings.get(guild.getIdLong());
        } else {
            JSONGuildConfig config = new JSONGuildConfig(guild.getIdLong());
            settings.put(guild.getIdLong(), config);
            update(config);
            return config;
        }
    }

    public JSONGuildConfigProvider(String filePath) {
        settingsPath = Paths.get(filePath);
        if(Files.exists(settingsPath)) {
            try {
                settingsObj = Utils.readJson(settingsPath);
                for(String key : settingsObj.keySet()) {
                    try {
                        long guildId = Long.parseUnsignedLong(key);
                        JSONGuildConfig setting = new JSONGuildConfig(guildId, settingsObj.getJSONObject(key));
                        settings.put(guildId, setting);
                    } catch(NumberFormatException ex) {
                        LOG.error("There was a non-id key ({}) in the guild settings file", key, ex);
                    } catch(JSONException ex) {
                        LOG.error("Error parsing json for guild with id {}", key, ex);
                    }
                }
            } catch(IOException ex) {
                LOG.error("Could not read existing guild settings file", ex);
            }
        } else {
            settingsObj = new JSONObject();
        }
    }

    public JSONGuildConfigProvider() {
        this(DEFAULT_CONFIG_PATH);
    }

    private synchronized void update(JSONGuildConfig config) {
        settingsObj.put(Long.toUnsignedString(config.guildId), config.toJson());
        try {
            Utils.writeJson(settingsPath, settingsObj);
        } catch(IOException ex) {
            LOG.error("Could not update the guild settings file", ex);
        }
    }

    public class JSONGuildConfig implements IGuildConfig {
        private final long guildId;
        private TLongSet announcerRoles = new TLongHashSet();
        private TLongSet announcementRoles = new TLongHashSet();
        private boolean subscriptionsEnabled = false;

        private JSONGuildConfig(long guildId) {
            this.guildId = guildId;
        }

        private JSONGuildConfig(long guildId, JSONObject settings) {
            this(guildId);
            announcerRoles.addAll(StreamSupport.stream(settings.getJSONArray("announcerIds").spliterator(), false).mapToLong(e -> (long) e).toArray());
            announcementRoles.addAll(StreamSupport.stream(settings.getJSONArray("announcementRoles").spliterator(), false).mapToLong(e -> (long) e).toArray());
            subscriptionsEnabled = settings.getBoolean("subscriptionsEnabled");
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

        @Override
        public void update() {
            JSONGuildConfigProvider.this.update(this);
        }

        private JSONObject toJson() {
            return new JSONObject()
                    .put("announcerIds", new JSONArray(announcerRoles.toArray()))
                    .put("announcementRoles", new JSONArray(announcementRoles.toArray()))
                    .put("subscriptionsEnabled", subscriptionsEnabled);
        }

        private List<Role> getRoleSublist(Guild guild, TLongSet ids) {
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

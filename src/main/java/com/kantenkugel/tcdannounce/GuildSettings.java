package com.kantenkugel.tcdannounce;

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

public class GuildSettings {
    private static final Logger LOG = LoggerFactory.getLogger(GuildSetting.class);
    private static final Path SETTINGS_PATH = Paths.get("guildSettings.json");

    private static final TLongObjectMap<GuildSetting> settings = new TLongObjectHashMap<>(5);
    //cached json object for easier re-construction on write
    private static JSONObject settingsObj;

    public static GuildSetting forGuild(Guild guild) {
        init();
        if(settings.containsKey(guild.getIdLong())) {
            return settings.get(guild.getIdLong());
        } else {
            GuildSetting guildSetting = new GuildSetting(guild.getIdLong());
            settings.put(guild.getIdLong(), guildSetting);
            update(guildSetting);
            return guildSetting;
        }
    }

    private static boolean isInitialized = false;
    private static synchronized void init() {
        if(isInitialized)
            return;
        isInitialized = true;

        if(Files.exists(SETTINGS_PATH)) {
            try {
                settingsObj = Utils.readJson(SETTINGS_PATH);
                for(String key : settingsObj.keySet()) {
                    try {
                        long guildId = Long.parseUnsignedLong(key);
                        GuildSetting setting = new GuildSetting(guildId, settingsObj.getJSONObject(key));
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

    private static synchronized void update(GuildSetting setting) {
        settingsObj.put(Long.toUnsignedString(setting.guildId), setting.toJson());
        try {
            Utils.writeJson(SETTINGS_PATH, settingsObj);
        } catch(IOException ex) {
            LOG.error("Could not update the guild settings file", ex);
        }
    }

    public static class GuildSetting {
        private final long guildId;
        private TLongSet announcerRoles = new TLongHashSet();
        private TLongSet announcementRoles = new TLongHashSet();
        private boolean subscriptionsEnabled = false;

        private GuildSetting(long guildId) {
            this.guildId = guildId;
        }

        private GuildSetting(long guildId, JSONObject settings) {
            this(guildId);
            announcerRoles.addAll(StreamSupport.stream(settings.getJSONArray("announcerIds").spliterator(), false).mapToLong(e -> (long) e).toArray());
            announcementRoles.addAll(StreamSupport.stream(settings.getJSONArray("announcementRoles").spliterator(), false).mapToLong(e -> (long) e).toArray());
            subscriptionsEnabled = settings.getBoolean("subscriptionsEnabled");
        }

        public boolean isAnnouncer(Member member) {
            return member.getRoles().stream().anyMatch(this::isAnnouncerRole);
        }

        public boolean isAnnouncerRole(Role role) {
            return announcerRoles.contains(role.getIdLong());
        }

        public List<Role> getAnnouncerRoles(Guild guild) {
            return getRoleSublist(guild, announcerRoles);
        }

        public boolean isAnnouncementRole(Role role) {
            return announcementRoles.contains(role.getIdLong());
        }

        public List<Role> getAnnouncementRoles(Guild guild) {
            return getRoleSublist(guild, announcementRoles);
        }

        public boolean isSubscriptionsEnabled() {
            return subscriptionsEnabled;
        }

        public void addAnnouncerRole(Role r) {
            announcerRoles.add(r.getIdLong());
        }

        public void addAnnouncementRole(Role r) {
            announcementRoles.add(r.getIdLong());
        }

        public void removeAnnouncerRole(Role r) {
            announcerRoles.remove(r.getIdLong());
        }

        public void removeAnnouncementRole(Role r) {
            announcementRoles.remove(r.getIdLong());
        }

        public void setSubscriptionsEnabled(boolean subscriptionsEnabled) {
            this.subscriptionsEnabled = subscriptionsEnabled;
        }

        public void update() {
            GuildSettings.update(this);
        }

        private JSONObject toJson() {
            return new JSONObject()
                    .put("announcerIds", new JSONArray(announcerRoles.toArray()))
                    .put("announcementRoles", new JSONArray(announcementRoles.toArray()))
                    .put("subscriptionsEnabled", subscriptionsEnabled);
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

package com.kantenkugel.hermes.guildConfig;

import com.kantenkugel.hermes.Utils;
import gnu.trove.set.hash.TLongHashSet;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.StreamSupport;

public class JSONGuildConfigProvider extends AbstractGuildConfigProvider<JSONGuildConfigProvider.JSONGuildConfig> {
    private static final Logger LOG = LoggerFactory.getLogger(JSONGuildConfigProvider.class);
    private static final String DEFAULT_CONFIG_PATH = "guildSettings.json";
    private final Path settingsPath;

    //cached json object for easier re-construction on write
    private JSONObject settingsObj;

    public JSONGuildConfigProvider() {
        this(DEFAULT_CONFIG_PATH);
    }

    public JSONGuildConfigProvider(String filePath) {
        super(-1);
        settingsPath = Paths.get(filePath);
        if(Files.exists(settingsPath)) {
            try {
                settingsObj = Utils.readJson(settingsPath);
                for(String key : settingsObj.keySet()) {
                    try {
                        long guildId = Long.parseUnsignedLong(key);
                        JSONGuildConfig setting = new JSONGuildConfig(guildId, settingsObj.getJSONObject(key));
                        configCache.put(guildId, setting);
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

    @Override
    public @NotNull Set<IGuildConfig> getAllConfigurations() {
        return new HashSet<>(configCache.values());
    }

    private synchronized void update(JSONGuildConfig config) {
        settingsObj.put(Long.toUnsignedString(config.guildId), config.toJson());
        try {
            Utils.writeJson(settingsPath, settingsObj);
        } catch(IOException ex) {
            LOG.error("Could not update the guild settings file", ex);
        }
    }

    @Override
    protected JSONGuildConfig createConfig(long guildId) {
        JSONGuildConfig config = new JSONGuildConfig(guildId);
        update(config);
        return config;
    }

    @Override
    protected JSONGuildConfig getConfig(long guildId) {
        return null;
    }

    public class JSONGuildConfig extends AbstractGuildConfigProvider.AbstractGuildConfig {
        private JSONGuildConfig(long guildId) {
            super(guildId);
        }

        private JSONGuildConfig(long guildId, JSONObject settings) {
            super(
                    guildId,
                    new TLongHashSet(StreamSupport.stream(settings.getJSONArray("announcerIds").spliterator(), false).mapToLong(e -> (long) e).toArray()),
                    new TLongHashSet(StreamSupport.stream(settings.getJSONArray("announcementRoles").spliterator(), false).mapToLong(e -> (long) e).toArray()),
                    settings.getBoolean("subscriptionsEnabled")
            );
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
    }

}

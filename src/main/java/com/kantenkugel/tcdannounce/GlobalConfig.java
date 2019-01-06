package com.kantenkugel.tcdannounce;

import com.kantenkugel.tcdannounce.guildConfig.IGuildConfigProvider;
import com.kantenkugel.tcdannounce.guildConfig.JSONGuildConfigProvider;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class GlobalConfig {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalConfig.class);

    //variables are initialized during static class init
    public static final String TOKEN;

    public static final Class<? extends IGuildConfigProvider> GUILD_CONFIG_PROVIDER;
    public static final String GUILD_CONFIG_PROVIDER_ARGS;

    //finals
    private static final int CONFIG_VERSION = 3;
    static final Path CONFIG_PATH = Paths.get("config.json");

    //static init code
    static {
        String tmpToken = null;
        Class<? extends IGuildConfigProvider> tmpConfigProvider = JSONGuildConfigProvider.class;
        String tmpGuildConfigArgs = null;
        if(!Files.exists(CONFIG_PATH)) {
            try {
                Utils.writeJson(CONFIG_PATH, getDefaultConfig());
                LOG.info("Created new config file. Please populate it and restart the bot");
            } catch(IOException ex) {
                LOG.error("Failed creating a new config file", ex);
            }
            System.exit(0);
        } else {
            JSONObject obj = null;
            try {
                obj = Utils.readJson(CONFIG_PATH);
            } catch(IOException ex) {
                LOG.error("Error reading/parsing config file", ex);
            }
            if(obj != null) {
                int currentVersion = obj.optInt("version", 1);
                if(currentVersion != CONFIG_VERSION) {
                    boolean restartRequired = migrateConfig(obj, currentVersion);
                    try {
                        Utils.writeJson(CONFIG_PATH, obj);

                        if(restartRequired) {
                            LOG.info("Config file was migrated to a newer version. There were critical changes that require population + restart of the bot");
                            System.exit(0);
                        } else {
                            LOG.info("Config file was migrated to a newer version. You may want to look look at it at a later time.");
                        }
                    } catch(IOException ex) {
                        LOG.error("Could not migrate config file to newer version", ex);
                        System.exit(1);
                    }
                }
                if(!obj.has("token"))
                    throw new JSONException("The json file was missing a required key. Delete the config file to recreate a new one");
                tmpToken = obj.getString("token");
                if(!obj.has("guildConfigProvider")) {
                    LOG.warn("No guildConfigProvider settings found. Defaulting to JSON");
                } else {
                    JSONObject gconf = obj.getJSONObject("guildConfigProvider");
                    if(!gconf.has("class")) {
                        LOG.warn("guildConfigProvider.class not found in config... Defaulting to JSON provider");
                    } else {
                        try {
                            Class<?> provider = Class.forName(gconf.getString("class"));
                            if(IGuildConfigProvider.class.isAssignableFrom(provider))
                                tmpConfigProvider = ((Class<? extends IGuildConfigProvider>) provider);
                            else
                                LOG.warn("guildConfigProvider.class not properly subclassing interface... Defaulting to JSON provider");
                        } catch(ClassNotFoundException ex) {
                            LOG.warn("guildConfigProvider.class was not found in classpath... Defaulting to JSON provider");
                        }
                    }
                    if(gconf.has("args"))
                        tmpGuildConfigArgs = gconf.getString("args");
                }
            } else {
                System.exit(1);
            }
        }
        TOKEN = tmpToken;
        GUILD_CONFIG_PROVIDER = tmpConfigProvider;
        GUILD_CONFIG_PROVIDER_ARGS = tmpGuildConfigArgs;
    }

    /**
     * Migrates from an older config version to the new(est) one
     *
     * @param config
     *          The current configuration
     * @param currVersion
     *          The version of the current configuration
     * @return  Boolean indicating if this migration requires population + program restart
     */
    private static boolean migrateConfig(JSONObject config, int currVersion) {
        if(currVersion > CONFIG_VERSION) {
            JSONObject defaultConfig = getDefaultConfig();
            //remove non-used keys
            Iterator<String> oldKeys = config.keys();
            while(oldKeys.hasNext()) {
                if(!defaultConfig.has(oldKeys.next()))
                    oldKeys.remove();
            }

            //add new keys
            for(String key : defaultConfig.keySet()) {
                if(!config.has(key))
                    config.put(key, defaultConfig.get(key));
            }

            //override version
            config.put("version", CONFIG_VERSION);

            LOG.error("The currently used config file had an invalid version and was restored to current version");
            return true;
        }
        boolean requiresRestart = false;
        switch(currVersion) {
            case 1:
                config.remove("allowedIds");
            case 2:
                config.put("guildConfigProvider", getDefaultConfig().getJSONObject("guildConfigProvider"));
            default:
                config.put("version", CONFIG_VERSION);
        }
        return requiresRestart;
    }

    private static JSONObject getDefaultConfig() {
        return new JSONObject()
                .put("token", "")
                .put("version", CONFIG_VERSION)
                .put("guildConfigProvider", new JSONObject()
                        .put("class", JSONGuildConfigProvider.class.getName())
                        .put("args", "guildSettings.json")
                );
    }

}

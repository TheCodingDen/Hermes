package com.kantenkugel.hermes;

import com.kantenkugel.hermes.guildConfig.IGuildConfig;
import com.kantenkugel.hermes.guildConfig.IGuildConfigProvider;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Hermes {
    public static final Logger LOG = LoggerFactory.getLogger(Hermes.class);

    public static void main(String[] args) {
        if(handleArgs(args))
            return;

        IGuildConfigProvider guildConfigProvider = IGuildConfigProvider.getFromClass(GlobalConfig.GUILD_CONFIG_PROVIDER,
                GlobalConfig.GUILD_CONFIG_PROVIDER_ARGS);

        try {
            MessageAction.setDefaultMentions(EnumSet.noneOf(Message.MentionType.class));
            JDABuilder.create(GlobalConfig.TOKEN, EnumSet.of(GatewayIntent.GUILD_MESSAGES))
                    .setBulkDeleteSplittingEnabled(false)
                    .setChunkingFilter(ChunkingFilter.NONE)
                    .setMemberCachePolicy(MemberCachePolicy.NONE)
                    .disableCache(EnumSet.allOf(CacheFlag.class))
                    .enableCache(EnumSet.of(CacheFlag.MEMBER_OVERRIDES))
                    .addEventListeners(new Listener(guildConfigProvider))
                    .build();
        } catch(LoginException e) {
            LOG.error("Error building jda instance", e);
        }
    }

    private static boolean handleArgs(String[] args) {
        if(args.length > 0 && args[0].equalsIgnoreCase("migratedb")) {
            if(args.length < 2) {
                LOG.error("DB Migration syntax: <fully.qualified.IGuildProvider.class.reference> [<arguments for class init>]");
                return true;
            }
            try {
                Class<?> rawClass = Class.forName(args[1]);
                if(!IGuildConfigProvider.class.isAssignableFrom(rawClass)) {
                    LOG.error("Provided class reference is not a subtype of IGuildConfigProvider");
                    return true;
                }
                @SuppressWarnings("unchecked") //cuz its actually checked by if above
                Class<? extends IGuildConfigProvider> newProviderClass = (Class<? extends IGuildConfigProvider>) rawClass;

                String providerArgs = args.length < 3 ? "" : Arrays.stream(args).skip(2).collect(Collectors.joining(" "));
                IGuildConfigProvider newProvider = IGuildConfigProvider.getFromClass(newProviderClass, providerArgs);

                IGuildConfigProvider currentProvider = IGuildConfigProvider.getFromClass(GlobalConfig.GUILD_CONFIG_PROVIDER,
                        GlobalConfig.GUILD_CONFIG_PROVIDER_ARGS);

                Set<IGuildConfig> allConfigurations = currentProvider.getAllConfigurations();
                for(IGuildConfig config : allConfigurations) {
                    IGuildConfig newConfig = newProvider.getConfigForGuild(config.getGuildId());
                    newConfig.copyFromConfig(config);
                    newConfig.update();
                }
                LOG.info("Successfully migrated {} configurations. Storing new config entries...", allConfigurations.size());
                try {
                    JSONObject jsonObject = Utils.readJson(GlobalConfig.CONFIG_PATH);
                    jsonObject.put("guildConfigProvider", new JSONObject()
                            .put("class", newProviderClass.getName())
                            .put("args", providerArgs)
                    );
                    Utils.writeJson(GlobalConfig.CONFIG_PATH, jsonObject);
                    LOG.info("Done. Please verify the new config file and restart the Bot");
                } catch(IOException e) {
                    LOG.warn("Error updating the config file. Please update the config provider in there yourself and restart the Bot", e);
                }
            } catch(ClassNotFoundException e) {
                LOG.error("Could not find/assign new IGuildConfigProvider class {}", args[1], e);
                return true;
            }
            return true;
        }
        return false;
    }
}

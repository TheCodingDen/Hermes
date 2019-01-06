package com.kantenkugel.hermes.guildConfig;

import net.dv8tion.jda.core.entities.Guild;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 * Persistence entrypoint used to retrieve {@link IGuildConfig} instances.
 */
public interface IGuildConfigProvider {

    /**
     * This method is by default called by {@link #getConfigForGuild(Guild)}
     * but is also invoked during the migration process.
     * It should create (and store!) a configuration object if not already present and should cache results for best performance.
     *
     * @param   guildId
     *          The guild id for which the configuration should be fetched or created
     * @return  The never-null configuration object for the given guild
     */
    @NotNull
    IGuildConfig getConfigForGuild(long guildId);

    /**
     * Called on every command invocation to provide settings to the individual command.
     * This should create (and store!) a configuration object if not already present and should cache results for best performance.
     *
     * By default, this forwards to {@link #getConfigForGuild(long)} and doesn't need to be overridden.
     *
     * @param   guild
     *          The guild for which the configuration should be fetched or created
     * @return  The never-null configuration object for the given guild
     */
    @NotNull
    default IGuildConfig getConfigForGuild(@NotNull Guild guild) {
        return getConfigForGuild(guild.getIdLong());
    }

    /**
     * Used to retreive all currently existing guild configurations.
     * Only called when migrating from one model to another.
     *
     * @return  Never-null (possibly empty) Set of all currently existing guild configurations
     */
    @NotNull
    Set<IGuildConfig> getAllConfigurations();

    static IGuildConfigProvider getFromClass(Class<? extends IGuildConfigProvider> clazz, String args) {
        Logger logger = LoggerFactory.getLogger(IGuildConfigProvider.class);
        IGuildConfigProvider guildConfigProvider = null;
        if(args != null && !args.isEmpty()) {
            try {
                guildConfigProvider = clazz.getConstructor(String.class).newInstance(args);
            } catch(NoSuchMethodException | InstantiationException | IllegalAccessException ex) {
                logger.warn("Guild config provider defined in config didn't support String arg constructor. Trying no-arg constructor");
            } catch(InvocationTargetException initException) {
                logger.error("GuildConfigProvider failed initialization", initException);
                System.exit(1);
            }
        }
        if(guildConfigProvider == null) {
            try {
                guildConfigProvider = clazz.getConstructor().newInstance();
            } catch(NoSuchMethodException | InstantiationException | IllegalAccessException ex) {
                logger.warn("Guild config provider defined in config didn't support empty constructor. Aborting startup");
                System.exit(1);
            } catch(InvocationTargetException initException) {
                logger.error("GuildConfigProvider failed initialization", initException);
                System.exit(1);
            }
        }
        return guildConfigProvider;
    }
}

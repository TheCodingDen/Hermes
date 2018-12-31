package com.kantenkugel.tcdannounce.guildConfig;

import net.dv8tion.jda.core.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface IGuildConfigProvider {
    /**
     * Called on every command invocation to provide settings to the individual command.
     * This should create (and store!) a configuration object if not already present and should cache results for best performance
     *
     * @param g The guild for which the configuration should be fetched or created
     * @return  The never-null configuration object for the given guild
     */
    @NotNull
    IGuildConfig getConfigForGuild(Guild g);

    /**
     * Used to retreive all currently existing guild configurations.
     * Only called when migrating from one model to another.
     *
     * @return  Never-null (possibly empty) Set of all currently existing guild configurations
     */
    @NotNull
    Set<IGuildConfig> getAllConfigurations();
}

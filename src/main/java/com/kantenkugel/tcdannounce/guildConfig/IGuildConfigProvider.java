package com.kantenkugel.tcdannounce.guildConfig;

import net.dv8tion.jda.core.entities.Guild;

public interface IGuildConfigProvider {
    IGuildConfig getConfigForGuild(Guild g);
}

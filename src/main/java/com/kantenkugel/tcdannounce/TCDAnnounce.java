package com.kantenkugel.tcdannounce;

import com.kantenkugel.tcdannounce.guildConfig.IGuildConfigProvider;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;

public class TCDAnnounce {
    public static final Logger LOG = LoggerFactory.getLogger(TCDAnnounce.class);

    public static void main(String[] args) {
        //construct GuildConfigProvider
        IGuildConfigProvider guildConfigProvider = null;
        if(GlobalConfig.GUILD_CONFIG_PROVIDER_ARGS != null) {
            try {
                guildConfigProvider = GlobalConfig.GUILD_CONFIG_PROVIDER.getConstructor(String.class).newInstance(GlobalConfig.GUILD_CONFIG_PROVIDER_ARGS);
            } catch(Exception ex) {
                LOG.warn("Guild config provider defined in config didn't support String arg constructor. Trying no-arg constructor");
            }
        }
        if(guildConfigProvider == null) {
            try {
                guildConfigProvider = GlobalConfig.GUILD_CONFIG_PROVIDER.getConstructor().newInstance();
            } catch(Exception ex) {
                LOG.warn("Guild config provider defined in config didn't support empty constructor. Aborting startup");
                System.exit(1);
            }
        }

        //launch Bot
        try {
            new JDABuilder(AccountType.BOT)
                    .setToken(GlobalConfig.TOKEN)
                    .setAudioEnabled(false)
                    .setBulkDeleteSplittingEnabled(false)
                    .addEventListener(new Listener(guildConfigProvider))
                    .build();
        } catch(LoginException e) {
            LOG.error("Error building jda instance", e);
        }
    }
}

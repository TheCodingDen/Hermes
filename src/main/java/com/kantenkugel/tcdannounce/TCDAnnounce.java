package com.kantenkugel.tcdannounce;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;

public class TCDAnnounce {
    public static final Logger LOG = LoggerFactory.getLogger(TCDAnnounce.class);

    public static void main(String[] args) {
        try {
            new JDABuilder(AccountType.BOT)
                    .setToken(Configs.TOKEN)
                    .setAudioEnabled(false)
                    .setBulkDeleteSplittingEnabled(false)
                    .addEventListener(new Listener())
                    .buildAsync();
        } catch(LoginException e) {
            LOG.error("Error building jda instance", e);
        }
    }
}

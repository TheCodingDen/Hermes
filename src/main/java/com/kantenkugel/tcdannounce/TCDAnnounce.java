package com.kantenkugel.tcdannounce;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;

import javax.security.auth.login.LoginException;

public class TCDAnnounce {
    public static void main(String[] args) {
        try {
            new JDABuilder(AccountType.BOT)
                    .setToken(Statics.TOKEN)
                    .setAudioEnabled(false)
                    .setBulkDeleteSplittingEnabled(false)
                    .addEventListener(new Listener())
                    .buildAsync();
        } catch(LoginException e) {
            Statics.LOG.error("Error building jda instance", e);
        }
    }
}

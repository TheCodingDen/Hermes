package com.kantenkugel.tcdannounce.command;

import com.kantenkugel.tcdannounce.GuildSettings;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import org.slf4j.LoggerFactory;

public interface ICommand {
    default void handleCommand(GuildMessageReceivedEvent event, GuildSettings.GuildSetting settings, String args) {
        handleCommand(event, settings, args.isEmpty() ? null : args.split("\\s+"));
    }

    default void handleCommand(GuildMessageReceivedEvent event, GuildSettings.GuildSetting settings, String[] args) {
        LoggerFactory.getLogger(ICommand.class).warn("ICommand implementation {} doesn't seem to properly handle command calls", getClass().getName());
    }

    default void handleUpdate(GuildMessageUpdateEvent event) {}

    String[] getNames();
}

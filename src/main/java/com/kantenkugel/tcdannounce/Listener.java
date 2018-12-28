package com.kantenkugel.tcdannounce;

import com.kantenkugel.tcdannounce.command.*;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Listener extends ListenerAdapter {
    private Map<String, ICommand> commandMap = new HashMap<>();
    private Set<ICommand> commands;
    private Pattern selfMentionPattern;

    public Listener() {
        registerCommand(new AnnounceCommand());
        registerCommand(new ConfigCommand());
        registerCommand(new HelpCommand());
        registerCommand(new MentionCommand());
        registerCommand(new SubscriptionCommand());
        this.commands = new HashSet<>(commandMap.values());
    }

    @Override
    public void onReady(ReadyEvent event) {
        String inviteUrl = event.getJDA().asBot().getInviteUrl(Permission.MANAGE_ROLES);
        selfMentionPattern = Pattern.compile("^<@!?"+Pattern.quote(event.getJDA().getSelfUser().getId())+">\\s*");
        TCDAnnounce.LOG.info("Bot is ready. Use following link to invite to servers: {}", inviteUrl);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        //ignore bots, webhooks,...
        if(event.getAuthor().isBot() || event.getAuthor().isFake())
            return;

        String messageContentRaw = event.getMessage().getContentRaw();
        //check and remove prefix (mention)
        Matcher matcher = selfMentionPattern.matcher(messageContentRaw);
        if(!matcher.find())
            return;

        String argString = matcher.replaceFirst("").trim();
        if(argString.isEmpty()) //abort if no command specified
            return;

        String[] args = argString.split("\\s+", 2);

        GuildSettings.GuildSetting guildSetting = GuildSettings.forGuild(event.getGuild());

        ICommand command = commandMap.get(args[0].toLowerCase());
        if(command != null)
            command.handleCommand(event, guildSetting, args.length > 1 ? args[1] : "");
    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        commands.forEach(cmdHandler -> cmdHandler.handleUpdate(event));
    }

    private void registerCommand(ICommand handler) {
        for(String name : handler.getNames()) {
            commandMap.put(name.toLowerCase(), handler);
        }
    }

}

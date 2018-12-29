package com.kantenkugel.tcdannounce.command;

import com.kantenkugel.tcdannounce.guildConfig.IGuildConfig;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.stream.Collectors;

public class HelpCommand implements ICommand {
    private static final String[] NAMES = {"help"};

    private static final String HELP_MESSAGE = "This bot only has 4 commands which have to be prefixed with the bot mention:\n" +
            "`help` - Shows this help message.\n" +
            "`sub` - Un-/Subscribe an announcement role. Has to be enabled via configuration.\n" +
            "`announce` - Creates an announcement. Only available to configured roles (%s).\n" +
            "`mention` - Mentions a configured role. Only available to configured roles (see above).\n" +
            "`config` - Configures roles and subscription. Only available to admins.";

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, IGuildConfig guildConfig, String[] args) {
        String help = String.format(HELP_MESSAGE, guildConfig.getAnnouncerRoles(event.getGuild()).stream()
                .map(r -> r.getName().replace("@everyone", "@ everyone"))
                .collect(Collectors.joining(", "))
        );
        if(event.getChannel().canTalk()) {
            event.getChannel().sendMessage(help).queue();
        } else {
            event.getAuthor().openPrivateChannel().queue(pc -> pc.sendMessage(help).queue());
        }
    }

    @Override
    public String[] getNames() {
        return NAMES;
    }
}

package com.kantenkugel.hermes.command;

import com.kantenkugel.hermes.Utils;
import com.kantenkugel.hermes.guildConfig.IGuildConfig;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;
import java.util.stream.Collectors;

public class MentionCommand implements ICommand {
    private static final String[] NAMES = {"mention", "ping"};

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, IGuildConfig guildConfig, String args) {
        //check if member is allowed to announce based on roles
        if(!guildConfig.isAnnouncer(event.getMember()))
            return;

        //can bot talk in current channel? (important for feedback)
        if(!event.getChannel().canTalk()) {
            Utils.reactError(event, "Can not send messages in channel " + event.getChannel().getName());
            return;
        }

        //check if bot can manage roles
        if(!event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            event.getChannel().sendMessage("Missing MANAGE_ROLES permission!").queue();
            return;
        }

        //get and check role to mention
        List<Role> roles = Utils.getRolesByName(event.getGuild(), args).stream()
                .filter(guildConfig::isAnnouncementRole)
                .collect(Collectors.toList());
        if(roles.size() == 0) {
            event.getChannel().sendMessage("No (announcement) roles matching "+args.trim()+" found!").queue();
        } else if(roles.size() > 1) {
            event.getChannel().sendMessage("Too many announcement roles with this name!").queue();
        } else if(!event.getGuild().getSelfMember().canInteract(roles.get(0))) {
            event.getChannel().sendMessage("Can't interact with this role!").queue();
        } else {
            Role role = roles.get(0);
            event.getChannel().getHistoryBefore(event.getMessageIdLong(), 1).queue(history -> {
                String mentionText;
                if(history.getRetrievedHistory().isEmpty() || !history.getRetrievedHistory().get(0).getAuthor().equals(event.getAuthor()))
                    mentionText = String.format("%s (Mention by %#s)", role.getAsMention(), event.getAuthor());
                else
                    mentionText = "^ " + role.getAsMention();
                //announce
                if(role.isMentionable()) {
                    event.getChannel().sendMessage(mentionText).queue(msg -> event.getMessage().delete().queue(null, err -> {}));
                } else {
                    role.getManager().setMentionable(true)
                            .queue(v ->
                                    event.getChannel().sendMessage(mentionText).queue(
                                            msg -> {
                                                role.getManager().setMentionable(false).queue();
                                                event.getMessage().delete().queue(null, err -> {});
                                            },
                                            err -> role.getManager().setMentionable(false).queue()
                                    )
                            );
                }
            });
        }
    }

    @Override
    public String[] getNames() {
        return NAMES;
    }
}

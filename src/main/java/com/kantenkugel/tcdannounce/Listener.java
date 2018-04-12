package com.kantenkugel.tcdannounce;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;

public class Listener extends ListenerAdapter {
    @Override
    public void onReady(ReadyEvent event) {
        String inviteUrl = event.getJDA().asBot().getInviteUrl(Permission.MANAGE_ROLES);
        Statics.LOG.info("Bot is ready. Use following link to invite to servers: {}", inviteUrl);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        Message message = event.getMessage();
        if(event.getAuthor().isBot() || event.getAuthor().isFake() || !message.getContentRaw().startsWith("%announce"))
            return;
        if(event.getMember().getRoles().stream().noneMatch(r -> Statics.ALLOWED_ROLE_IDS.contains(r.getIdLong())))
            return;

        String[] splits = message.getContentRaw().substring(9).trim().split("\\s*\\|\\s*", 3);
        if(splits.length < 2) {
            event.getChannel().sendMessage("Syntax: `%announce role_name [ | channel_mention] | text`").queue();
            return;
        }

        TextChannel channel;
        String textToSend;
        if(splits.length == 3) {
            if(message.getMentionedChannels().size() == 0) {
                event.getChannel().sendMessage("Channel mention missing!").queue();
                return;
            }
            channel = message.getMentionedChannels().get(0);
            textToSend = splits[2].trim();
        } else {
            channel = event.getChannel();
            textToSend = splits[1].trim();
        }

        List<Role> roles = event.getGuild().getRolesByName(splits[0].trim(), true);
        if(roles.size() == 0) {
            event.getChannel().sendMessage("No roles matching "+splits[0].trim()+" found!").queue();
        } else if(roles.size() > 1) {
            event.getChannel().sendMessage("Too many roles with this name!").queue();
        } else if(!event.getGuild().getSelfMember().canInteract(roles.get(0))) {
            event.getChannel().sendMessage("Can't interact with this role!").queue();
        } else {
            Role role = roles.get(0);
            role.getManager().setMentionable(true)
                    .queue(v -> channel.sendMessage(String.format("%s %s (announcement by %s)", role.getAsMention(), textToSend, event.getAuthor()))
                            .queue(msg -> role.getManager().setMentionable(false).queue())
                    );
        }
    }
}

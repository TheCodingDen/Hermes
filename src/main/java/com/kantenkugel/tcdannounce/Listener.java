package com.kantenkugel.tcdannounce;

import com.kantenkugel.common.FixedSizeCache;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;
import java.util.Map;

public class Listener extends ListenerAdapter {
    private static final Map<Long, Message> CACHE = new FixedSizeCache<>(5);

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

        if(!event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            event.getChannel().sendMessage("Missing MANAGE_ROLES permission!").queue();
            return;
        }

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

        if(!channel.canTalk()) {
            event.getChannel().sendMessage("Can not talk in target channel").queue();
            return;
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
                    .queue(v -> {
                        channel.sendMessage(getContent(role, textToSend, event.getAuthor()))
                                .queue(msg -> {
                                    CACHE.put(event.getMessageIdLong(), msg);
                                    role.getManager().setMentionable(false).queue();
                                    if(event.getChannel() != channel)
                                        event.getChannel().sendMessage("Successfully announced").queue();
                                }, err -> {
                                    role.getManager().setMentionable(false).queue();
                                });
                    });
        }
    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        Message botMsg = CACHE.get(event.getMessageIdLong());
        if(botMsg == null)
            return;
        String[] splits = event.getMessage().getContentRaw().split("\\s*\\|\\s*", 3);
        botMsg.editMessage(getContent(botMsg.getMentionedRoles().get(0), splits[splits.length-1].trim(), event.getAuthor())).queue();
    }

    private static String getContent(Role role, String text, User author) {
        return String.format("%s %s\n(announcement by %s)", role.getAsMention(), text, author);
    }
}

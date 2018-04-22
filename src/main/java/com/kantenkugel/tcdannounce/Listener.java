package com.kantenkugel.tcdannounce;

import com.kantenkugel.common.FixedSizeCache;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Listener extends ListenerAdapter {
    private static final String HELP_MESSAGE = "This bot is very simple.\nConfigured roles (%s) can make announcements " +
            "with the `announce` command, and everyone can join/leave announcement roles with the `sub`/`subscribe` command.";

    private static final Map<Long, Message> CACHE = new FixedSizeCache<>(5);
    private Pattern selfMentionPattern;

    @Override
    public void onReady(ReadyEvent event) {
        String inviteUrl = event.getJDA().asBot().getInviteUrl(Permission.MANAGE_ROLES);
        selfMentionPattern = Pattern.compile("^<@!?"+Pattern.quote(event.getJDA().getSelfUser().getId())+">\\s*");
        Statics.LOG.info("Bot is ready. Use following link to invite to servers: {}", inviteUrl);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        String messageContentRaw = event.getMessage().getContentRaw();
        //ignore bots, webhooks,...
        if(event.getAuthor().isBot() || event.getAuthor().isFake())
            return;
        //check and remove prefix (mention)
        Matcher matcher = selfMentionPattern.matcher(messageContentRaw);
        if(!matcher.find())
            return;
        String[] commandSplit = matcher.replaceFirst("").split("\\s+", 2);

        //handle each command accordingly
        switch(commandSplit[0]) {
            case "help":
                handleHelp(event);
                break;
            case "announce":
                handleAnnounce(event, commandSplit);
                break;
            case "sub":
            case "subscribe":
                handleSubscription(event, commandSplit);
                break;
        }
    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        //handle updates of announcement commands
        Message botMsg = CACHE.get(event.getMessageIdLong());
        if(botMsg == null)
            return;
        String[] splits = event.getMessage().getContentRaw().split("\\s*\\|\\s*", 3);
        botMsg.editMessage(getContent(botMsg.getMentionedRoles().get(0), splits[splits.length-1].trim(), event.getAuthor())).queue();
    }

    private static void handleHelp(GuildMessageReceivedEvent event) {
        String help = String.format(HELP_MESSAGE, event.getGuild().getRoles().stream()
                .filter(r -> Statics.ALLOWED_ROLE_IDS.contains(r.getIdLong()))
                .map(r -> r.getName().replace("@everyone", "@ everyone"))
                .collect(Collectors.joining(", "))
        );
        if(event.getChannel().canTalk()) {
            event.getChannel().sendMessage(help).queue();
        } else {
            event.getAuthor().openPrivateChannel().queue(pc -> pc.sendMessage(help).queue());
        }
    }

    private static void handleAnnounce(GuildMessageReceivedEvent event, String[] commandSplit) {
        //check if member is allowed to announce based on roles
        if(event.getMember().getRoles().stream().noneMatch(r -> Statics.ALLOWED_ROLE_IDS.contains(r.getIdLong())))
            return;

        //check if bot can manage roles
        if(!event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            event.getChannel().sendMessage("Missing MANAGE_ROLES permission!").queue();
            return;
        }

        //get and check argument list
        String[] splits = commandSplit.length == 1 ? null : commandSplit[1].split("\\s*\\|\\s*", 3);
        if(splits == null || splits.length < 2) {
            event.getChannel().sendMessage("Syntax: `announce role_name [ | channel_mention] | text`").queue();
            return;
        }

        //get text to send and channel to send in
        TextChannel channel;
        String textToSend;
        if(splits.length == 3) {
            if(event.getMessage().getMentionedChannels().size() == 0) {
                event.getChannel().sendMessage("Channel mention missing!").queue();
                return;
            }
            channel = event.getMessage().getMentionedChannels().get(0);
            textToSend = splits[2].trim();
        } else {
            channel = event.getChannel();
            textToSend = splits[1].trim();
        }

        //can bot talk in channel?
        if(!channel.canTalk()) {
            event.getChannel().sendMessage("Can not talk in target channel").queue();
            return;
        }

        //get and check role to mention
        List<Role> roles = event.getGuild().getRolesByName(splits[0], true);
        if(roles.size() == 0) {
            event.getChannel().sendMessage("No roles matching "+splits[0].trim()+" found!").queue();
        } else if(roles.size() > 1) {
            event.getChannel().sendMessage("Too many roles with this name!").queue();
        } else if(!event.getGuild().getSelfMember().canInteract(roles.get(0))) {
            event.getChannel().sendMessage("Can't interact with this role!").queue();
        } else {
            Role role = roles.get(0);
            //announce
            role.getManager().setMentionable(true)
                    .queue(v -> {
                        channel.sendMessage(getContent(role, textToSend, event.getAuthor()))
                                .queue(msg -> {
                                    //cache sent message for future edits
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

    private static void handleSubscription(GuildMessageReceivedEvent event, String[] commandSplit) {
        if(commandSplit.length == 1) {
            event.getChannel().sendMessage("Syntax: `sub[scribe] role_name`").queue();
            return;
        }

        //check if bot can manage roles
        if(!event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            event.getChannel().sendMessage("Missing MANAGE_ROLES permission!").queue();
            return;
        }

        String[] args = commandSplit[1].split("\\s+", 2);
        if(args.length == 2) {
            event.getChannel().sendMessage("Too many arguments!").queue();
            return;
        }

        List<Role> rolesByName = event.getGuild().getRolesByName(args[0], true);
        if(rolesByName.size() == 0) {
            event.getChannel().sendMessage("No roles matching "+args[0]+" found!").queue();
        } else if(rolesByName.size() > 1) {
            event.getChannel().sendMessage("Too many roles with this name!").queue();
        } else if(!event.getGuild().getSelfMember().canInteract(rolesByName.get(0))) {
            event.getChannel().sendMessage("Role is not available to subscription!").queue();
        } else {
            Role role = rolesByName.get(0);
            Member member = event.getMember();
            boolean canReact = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);
            if(member.getRoles().contains(role)) {
                event.getGuild().getController().removeSingleRoleFromMember(member, role).reason("Subscription").queue(v -> {
                    if(canReact)
                        event.getMessage().addReaction("\u2705").queue();
                    else
                        event.getChannel().sendMessage("Done").queue();
                });
            } else {
                event.getGuild().getController().addSingleRoleToMember(member, role).reason("Subscription").queue(v -> {
                    if(canReact)
                        event.getMessage().addReaction("\u2705").queue();
                    else
                        event.getChannel().sendMessage("Done").queue();
                });
            }
        }
    }

    private static String getContent(Role role, String text, User author) {
        return String.format("%s %s\n(announcement by %s)", role.getAsMention(), text, author);
    }
}

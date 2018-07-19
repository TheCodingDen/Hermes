package com.kantenkugel.tcdannounce;

import com.kantenkugel.common.FixedSizeCache;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import static com.kantenkugel.tcdannounce.Utils.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Listener extends ListenerAdapter {
    private static final String HELP_MESSAGE = "This bot only has 4 commands which have to be prefixed with the bot mention:\n" +
            "`help` - Shows this help message.\n" +
            "`sub` - Un-/Subscribe an announcment role. Has to be enabled via configuration.\n" +
            "`announce` - Creates an announcement. Only available to configured roles.\n" +
            "`config` - Configures roles and subscription. Only available to admins.";

    private static final Map<Long, Message> CACHE = new FixedSizeCache<>(5);
    private Pattern selfMentionPattern;

    @Override
    public void onReady(ReadyEvent event) {
        String inviteUrl = event.getJDA().asBot().getInviteUrl(Permission.MANAGE_ROLES);
        selfMentionPattern = Pattern.compile("^<@!?"+Pattern.quote(event.getJDA().getSelfUser().getId())+">\\s*");
        TCDAnnounce.LOG.info("Bot is ready. Use following link to invite to servers: {}", inviteUrl);
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
        GuildSettings.GuildSetting guildSetting = GuildSettings.forGuild(event.getGuild());

        //handle each command accordingly
        switch(commandSplit[0]) {
            case "help":
                handleHelp(event, guildSetting);
                break;
            case "announce":
                handleAnnounce(event, guildSetting, commandSplit);
                break;
            case "sub":
            case "subscribe":
                handleSubscription(event, guildSetting, commandSplit);
                break;
            case "config":
            case "configure":
                handleConfiguration(event, guildSetting, commandSplit);
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
        botMsg.editMessage(getAnnouncementMessage(botMsg.getMentionedRoles().get(0), splits[splits.length-1].trim(), event.getMember())).queue();
    }

    private static void handleHelp(GuildMessageReceivedEvent event, GuildSettings.GuildSetting guildSetting) {
        String help = String.format(HELP_MESSAGE, event.getGuild().getRoles().stream()
                .filter(guildSetting::isAnnouncerRole)
                .map(r -> r.getName().replace("@everyone", "@ everyone"))
                .collect(Collectors.joining(", "))
        );
        if(event.getChannel().canTalk()) {
            event.getChannel().sendMessage(help).queue();
        } else {
            event.getAuthor().openPrivateChannel().queue(pc -> pc.sendMessage(help).queue());
        }
    }

    private static void handleAnnounce(GuildMessageReceivedEvent event, GuildSettings.GuildSetting guildSetting, String[] commandSplit) {
        //check if member is allowed to announce based on roles
        if(!guildSetting.isAnnouncer(event.getMember()))
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
        List<Role> roles = getRolesByName(event.getGuild(), splits[0]).stream()
                .filter(guildSetting::isAnnouncementRole)
                .collect(Collectors.toList());
        if(roles.size() == 0) {
            event.getChannel().sendMessage("No (announcement) roles matching "+splits[0].trim()+" found!").queue();
        } else if(roles.size() > 1) {
            event.getChannel().sendMessage("Too many announcement roles with this name!").queue();
        } else if(!event.getGuild().getSelfMember().canInteract(roles.get(0))) {
            event.getChannel().sendMessage("Can't interact with this role!").queue();
        } else {
            Role role = roles.get(0);
            //announce
            if(role.isMentionable()) {
                channel.sendMessage(getAnnouncementMessage(role, textToSend, event.getMember()))
                        .queue(msg -> {
                            //cache sent message for future edits
                            CACHE.put(event.getMessageIdLong(), msg);
                            if(event.getChannel() != channel)
                                event.getChannel().sendMessage("Successfully announced").queue();
                        });
            } else {
                role.getManager().setMentionable(true)
                        .queue(v -> {
                            channel.sendMessage(getAnnouncementMessage(role, textToSend, event.getMember()))
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
    }

    private static void handleSubscription(GuildMessageReceivedEvent event, GuildSettings.GuildSetting guildSetting, String[] commandSplit) {
        //abort if subscriptions are not enabled
        if(!guildSetting.isSubscriptionsEnabled()) {
            event.getChannel().sendMessage("Subscriptions are not enabled for this server").queue();
            return;
        }

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
            event.getChannel().sendMessage("Too many arguments! (replace spaces in role names with underscores)").queue();
            return;
        }

        List<Role> rolesByName = getRolesByName(event.getGuild(), args[0]).stream()
                .filter(guildSetting::isAnnouncementRole)
                .collect(Collectors.toList());
        if(rolesByName.size() == 0) {
            event.getChannel().sendMessage("No (announcement) roles matching "+args[0]+" found!").queue();
        } else if(rolesByName.size() > 1) {
            event.getChannel().sendMessage("Too many announcement roles with this name!").queue();
        } else if(rolesByName.get(0).isManaged()) {
            event.getChannel().sendMessage("Can not subscribe to a managed role!").queue();
        } else if(!event.getGuild().getSelfMember().canInteract(rolesByName.get(0))) {
            event.getChannel().sendMessage("Can't interact with this role!").queue();
        } else {
            Role role = rolesByName.get(0);
            Member member = event.getMember();
            if(member.getRoles().contains(role)) {
                event.getGuild().getController().removeSingleRoleFromMember(member, role).reason("Subscription").queue(v -> {
                    reactSuccess(event, "Unsubscribed from " + role.getName());
                });
            } else {
                event.getGuild().getController().addSingleRoleToMember(member, role).reason("Subscription").queue(v -> {
                    reactSuccess(event, "Subscribed to " + role.getName());
                });
            }
        }
    }

    private static void handleConfiguration(GuildMessageReceivedEvent event, GuildSettings.GuildSetting guildSetting, String[] commandSplit) {
        //admin only (Kantenkugel is hardcoded as bot admin)
        if(!event.getMember().hasPermission(Permission.ADMINISTRATOR) && event.getAuthor().getIdLong() != 122758889815932930L)
            return;

        TextChannel channel = event.getChannel();

        if(commandSplit.length == 1) {
            channel.sendMessageFormat(
                    "**Current configuration:**\n" +
                            "Roles with announce permission (change with `config(ure) announcers add/remove role_name`):\n%s\n\n" +
                            "Announcement roles (change with `config(ure) roles add/remove role_name`):\n%s\n\n" +
                            "Subscriptions enabled? (allows `sub(scribe)` command, change with `config(ure) enablesub(scription)s true/false`)\n - %s",
                    getRoleList(guildSetting.getAnnouncerRoles(event.getGuild())),
                    getRoleList(guildSetting.getAnnouncementRoles(event.getGuild())),
                    guildSetting.isSubscriptionsEnabled()
            ).queue();
            return;
        }

        String[] args = commandSplit[1].toLowerCase().split("\\s+", 4);

        List<Role> rolesByName;
        switch(args[0]) {
            case "announcers":
                if(args.length != 3) {
                    channel.sendMessage("Invalid number of arguments").queue();
                    return;
                }
                rolesByName = getRolesByName(event.getGuild(), args[2]);
                if(rolesByName.size() != 1) {
                    channel.sendMessage("None or too many Roles matching given name").queue();
                } else {
                    if(args[1].equals("add")) {
                        guildSetting.addAnnouncerRole(rolesByName.get(0));
                        guildSetting.update();
                        reactSuccess(event);
                    } else if(args[1].equals("remove")) {
                        guildSetting.removeAnnouncerRole(rolesByName.get(0));
                        guildSetting.update();
                        reactSuccess(event);
                    } else {
                        channel.sendMessage("unknown sub-option " + args[1]).queue();
                    }
                }
                break;

            case "roles":
                if(args.length != 3) {
                    channel.sendMessage("Invalid number of arguments").queue();
                    return;
                }
                rolesByName = getRolesByName(event.getGuild(), args[2]);
                if(rolesByName.size() != 1) {
                    channel.sendMessage("None or too many Roles matching given name").queue();
                } else if(!event.getGuild().getSelfMember().canInteract(rolesByName.get(0))) {
                    channel.sendMessage("I can not interact with that role!").queue();
                } else {
                    if(args[1].equals("add")) {
                        guildSetting.addAnnouncementRole(rolesByName.get(0));
                        guildSetting.update();
                        reactSuccess(event);
                    } else if(args[1].equals("remove")) {
                        guildSetting.removeAnnouncementRole(rolesByName.get(0));
                        guildSetting.update();
                        reactSuccess(event);
                    } else {
                        channel.sendMessage("unknown sub-option " + args[1]).queue();
                    }
                }
                break;

            case "enablesub":
            case "enablesubs":
            case "enablesubscription":
            case "enablesubscriptions":
                if(args.length != 2) {
                    channel.sendMessage("Invalid number of arguments").queue();
                    return;
                }
                if(args[1].equals("true")) {
                    guildSetting.setSubscriptionsEnabled(true);
                    guildSetting.update();
                    reactSuccess(event);
                } else if(args[1].equals("false")) {
                    guildSetting.setSubscriptionsEnabled(false);
                    guildSetting.update();
                    reactSuccess(event);
                } else {
                    channel.sendMessage("unknown sub-option " + args[1]).queue();
                }
                break;

            default:
                channel.sendMessage("Unknown option " + args[0]).queue();
        }
    }
}

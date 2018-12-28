package com.kantenkugel.tcdannounce.command;

import com.kantenkugel.tcdannounce.GuildSettings;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;

import static com.kantenkugel.tcdannounce.Utils.getRoleList;
import static com.kantenkugel.tcdannounce.Utils.getRolesByName;
import static com.kantenkugel.tcdannounce.Utils.reactSuccess;

public class ConfigCommand implements ICommand {
    private static final String[] NAMES = {"config", "configure"};

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, GuildSettings.GuildSetting settings, String[] args) {
        //admin only (Kantenkugel is hardcoded as bot admin)
        if(!event.getMember().hasPermission(Permission.ADMINISTRATOR) && event.getAuthor().getIdLong() != 122758889815932930L)
            return;

        TextChannel channel = event.getChannel();

        if(args == null) {
            channel.sendMessageFormat(
                    "**Current configuration:**\n" +
                            "Roles with announce permission (change with `config(ure) announcers add/remove role_name`):\n%s\n\n" +
                            "Announcement roles (change with `config(ure) roles add/remove role_name`):\n%s\n\n" +
                            "Subscriptions enabled? (allows `sub(scribe)` command, change with `config(ure) enablesub(scription)s true/false`)\n - %s",
                    getRoleList(settings.getAnnouncerRoles(event.getGuild())),
                    getRoleList(settings.getAnnouncementRoles(event.getGuild())),
                    settings.isSubscriptionsEnabled()
            ).queue();
            return;
        }

//        String[] args = commandSplit[1].toLowerCase().split("\\s+", 4);

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
                        settings.addAnnouncerRole(rolesByName.get(0));
                        settings.update();
                        reactSuccess(event);
                    } else if(args[1].equals("remove")) {
                        settings.removeAnnouncerRole(rolesByName.get(0));
                        settings.update();
                        reactSuccess(event);
                    } else {
                        channel.sendMessage("unknown sub-option " + args[1]).queue();
                    }
                }
                break;

            case "roles":
            case "role":
                if(args.length != 3) {
                    channel.sendMessage("Invalid number of arguments").queue();
                    return;
                }
                rolesByName = getRolesByName(event.getGuild(), args[2]);
                if(rolesByName.size() != 1) {
                    channel.sendMessage("None or too many Roles matching given name").queue();
                } else if(rolesByName.get(0).isManaged() || !event.getGuild().getSelfMember().canInteract(rolesByName.get(0))) {
                    channel.sendMessage("I can not interact with that role!").queue();
                } else {
                    if(args[1].equals("add")) {
                        settings.addAnnouncementRole(rolesByName.get(0));
                        settings.update();
                        reactSuccess(event);
                    } else if(args[1].equals("remove")) {
                        settings.removeAnnouncementRole(rolesByName.get(0));
                        settings.update();
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
                    settings.setSubscriptionsEnabled(true);
                    settings.update();
                    reactSuccess(event);
                } else if(args[1].equals("false")) {
                    settings.setSubscriptionsEnabled(false);
                    settings.update();
                    reactSuccess(event);
                } else {
                    channel.sendMessage("unknown sub-option " + args[1]).queue();
                }
                break;

            default:
                channel.sendMessage("Unknown option " + args[0]).queue();
        }
    }

    @Override
    public String[] getNames() {
        return NAMES;
    }
}

package com.kantenkugel.hermes.command;

import com.kantenkugel.hermes.Utils;
import com.kantenkugel.hermes.guildConfig.IGuildConfig;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SubscriptionCommand implements ICommand {
    private static final String[] NAMES = {"sub", "subscribe", "unsub", "unsubscribe", "toggle"};

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, IGuildConfig guildConfig, String[] args) {
        //abort if subscriptions are not enabled
        if(!guildConfig.isSubscriptionsEnabled()) {
            event.getChannel().sendMessage("Subscriptions are not enabled for this server").queue();
            return;
        }

        if(args == null) {
            event.getChannel().sendMessage("Syntax: `sub[scribe] role_name`").queue();
            return;
        }

        //check if bot can manage roles
        if(!event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            event.getChannel().sendMessage("Missing MANAGE_ROLES permission!").queue();
            return;
        }

        List<Role> rolesToToggle = new ArrayList<>(args.length);
        List<String> unavailableRoles = new ArrayList<>(args.length);
        for(int i=0; i<args.length; i++) {
            Role r = Utils.getValidRoleByName(event.getGuild(), guildConfig, args[i]);
            if(r == null)
                unavailableRoles.add(args[i]);
            else
                rolesToToggle.add(r);
        }

        if(!unavailableRoles.isEmpty()) {
            event.getChannel().sendMessage(String.format("Following role(s) were not found or are not available for subscription:" +
                            "\n%s\nIf the role you want to subscribe to has spaces in its name, please replace them with underscores.",
                    String.join(", ", unavailableRoles))).queue();
        } else {
            Member member = event.getMember();
            if(rolesToToggle.size() == 1) {
                Role role = rolesToToggle.get(0);
                if(member.getRoles().contains(role)) {
                    event.getGuild().getController().removeSingleRoleFromMember(member, role).reason("Subscription").queue(v -> {
                        Utils.reactSuccess(event, "Unsubscribed from " + role.getName());
                    });
                }
                else {
                    event.getGuild().getController().addSingleRoleToMember(member, role).reason("Subscription").queue(v -> {
                        Utils.reactSuccess(event, "Subscribed to " + role.getName());
                    });
                }
            } else {
                Map<Boolean, List<Role>> roleTypes = rolesToToggle.stream().collect(Collectors.groupingBy(role -> member.getRoles().contains(role)));
                event.getGuild().getController().modifyMemberRoles(member,
                        roleTypes.getOrDefault(false, Collections.emptyList()),
                        roleTypes.getOrDefault(true, Collections.emptyList())
                ).queue(v -> {
                    event.getChannel().sendMessage("Toggled subscriptions of " +
                            rolesToToggle.stream().map(Role::getName).collect(Collectors.joining(", "))
                    ).queue();
                });
            }
        }
    }

    @Override
    public String[] getNames() {
        return NAMES;
    }
}

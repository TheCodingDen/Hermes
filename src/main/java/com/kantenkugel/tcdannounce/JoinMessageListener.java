package com.kantenkugel.tcdannounce;

import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JoinMessageListener implements EventListener {
    private static final long GUILD_ID = 172018499005317120L; //TCD
//    private static final long GUILD_ID = 268716314279804929L; //Testing
    private static final long ADMIN_ROLE_ID = 257497572183113728L;  //Staff
//    private static final long ADMIN_ROLE_ID = 433933820127870976L;  //Testing

    private Pattern selfMentionPattern;
    private String joinMessage = null;

    @Override
    public void onEvent(Event event) {
        if(event instanceof ReadyEvent) {
            selfMentionPattern = Pattern.compile("^<@!?"+Pattern.quote(event.getJDA().getSelfUser().getId())+">\\s*");
        } else if(event instanceof GuildMemberJoinEvent) {
            //User joined, send message if configured

            GuildMemberJoinEvent e = (GuildMemberJoinEvent) event;
            if(e.getGuild().getIdLong() != GUILD_ID)
                return;

            if(joinMessage != null) {
                e.getUser().openPrivateChannel().queue(pc -> pc.sendMessage(joinMessage).queue(null, err -> {}));
            }

        } else if(event instanceof GuildMessageReceivedEvent) {
            //message received (config command)

            GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;
            if(e.getGuild().getIdLong() != GUILD_ID)
                return;

            //ignore bots, webhooks,...
            if(e.getAuthor().isBot() || e.getAuthor().isFake())
                return;
            //only allow staff
            if(e.getMember().getRoles().stream().noneMatch(r -> r.getIdLong() == ADMIN_ROLE_ID))
                return;

            //check and remove prefix (mention)
            String messageContentRaw = e.getMessage().getContentRaw();
            Matcher matcher = selfMentionPattern.matcher(messageContentRaw);
            if(!matcher.find())
                return;
            String[] commandSplit = matcher.replaceFirst("").split("\\s+", 2);
            if(commandSplit[0].equalsIgnoreCase("joinmessage")) {
                if(commandSplit.length == 2 && !commandSplit[1].trim().isEmpty()) {
                    joinMessage = commandSplit[1].trim();
                    e.getChannel().sendMessage("Set join message").queue();
                } else {
                    joinMessage = null;
                    e.getChannel().sendMessage("Removed join message").queue();
                }
            }
        }
    }
}

package com.kantenkugel.tcdannounce;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    public static JSONObject readJson(Path path) throws IOException {
        try(BufferedReader reader = Files.newBufferedReader(path)) {
            return new JSONObject(new JSONTokener(reader));
        }
    }

    public static void writeJson(Path path, JSONObject json) throws IOException {
        Files.write(path, json.toString(4).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void reactSuccess(GuildMessageReceivedEvent event) {
        reactSuccess(event, "Done");
    }

    public static void reactSuccess(GuildMessageReceivedEvent event, String messageContent) {
        boolean canReact = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);
        if(canReact)
            event.getMessage().addReaction("\u2705").queue();
        else
            event.getChannel().sendMessage(messageContent).queue();
    }

    public static String getRoleList(List<Role> roles) {
        if(roles.isEmpty())
            return " - *None*";
        return roles.stream().map(r -> " - " + r.getName().replace("@everyone", "@ everyone")).collect(Collectors.joining("\n"));
    }

    public static Message getAnnouncementMessage(Role role, String text, Member author) {
        String avatarUrl = author.getUser().getEffectiveAvatarUrl();
        if(avatarUrl.endsWith(".gif"))      //force "static" png avatar in footer instead of possibly animated one
            avatarUrl = avatarUrl.substring(0, avatarUrl.length() - 3) + "png";
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Announcement")
                .setDescription(text)
                .setColor(0xFFFFD0)
                .setFooter(author.getEffectiveName(), avatarUrl)
                .setTimestamp(Instant.now());
        return new MessageBuilder(role.getAsMention()).setEmbed(embedBuilder.build()).build();
    }

    public static Role getValidRoleByName(Guild guild, String name) {
        GuildSettings.GuildSetting guildSetting = GuildSettings.forGuild(guild);
        List<Role> rolesByName = getRolesByName(guild, name).stream()
                .filter(guildSetting::isAnnouncementRole)
                .collect(Collectors.toList());
        if(rolesByName.isEmpty() || rolesByName.get(0).isManaged() || !guild.getSelfMember().canInteract(rolesByName.get(0)))
            return null;
        return rolesByName.get(0);
    }

    public static List<Role> getRolesByName(Guild guild, String name) {
        List<Role> rolesByName = guild.getRolesByName(name, true);
        if(rolesByName.isEmpty() && name.contains("_"))
            return guild.getRolesByName(name.replace('_', ' '), true);
        return rolesByName;
    }
}

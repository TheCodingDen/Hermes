package com.kantenkugel.tcdannounce;

import com.kantenkugel.tcdannounce.guildConfig.IGuildConfig;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    /**
     * Tries to read json (object) from the given file path.
     *
     * @param   path
     *          The file path to read the json from
     * @return  The read + parsed JSONObject
     *
     * @throws  IOException
     *          If there was some error reading or parsing the file
     */
    @NotNull
    public static JSONObject readJson(@NotNull Path path) throws IOException {
        try(BufferedReader reader = Files.newBufferedReader(path)) {
            return new JSONObject(new JSONTokener(reader));
        }
    }

    /**
     * Tries to write a json object to the given file path.
     *
     * @param   path
     *          The file path to write to
     * @param   json
     *          The json to write to the file
     *
     * @throws  IOException
     *          If there was some error writing the json data
     */
    public static void writeJson(@NotNull Path path, @NotNull JSONObject json) throws IOException {
        Files.write(path, json.toString(4).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Tries to react in a positive way to the given event.<br>
     * This is a shorthand for {@link #reactSuccess(GuildMessageReceivedEvent, String) reactSuccess(event, "Done")}.<br>
     * Please take a look at {@link #react(GuildMessageReceivedEvent, String, String)} for an explanation on the full behavior.
     *
     * @param   event
     *          The event to react positive to
     *
     * @see #reactSuccess(GuildMessageReceivedEvent, String)
     * @see #react(GuildMessageReceivedEvent, String, String)
     */
    public static void reactSuccess(@NotNull GuildMessageReceivedEvent event) {
        reactSuccess(event, "Done");
    }

    /**
     * Tries to react in a positive way to the given event.<br>
     * This is a shorthand for {@link #react(GuildMessageReceivedEvent, String, String) react(event, "GREEN_CHECKMARK_EMOJI", messageContent}.<br>
     * Please take a look at {@link #react(GuildMessageReceivedEvent, String, String)} for an explanation on the full behavior.
     *
     * @param   event
     *          The event to react positive to
     * @param   messageContent
     *          The message content to use in case reactions are not possible.
     *
     * @see #react(GuildMessageReceivedEvent, String, String)
     */
    public static void reactSuccess(@NotNull GuildMessageReceivedEvent event, @NotNull String messageContent) {
        react(event, "\u2705", messageContent);
    }

    /**
     * Tries to react in a negative way to the given event.<br>
     * This is a shorthand for {@link #reactError(GuildMessageReceivedEvent, String) reactError(event, "Something went wrong")}.<br>
     * Please take a look at {@link #react(GuildMessageReceivedEvent, String, String)} for an explanation on the full behavior.
     *
     * @param   event
     *          The event to react negative to
     *
     * @see #reactError(GuildMessageReceivedEvent, String)
     * @see #react(GuildMessageReceivedEvent, String, String)
     */
    public static void reactError(@NotNull GuildMessageReceivedEvent event) {
        reactError(event, "Something went wrong");
    }

    /**
     * Tries to react in a negative way to the given event.<br>
     * This is a shorthand for {@link #react(GuildMessageReceivedEvent, String, String) react(event, "RED_CROSS_EMOJI", messageContent}.<br>
     * Please take a look at {@link #react(GuildMessageReceivedEvent, String, String)} for an explanation on the full behavior.
     *
     * @param   event
     *          The event to react negative to
     * @param   messageContent
     *          The message content to use in case reactions are not possible.
     *
     * @see #react(GuildMessageReceivedEvent, String, String)
     */
    public static void reactError(@NotNull GuildMessageReceivedEvent event, @NotNull String messageContent) {
        react(event, "\u274C", messageContent);
    }

    /**
     * Tries to react to the given event.
     * <p>
     * This will try following steps in order:
     * <ul>
     *     <li>React with the given reaction if reactions are allowed</li>
     *     <li>Send a message into the same channel if possible, using the given {@code messageContent}</li>
     *     <li>Send the message author a DM, failing silently</li>
     * </ul>
     * <p>
     * Note: this uses {@link #sendMessageExhaustive(GuildMessageReceivedEvent, String)} to actually send messages.
     *
     * @param   event
     *          The event to react to
     * @param   reaction
     *          The reaction used to react to the event
     * @param   messageContent
     *          The message content used in messages if reactions are not possible
     */
    public static void react(@NotNull GuildMessageReceivedEvent event, @NotNull String reaction, @NotNull String messageContent) {
        boolean canReact = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);
        if(canReact)                            //try reacting first
            event.getMessage().addReaction(reaction).queue();
        else                                    //try send message in channel/dm
            sendMessageExhaustive(event, messageContent);
    }

    /**
     * Tries to send a message to the same channel the given event originates from,
     * falling back to DMing the message author if the bot can't send messages in the TextChannel.
     *
     * @param   event
     *          The event used to look up the channel / user to send the message to
     * @param   message
     *          The message to send
     */
    public static void sendMessageExhaustive(@NotNull GuildMessageReceivedEvent event, @NotNull String message) {
        if(event.getChannel().canTalk())        //then try message
            event.getChannel().sendMessage(message).queue();
        else                                    //lastly, send dm and ignore error
            event.getAuthor().openPrivateChannel().queue(chan -> chan.sendMessage(message + " (could not react/message in server)").queue(), err -> {});
    }

    /**
     * Constructs a String listing all given roles. Roles will be ordered based on their lexicographical order.<br>
     * This has 3 types of output:
     * <ul>
     *     <li>{@code " - *None*"} if the given Set is empty</li>
     *     <li>Multiple lines of {@code " - RoleName"} if the Set contains at most 5 elements</li>
     *     <li>{@code " - RoleName, RoleName, ..."} if the Set contains more than 5 elements</li>
     * </ul>
     *
     * @param   roles
     *          The roles to construct a String listing from
     * @return  A String containing all given roles in lexicographical order
     */
    @NotNull
    public static String getRoleList(@NotNull Set<Role> roles) {
        if(roles.isEmpty())
            return " - *None*";
        Stream<String> roleNameStream = roles.stream().map(r -> r.getName().replace("@everyone", "@ everyone")).sorted();
        if(roles.size() > 5)
            return " - " + roleNameStream.collect(Collectors.joining(", "));
        else
            return roleNameStream.map(name -> " - " + name).collect(Collectors.joining("\n"));
    }

    /**
     * Creates the announcement message used in the announce command:
     * <ul>
     *     <li>Mentions the given role in the message body</li>
     *     <li>Embed title: {@code Announcement}</li>
     *     <li>Sets the embed description to the given text</li>
     *     <li>Sets embed color to a nice yellow-ish</li>
     *     <li>Adds the authors name + avatar image (disables gif avatars) to the footer</li>
     *     <li>Adds timestamp to footer</li>
     * </ul>
     *
     * @param   role
     *          The role to mention
     * @param   text
     *          The announcement text
     * @param   author
     *          The author of the announcement
     * @return  Message object ready to be sent to discord
     */
    @NotNull
    public static Message getAnnouncementMessage(@NotNull Role role, @NotNull String text, @NotNull Member author) {
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

    /**
     * Tries to get a single announcement Role given Guild, the Role name and the GuildConfig.<br>
     * First, the name is tried as-is and in a second attempt, all underscores are replaced by spaces.<br>
     * This will return {@code null} if no announcement Role with given name were found, the role is managed
     * or the bot can't interact with the role.
     *
     * @param   guild
     *          The Guild where the Role should be searched in
     * @param   guildConfig
     *          The GuildConfig used to check announcement role status
     * @param   name
     *          The name of the Role to search for
     * @return  Possibly-null role matching mentioned criteria
     *
     * @see #getRolesByName(Guild, String) if announcement/managed/interact check are not needed
     */
    @Nullable
    public static Role getValidRoleByName(@NotNull Guild guild, @NotNull IGuildConfig guildConfig, @NotNull String name) {
        List<Role> rolesByName = getRolesByName(guild, name).stream()
                .filter(guildConfig::isAnnouncementRole)
                .collect(Collectors.toList());
        if(rolesByName.isEmpty() || rolesByName.get(0).isManaged() || !guild.getSelfMember().canInteract(rolesByName.get(0)))
            return null;
        return rolesByName.get(0);
    }

    /**
     * Retrieves all roles of the Guild matching the given name.<br>
     * First, the name is tried as-is and in a second attempt, all underscores are replaced by spaces.
     *
     * @param   guild
     *          The Guild where the Roles should be searched for in
     * @param   name
     *          The name of the Roles to search
     * @return  All found roles with given name
     */
    @NotNull
    public static List<Role> getRolesByName(@NotNull Guild guild, @NotNull String name) {
        name = name.trim();
        List<Role> rolesByName = guild.getRolesByName(name, true);
        if(rolesByName.isEmpty() && name.contains("_"))
            return guild.getRolesByName(name.replace('_', ' '), true);
        return rolesByName;
    }
}

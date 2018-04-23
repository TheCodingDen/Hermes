package com.kantenkugel.tcdannounce;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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
        boolean canReact = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);
        if(canReact)
            event.getMessage().addReaction("\u2705").queue();
        else
            event.getChannel().sendMessage("Done").queue();
    }
}

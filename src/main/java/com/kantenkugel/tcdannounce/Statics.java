package com.kantenkugel.tcdannounce;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.StreamSupport;

public class Statics {

    public static final Logger LOG = LoggerFactory.getLogger(TCDAnnounce.class);

    //variables are initialized during static class init
    public static final String TOKEN;
    public static final TLongSet ALLOWED_ROLE_IDS;

    private static final Path configFile = Paths.get("config.json");

    //static init code
    static {
        String tmpToken = null;
        TLongSet tmpRoleIds = null;
        if(!Files.exists(configFile)) {
            JSONObject obj = new JSONObject()
                    .put("token", "")
                    .put("allowedIds", new JSONArray());
            try {
                Files.write(configFile, obj.toString(4).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
                LOG.info("Created new config file. Please populate it and restart the bot");
            } catch(IOException e) {
                LOG.error("Failed creating a new config file", e);
            }
            System.exit(0);
        } else {
            try(BufferedReader reader = Files.newBufferedReader(configFile)) {
                JSONObject obj = new JSONObject(new JSONTokener(reader));
                if(!obj.has("token") || !obj.has("allowedIds"))
                    throw new JSONException("The json file was missing a required key. Delete the config file to recreate a new one");
                tmpToken = obj.getString("token");
                tmpRoleIds = StreamSupport.stream(obj.getJSONArray("allowedIds").spliterator(), false)
                        .mapToLong(elem -> (elem instanceof String) ? Long.parseUnsignedLong((String) elem) : (long) elem)
                        .collect(TLongHashSet::new, TLongHashSet::add, TLongHashSet::addAll);
            } catch(Exception ex) {
                LOG.error("Error reading/parsing config file", ex);
                System.exit(1);
            }
        }
        TOKEN = tmpToken;
        ALLOWED_ROLE_IDS = tmpRoleIds;
    }
}

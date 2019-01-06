package com.kantenkugel.hermes.guildConfig;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class PGGuildConfigProvider extends AbstractGuildConfigProvider<PGGuildConfigProvider.PGConfigObject> {
    private static final Logger LOG = LoggerFactory.getLogger(PGGuildConfigProvider.class);
    private static final String DEFAULT_CONNECT_PATH = "jdbc:postgresql://localhost/hermes?user=hermes&password=hermespw";

    private final Connection connection;
    private final PreparedStatement fetchStatement;
    private final PreparedStatement insertStatement;
    private final PreparedStatement updateStatement;

    public PGGuildConfigProvider() {
        this(DEFAULT_CONNECT_PATH);
    }

    public PGGuildConfigProvider(String connectString) {
        try {
            connection = DriverManager.getConnection(connectString);
            Statement statement = connection.createStatement();
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS guildConfigs" +
                    "(" +
                    " guildId bigint NOT NULL," +
                    " announcerRoleIds bigint[] NOT NULL DEFAULT array[]::bigint[]," +
                    " announcementRoleIds bigint[] NOT NULL DEFAULT array[]::bigint[]," +
                    " subsEnabled boolean NOT NULL DEFAULT false," +
                    " CONSTRAINT guildConfigs_pk PRIMARY KEY (guildid)" +
                    ");"
            );
            statement.close();
            fetchStatement = connection.prepareStatement("SELECT * FROM guildConfigs WHERE guildId = ?;");
            insertStatement = connection.prepareStatement("INSERT INTO guildConfigs VALUES (?);");
            updateStatement = connection.prepareStatement("UPDATE guildConfigs SET announcerRoleIds = ?, announcementRoleIds = ?, subsEnabled = ? WHERE guildId = ?");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    fetchStatement.close();
                    insertStatement.close();
                    updateStatement.close();
                    connection.close();
                } catch(SQLException ignored) {}
            }));
        } catch(SQLException e) {
            throw new RuntimeException("Could not connect to pg database", e);
        }
    }

    @Override
    public @NotNull Set<IGuildConfig> getAllConfigurations() {
        Set<IGuildConfig> configs = new HashSet<>();
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM guildConfigs")) {
            while(rs.next()) {
                //todo
            }
        } catch(SQLException e) {
            LOG.error("Error creating/Executing query", e);
        }
        return configs;
    }

    @Override
    protected PGConfigObject createConfig(long guildId) {
        try {
            insertStatement.setLong(1, guildId);
            insertStatement.executeUpdate();
            return new PGConfigObject(guildId);
        } catch(SQLException e) {
            LOG.error("Could not create new config entry for guild id {}", guildId, e);
        }
        return null;
    }

    @Override
    protected PGConfigObject getConfig(long guildId) {
        try {
            fetchStatement.setLong(1, guildId);
            try(ResultSet resultSet = fetchStatement.executeQuery()) {
                if(resultSet.next()) {
                    return new PGConfigObject(guildId, resultSet);
                }
                return null;
            }
        } catch(SQLException e) {
            LOG.error("Error fetching config for guild with id {}", guildId, e);
        }
        return null;
    }

    private void update(PGConfigObject confObject) {
        try {
            updateStatement.setObject(1, confObject.getAnnouncerIds());
            updateStatement.setObject(2, confObject.getAnnouncementIds());
            updateStatement.setBoolean(3, confObject.isSubscriptionsEnabled());
            updateStatement.setLong(4, confObject.getGuildId());
            updateStatement.executeUpdate();
        } catch(SQLException e) {
            LOG.error("Error updating config object with new values (guild id {})", confObject.getGuildId(), e);
        }
    }

    public class PGConfigObject extends AbstractGuildConfigProvider.AbstractGuildConfig {

        private PGConfigObject(long guildId) {
            super(guildId);
        }


        private PGConfigObject(long guildId, ResultSet rs) {
            super(guildId);
            try {
                Array announcerRoleIds = rs.getArray("announcerRoleIds");
                Array announcementRoleIds = rs.getArray("announcementRoleIds");
                boolean subsEnabled = rs.getBoolean("subsEnabled");

                for(long announcerId : (Long[]) announcerRoleIds.getArray())
                    this.announcerRoles.add(announcerId);
                for(long announcementId: (Long[]) announcementRoleIds.getArray())
                    this.announcementRoles.add(announcementId);
                this.subscriptionsEnabled = subsEnabled;
            } catch(SQLException e) {
                LOG.error("Error populating config object from ResultSet (guild id: {})", guildId, e);
            }
        }

        private long[] getAnnouncerIds() {
            return announcerRoles.toArray();
        }

        private long[] getAnnouncementIds() {
            return announcementRoles.toArray();
        }

        @Override
        public void update() {
            PGGuildConfigProvider.this.update(this);
        }
    }
}

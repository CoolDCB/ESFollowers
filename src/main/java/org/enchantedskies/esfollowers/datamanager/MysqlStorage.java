package org.enchantedskies.esfollowers.datamanager;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.enchantedskies.esfollowers.ESFollowers;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MysqlStorage implements Storage {
    private final ESFollowers plugin = ESFollowers.getInstance();
    private final MysqlDataSource dataSource;

    public MysqlStorage() {
        initDb();
        dataSource = initMySQLDataSource();
        ConfigurationSection databaseSection = plugin.getConfig().getConfigurationSection("Database");
        dataSource.setPassword(databaseSection.getString("password", ""));
        dataSource.setPortNumber(databaseSection.getInt("port"));
        dataSource.setDatabaseName(databaseSection.getString("name"));
        dataSource.setUser(databaseSection.getString("user"));
    }

    public Connection conn() {
        try {
            return dataSource.getConnection();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    @Override
    public FollowerUser loadFollowerUser(UUID uuid) {
        try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement(
            "SELECT * FROM follower_users WHERE uuid = ?;"
        )) {
            stmt.setString(1, uuid.toString());
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                return new FollowerUser(
                    UUID.fromString(resultSet.getString("uuid")),
                    resultSet.getString("name"),
                    resultSet.getString("follower"),
                    resultSet.getString("followerDisplayName"),
                    resultSet.getBoolean("followerNameEnabled"),
                    resultSet.getBoolean("followerEnabled")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void saveFollowerUser(FollowerUser followerUser) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement(
                "REPLACE INTO follower_users(uuid, name, follower, followerDisplayName, followerNameEnabled, followerEnabled) VALUES(?, ?, ?, ?, ?, ?);"
            )) {
                stmt.setString(1, followerUser.getUUID().toString());
                stmt.setString(2, followerUser.getUsername());
                stmt.setString(3, followerUser.getFollower());
                stmt.setString(4, followerUser.getDisplayName());
                stmt.setBoolean(5, followerUser.isDisplayNameEnabled());
                stmt.setBoolean(6, followerUser.isFollowerEnabled());
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void initDb() {
        String setup;
        try (InputStream in = DataManager.class.getClassLoader().getResourceAsStream("dbsetup.sql")) {
            setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("n"));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not read db setup file.", e);
            e.printStackTrace();
            return;
        }
        String[] queries = setup.split(";");
        for (String query : queries) {
            if (query.isEmpty()) continue;
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        plugin.getLogger().info("§2Database setup complete.");
    }

    private MysqlDataSource initMySQLDataSource() {
        MysqlDataSource dataSource = new MysqlConnectionPoolDataSource();
        try {
            testDataSource(dataSource);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return dataSource;
    }

    private void testDataSource(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }
        }
    }
}

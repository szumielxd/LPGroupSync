package me.szumielxd.lpgroupsync.hikari;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.AbstractMap.SimpleEntry;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import me.szumielxd.lpgroupsync.LPGroupSync;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

public abstract class HikariDB {
	
	
	protected final LPGroupSync plugin;
	protected final DatabaseConfig dbconfig;
	protected HikariDataSource hikari;
	
	private final String DB_HOST;
	private final String DB_NAME;
	private final String DB_USER;
	private final String DB_PASSWD;

	
	private static @NotNull String escapeSql(@NotNull String text) {
		return text.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"");
	}
	
	
	public HikariDB(LPGroupSync plugin, DatabaseConfig dbconfig) {
		this.plugin = plugin;
		this.dbconfig = dbconfig;
		
		DB_HOST = dbconfig.getHost();
		DB_NAME = dbconfig.getDatabase();
		DB_USER = dbconfig.getUser();
		DB_PASSWD = dbconfig.getPassword();
		
	}
	
	
	/**
	 * Get default port for this implementation of HikariCP.
	 * 
	 * @return default port
	 */
	protected abstract int getDefaultPort();
	
	
	/**
	 * Setup database connection properties.
	 */
	public HikariDB setup() {
		HikariConfig config = new HikariConfig();
		config.setPoolName("portfel-hikari");
		final String[] host = DB_HOST.split(":");
		int port = this.getDefaultPort();
		if (host.length > 1) {
			try {
				port = Integer.parseInt(host[1]);
			} catch (NumberFormatException e) {}
		}
		this.setupDatabase(config, host[0], port, DB_NAME, DB_USER, DB_PASSWD);
		
		PoolOptions options = this.dbconfig.getPoolOptions();
		Map<String, String> properties = options.getProperties();
		this.setupProperties(config, properties);
		
		config.setMaximumPoolSize(options.getMaxPoolSize());
		config.setMinimumIdle(options.getMinIdle());
		config.setMaxLifetime(options.getMaxLifetime());
		config.setKeepaliveTime(options.getKeepAlive());
		config.setConnectionTimeout(options.getConnTimeout());
		config.setInitializationFailTimeout(-1);
		
		this.hikari = new HikariDataSource(config);
		return this;
	}
	
	/**
	 * Modify and setup connection properties.
	 * 
	 * @param properties default properties map
	 */
	protected abstract void setupProperties(@NotNull HikariConfig config, @NotNull Map<String, String> properties);
	
	/**
	 * Setup database connection.
	 * 
	 * @param config database configuration object
	 * @param address connection's address
	 * @param port connection's port
	 * @param database database name
	 * @param user database user name
	 * @param password database password
	 */
	public abstract void setupDatabase(@NotNull HikariConfig config, @NotNull String address, int port, @NotNull String database, @NotNull String user, @NotNull String password);
	
	/**
	 * Get database connection.
	 * 
	 * @return database connection
	 * @throws SQLException when cannot establish database connection
	 */
	public Connection connect() throws SQLException {
		if (this.hikari == null) throw new SQLException("Unable to get a connection from the pool. (hikari is null)");
		Connection conn = this.hikari.getConnection();
		if (conn == null) throw new SQLException("Unable to get a connection from the pool. (connection is null)");
		return conn;
	}
	
	/**
	 * Check if database is connected.
	 * 
	 * @return true if connection to database is opened
	 */
	public boolean isConnected() {
		return this.isValid() && !this.hikari.isClosed();
	}
	
	/**
	 * Check if database connection is valid.
	 * 
	 * @return true if connection to database is valid
	 */
	public boolean isValid() {
		return this.hikari != null;
	}
	
	/**
	 * Shutdown database
	 */
	public void shutdown() {
		if (this.hikari != null) this.hikari.close();
	}
	
	/**
	 * Get users and his inheritance nodes related to given groups.
	 * 
	 * @implNote Thread unsafe.
	 * @param groups groups to select
	 * @return map of nodes accessed by users's UUID
	 * @throws SQLException when cannot establish the connection to the database
	 */
	public @Nullable Map<UUID, Entry<String, List<InheritanceNode>>> getGroupNodesByUser(@NotNull String... groups) throws SQLException {
		this.checkConnection();
		Map<UUID, Entry<String, List<InheritanceNode>>> groupsByUser = new HashMap<>();
		if (groups.length == 0) return groupsByUser;
		String sql = String.format("SELECT `players`.`uuid`, `players`.`username`, `perms`.`permission`, `perms`.`value`, `perms`.`expiry` FROM `luckperms_user_permissions` as `perms` INNER JOIN `luckperms_players` as `players` ON `perms`.`uuid` = `players`.`uuid` WHERE `server` = 'global' AND `world` = 'global' AND `permission` IN (%s) AND (`expiry` = 0 OR `expiry` > UNIX_TIMESTAMP())", String.join(", ", Stream.of(groups).map(s -> "?").toArray(String[]::new)));
		try (Connection conn = this.connect()) {
			try (PreparedStatement stm = conn.prepareStatement(sql)) {
				for (int i = groups.length; i > 0;) {
					stm.setString(i, InheritanceNode.builder(groups[--i]).build().getKey());
				}
				String str = sql;
				for (String s : groups) str = str.replaceFirst("\\?", "'"+InheritanceNode.builder(s).build().getKey()+"'");
				try (ResultSet rs = stm.executeQuery()) {
					while (rs.next()) {
						final String name = rs.getString(2);
						groupsByUser.computeIfAbsent(UUID.fromString(rs.getString(1)), k -> new SimpleEntry<>(name, new ArrayList<>())).getValue().add(
								(InheritanceNode)Node.builder(rs.getString(3)).value(rs.getBoolean(4)).expiry(rs.getLong(5)).build());
					}
				}
			}
		}
		return groupsByUser;
	}
	
	/**
	 * Get groups and his meta nodes related to given meta types.
	 * 
	 * @implNote Thread unsafe.
	 * @param metaTypes list of metadata types to fetch
	 * @param groups list of valid groups
	 * @return map of modes accessed by group's name
	 * @throws SQLException when cannot establish the connection to the database
	 */
	public @Nullable Map<String, List<Node>> getMetaNodesByGroup(@NotNull String[] metaTypes, @NotNull String... groups) throws SQLException {
		this.checkConnection();
		List<String> special = Arrays.asList("display", "prefix", "suffix", "weight");
		metaTypes = Stream.of(metaTypes).map(str -> special.contains(str) ? str : "meta."+str).map(Pattern::quote).toArray(String[]::new);
		Map<String, List<Node>> metaByGroup = new HashMap<>();
		if (groups.length == 0 || metaTypes.length == 0) return metaByGroup;
		String sql = String.format("SELECT `name`, `permission`, `value`, `expiry` FROM `luckperms_group_permissions` WHERE `name` IN (%s) AND `server` = 'global' AND `world` = 'global' AND `permission` RLIKE ? AND (`expiry` = 0 OR `expiry` > UNIX_TIMESTAMP())", String.join(", ", Stream.of(groups).map(s -> "?").toArray(String[]::new)));
		try (Connection conn = this.connect()) {
			try (PreparedStatement stm = conn.prepareStatement(sql)) {
				for (int i = groups.length; i > 0;) {
					stm.setString(i, groups[--i]);
				}
				stm.setString(groups.length+1, String.format("^(%s)\\..+", String.join("|", metaTypes)));
				try (ResultSet rs = stm.executeQuery()) {
					while (rs.next()) {
						metaByGroup.computeIfAbsent(rs.getString(1), k -> new ArrayList<>()).add(
								Node.builder(rs.getString(2)).value(rs.getBoolean(3)).expiry(rs.getLong(4)).build());
					}
				}
			}
		}
		return metaByGroup;
	}
	
	/**
	 * Check if connection can be obtained, otherwise creates new one.
	 */
	public void checkConnection() {
		if (!this.isConnected()) this.setup();
	}

}

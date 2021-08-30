package me.szumielxd.lpgroupsync;

import java.util.Arrays;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;

import me.szumielxd.lpgroupsync.hikari.DatabaseConfig;

public enum ConfigKey {
	
	//DATABASE_TYPE("database.type", "MySQL"),
	//
	//DATABASE_HOST("database.host", "localhost"),
	//DATABASE_DATABASE("database.database", "portfel"),
	//DATABASE_USERNAME("database.username", "root"),
	//DATABASE_PASSWORD("database.password", ""),
	//
	//DATABASE_POOL_MAXSIZE("database.pool-options.maximum-pool-size", 10),
	//DATABASE_POOL_MINIDLE("database.pool-options.minimum-idle", 10),
	//DATABASE_POOL_MAXLIFETIME("database.pool-options.maximum-lifetime", 1800000),
	//DATABASE_POOL_TIMEOUT("database.pool-options.connection-timeout", 5000),
	//DATABASE_POOL_KEEPALIVE("database.pool-options.keep-alive-time", 0),
	//DATABASE_POOL_PROPERTIES("database.pool-options.properties", ImmutableMap.of("useUnicode", "true", "characterEncoding", "utf8")),
	DATABASES("databases", Arrays.asList(DatabaseConfig.deserialize(Collections.emptyMap()))),
	
	USER_GROUPSYNC_MAPPINGS("user.group-sync.mappings", Arrays.asList("owner", "admin", "staff -> globalstaff")),
	
	GROUP_METASYNC_MAPPINGS("group.meta-sync.mappings", Arrays.asList("owner", "admin", "staff -> globalstaff")),
	GROUP_METASYNC_NAMES("group.meta-sync.names", Arrays.asList("prefix", "suffix", "display", "tabprefix")),
	GROUP_METASYNC_OVERRIDE("group.meta-sync.override", true),
	;

	private final String path;
	private final Object defaultValue;
	private final Class<?> type;
	
	private ConfigKey(@NotNull String path, @NotNull Object defaultValue) {
		this.path = path;
		this.defaultValue = defaultValue;
		this.type = defaultValue.getClass();
	}
	
	public String getPath() {
		return this.path;
	}
	
	public Object getDefault() {
		return this.defaultValue;
	}
	
	public Class<?> getType() {
		return this.type;
	}

}

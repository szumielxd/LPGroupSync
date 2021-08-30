package me.szumielxd.lpgroupsync.hikari;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import lombok.Getter;

@SerializableAs("DatabaseConfiguration - dont remove this line")
public class DatabaseConfig implements Cloneable, ConfigurationSerializable {
	
	
	private final @Getter String type;
	private final @Getter String host;
	private final @Getter String database;
	private final @Getter String user;
	private final @Getter String password;
	private final @Getter PoolOptions poolOptions;
	
	
	public DatabaseConfig(String type, String host, String database, String user, String password, PoolOptions poolOptions) {
		this.type = type;
		this.host = host;
		this.database = database;
		this.user = user;
		this.password = password;
		this.poolOptions = poolOptions;
	}
	

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		map.put("type", this.type);
		map.put("host", this.host);
		map.put("database", this.database);
		map.put("user", this.user);
		map.put("password", this.password);
		map.put("pool-options", this.poolOptions);
		return map;
	}
	
	
	public static DatabaseConfig deserialize(Map<String, Object> map) {
		String type = (String) map.getOrDefault("type", "MySQL");
		String host = (String) map.getOrDefault("host", "localhost");
		String database = (String) map.getOrDefault("database", "luckperms");
		String user = (String) map.getOrDefault("user", "root");
		String password = (String) map.getOrDefault("password", "");
		PoolOptions poolOptions = (PoolOptions) map.getOrDefault("pool-options", PoolOptions.deserialize(new HashMap<>()));
		return new DatabaseConfig(type, host, database, user, password, poolOptions);
	}

}

package me.szumielxd.lpgroupsync.hikari;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import com.google.common.collect.ImmutableMap;

import lombok.Getter;

@SerializableAs("PoolOptions - dont remove this line")
public class PoolOptions implements Cloneable, ConfigurationSerializable {

	
	private final @Getter int maxLifetime;
	private final @Getter int minIdle;
	private final @Getter int connTimeout;
	private final @Getter int maxPoolSize;
	private final @Getter int keepAlive;
	private final @Getter Map<String, String> properties;
	
	
	public PoolOptions(int maxLifetime, int minIdle, int connTimeout, int maxPoolSize, int keepAlive, Map<String, String> properties) {
		this.maxLifetime = maxLifetime;
		this.minIdle = minIdle;
		this.connTimeout = connTimeout;
		this.maxPoolSize = maxPoolSize;
		this.keepAlive = keepAlive;
		this.properties = Collections.unmodifiableMap(properties);
	}
	
	
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		map.put("maximum-lifetime", this.maxLifetime);
		map.put("minimum-idle", this.minIdle);
		map.put("connection-timeout", this.connTimeout);
		map.put("maximum-pool-size", this.maxPoolSize);
		map.put("keep-alive-time", this.keepAlive);
		map.put("properties", this.properties);
		return map;
	}
	
	public static PoolOptions deserialize(Map<String, Object> map) {
		int maxLifetime = (int) map.getOrDefault("maximum-lifetime", 1800000);
		int minIdle = (int) map.getOrDefault("minimum-idle", 10);
		int connTimeout = (int) map.getOrDefault("connection-timeout", 5000);
		int maxPoolSize = (int) map.getOrDefault("maximum-pool-size", 10);
		int keepAlive = (int) map.getOrDefault("keep-alive-time", 0);
		@SuppressWarnings("unchecked")
		Map<String, String> properties = (Map<String, String>) map.getOrDefault("properties", ImmutableMap.of("useUnicode", "true", "characterEncoding", "utf8"));
		return new PoolOptions(maxLifetime, minIdle, connTimeout, maxPoolSize, keepAlive, properties);
	}

}

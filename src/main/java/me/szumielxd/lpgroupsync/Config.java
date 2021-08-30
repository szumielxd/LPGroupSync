package me.szumielxd.lpgroupsync;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class Config {
	
	
	private final LPGroupSync plugin;
	private final YamlConfiguration yaml;
	private final File file;
	
	public Config(@NotNull LPGroupSync plugin) {
		this.plugin = plugin;
		this.yaml = new YamlConfiguration();
		this.file = new File(this.plugin.getDataFolder(), "config.yml");
	}
	
	
	/**
	 * Initialize configuration
	 * 
	 * @param values to load
	 * @return this object
	 */
	public @NotNull Config init(@NotNull ConfigKey... values) {
		this.yaml.addDefaults(Stream.of(values).collect(Collectors.toMap(ConfigKey::getPath, ConfigKey::getDefault)));
		
		if (!this.file.getParentFile().exists()) {
			this.file.getParentFile().mkdirs();
		}
		try {
			if (!this.file.exists()) {
				this.file.createNewFile();
				this.yaml.options().copyDefaults(true);
				this.yaml.save(this.file);
			} else {
				this.yaml.load(this.file);
			}
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	/**
	 * Get value of given key parsed as string
	 * 
	 * @param key the key
	 * @return value of given key
	 */
	public @NotNull String getString(@NotNull ConfigKey key) {
		return this.yaml.isString(key.getPath()) ? this.yaml.getString(key.getPath()) : (String) key.getDefault();
	}
	
	/**
	 * Get value of given key parsed as list of strings
	 * 
	 * @param key the key
	 * @return value of given key
	 */
	@SuppressWarnings("unchecked")
	public @NotNull List<String> getStringList(@NotNull ConfigKey key) {
		return this.yaml.isList(key.getPath()) ? this.yaml.getStringList(key.getPath()) : (List<String>) key.getDefault();
	}
	
	/**
	 * Get value of given key parsed as list of objects
	 * 
	 * @param key the key
	 * @return value of given key
	 */
	public @NotNull List<?> getList(@NotNull ConfigKey key) {
		return this.yaml.isList(key.getPath()) ? this.yaml.getList(key.getPath()) : (List<?>)key.getDefault();
	}
	
	/**
	 * Get value of given key parsed as integer
	 * 
	 * @param key the key
	 * @return value of given key
	 */
	public int getInt(@NotNull ConfigKey key) {
		return this.yaml.isInt(key.getPath()) ? this.yaml.getInt(key.getPath()) : (int) key.getDefault();
	}
	
	/**
	 * Get value of given key parsed as integer
	 * 
	 * @param key the key
	 * @return value of given key
	 */
	public boolean getBoolean(@NotNull ConfigKey key) {
		return this.yaml.isBoolean(key.getPath()) ? this.yaml.getBoolean(key.getPath()) : (boolean) key.getDefault();
	}
	
	/**
	 * Get value of given key parsed as map of strings
	 * 
	 * @param key the key
	 * @return value of given key
	 */
	public Map<String, String> getStringMap(@NotNull ConfigKey key) {
		if (this.yaml.isConfigurationSection(key.getPath())) {
			final ConfigurationSection cfg = this.yaml.getConfigurationSection(key.getPath());
			return cfg.getKeys(false).stream().collect(Collectors.toMap(k -> k, k -> cfg.getString(k)));
		}
		return new HashMap<>();
	}
	

}

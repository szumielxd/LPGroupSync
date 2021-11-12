package me.szumielxd.lpgroupsync;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
import me.szumielxd.lpgroupsync.hikari.DatabaseConfig;
import me.szumielxd.lpgroupsync.hikari.HikariDB;
import me.szumielxd.lpgroupsync.hikari.MariaDB;
import me.szumielxd.lpgroupsync.hikari.MysqlDB;
import me.szumielxd.lpgroupsync.hikari.PoolOptions;

public class LPGroupSync extends JavaPlugin {
	
	
	public static final String PREFIX = "§7[§b§lL§3§lP§a§lSync§7] §3";
	
	
	private Config config;
	private List<HikariDB> databases;
	@Getter private UserGroupUpdater userGroupUpdater;
	@Getter private GroupMetaUpdater groupMetaUpdater;
	
	
	@Override
	public void onEnable() {
		ConfigurationSerialization.registerClass(DatabaseConfig.class);
		ConfigurationSerialization.registerClass(PoolOptions.class);
		this.config = new Config(this).init(ConfigKey.values());
		//
		this.getCommand("lpgroupsync").setExecutor(new MainCommand(this));
		//
		this.getLogger().info("Establishing connection with databases...");
		this.databases = Collections.unmodifiableList(this.getConfiguration().getList(ConfigKey.DATABASES).stream().map(DatabaseConfig.class::cast)
				.map(cfg -> cfg.getType().equalsIgnoreCase("mariadb")? new MariaDB(this, cfg) : new MysqlDB(this, cfg)).map(HikariDB::setup)
				.collect(Collectors.toList()));
		//
		this.userGroupUpdater = new UserGroupUpdater(this);
		this.groupMetaUpdater = new GroupMetaUpdater(this);
		this.getServer().getScheduler().runTaskTimerAsynchronously(this, this.userGroupUpdater::updateUsers, 20L, 5*60*20L);
		this.getServer().getScheduler().runTaskTimerAsynchronously(this, this.groupMetaUpdater::updateGroups, 20L, 5*60*20L);
	}
	
	
	@Override
	public void onDisable() {
		ConfigurationSerialization.unregisterClass(DatabaseConfig.class);
		ConfigurationSerialization.unregisterClass(PoolOptions.class);
		this.getServer().getScheduler().cancelTasks(this);
		this.databases.forEach(HikariDB::shutdown);
	}
	
	
	public Config getConfiguration() {
		return this.config;
	}
	
	
	public List<HikariDB> getDB() {
		return this.databases;
	}
	
	
	
	

}

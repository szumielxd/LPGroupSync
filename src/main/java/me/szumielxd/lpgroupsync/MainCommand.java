package me.szumielxd.lpgroupsync;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public class MainCommand implements TabExecutor {
	
	
	private final LPGroupSync plugin;
	
	
	public MainCommand(LPGroupSync plugin) {
		this.plugin = plugin;
	}
	

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> list = new ArrayList<>();
		if (args.length == 1) {
			String arg = args[0].toLowerCase();
			if ("force".startsWith(arg)) list.add("force");
		}
		return list;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length > 0 && args[0].equalsIgnoreCase("force")) {
			sender.sendMessage(LPGroupSync.PREFIX + "Forcing update of LuckPerms groups data...");
			this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> {
				this.plugin.getUserGroupUpdater().updateUsers();
				this.plugin.getGroupMetaUpdater().updateGroups();
				sender.sendMessage(LPGroupSync.PREFIX + "§aSuccessfully updated groups data.");
			});
			return true;
		}
		sender.sendMessage(LPGroupSync.PREFIX + String.format("Usage: §a/%s force", label));
		return true;
	}

}

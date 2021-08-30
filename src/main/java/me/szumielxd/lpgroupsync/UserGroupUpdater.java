package me.szumielxd.lpgroupsync;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.szumielxd.lpgroupsync.hikari.HikariDB;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.matcher.NodeMatcher;
import net.luckperms.api.node.types.InheritanceNode;

public class UserGroupUpdater {
	
	
	private final LPGroupSync plugin;
	private final Pattern mapperPattern = Pattern.compile("([^ ]+) +-> +([^ ]+)");
	private Map<String, String> mappings = new HashMap<>();
	
	
	public UserGroupUpdater(LPGroupSync plugin) {
		this.plugin = plugin;
		this.plugin.getConfiguration().getStringList(ConfigKey.USER_GROUPSYNC_MAPPINGS).forEach(str -> {
			Matcher match = mapperPattern.matcher(str);
			if (match.matches()) this.mappings.put(match.group(1), match.group(2));
			else this.mappings.put(str, str);
		});
	}
	
	
	/**
	 * Update user's ranks.
	 */
	public void updateUsers() {
		try {
			LuckPerms lp = LuckPermsProvider.get();
			CompletableFuture<Map<UUID, Collection<Node>>> future = lp.getUserManager().searchAll(NodeMatcher.type(NodeType.INHERITANCE));
			HashMap<UUID, List<InheritanceNode>> toRemove = future.get().entrySet().stream().collect(Collectors.toMap(e -> e.getKey(),
					e -> e.getValue().stream().map(InheritanceNode.class::cast).filter(n -> this.mappings.containsValue(n.getGroupName()))
					.collect(Collectors.toCollection(ArrayList::new)), (a,b) -> b, HashMap::new));
			HashMap<UUID, Entry<String, Set<InheritanceNode>>> toAdd = new HashMap<>();
			HashMap<UUID, Entry<String, Set<InheritanceNode>>> map = new HashMap<>();
			if (!this.plugin.getDB().isEmpty()) for (HikariDB db : this.plugin.getDB()) db.getGroupNodesByUser(this.mappings.keySet().toArray(new String[0])).forEach((k, v) -> map.computeIfAbsent(k, s -> new SimpleEntry<>(v.getKey(), new HashSet<>())).getValue().addAll(v.getValue()));
			map.forEach((uuid, nodes) -> {
				List<InheritanceNode> removable = toRemove.get(uuid);
				nodes.getValue().forEach(node -> {
					final InheritanceNode finalNode = node.toBuilder().group(this.mappings.get(node.getGroupName())).build();
					if (removable == null || !removable.remove(finalNode)) {
						toAdd.computeIfAbsent(uuid, k -> new SimpleEntry<>(nodes.getKey(), new HashSet<>())).getValue().add(finalNode);
					}
				});
			});
			toRemove.entrySet().removeIf(e -> e.getValue().isEmpty());
			Map<UUID, String> users = new HashMap<>();
			toRemove.forEach((k,v) -> users.put(k, null));
			toAdd.forEach((k, v) -> users.put(k, v.getKey()));
			if (!users.isEmpty()) for (Entry<UUID, String> entry : new HashMap<>(users).entrySet()) {
				if (entry.getValue() != null) lp.getUserManager().savePlayerData(entry.getKey(), entry.getValue()).get();
				User user = lp.getUserManager().loadUser(entry.getKey()).get();
				if (user == null) {
					toRemove.remove(entry.getKey());
					toAdd.remove(entry.getKey());
					users.remove(entry.getKey());
					continue;
				}
				if (toRemove.containsKey(entry.getKey())) toRemove.get(entry.getKey()).forEach(user.data()::remove);
				if (toAdd.containsKey(entry.getKey())) toAdd.get(entry.getKey()).getValue().forEach(user.data()::add);
				lp.getUserManager().saveUser(user);
			}
			int removed = toRemove.values().stream().mapToInt(List::size).sum();
			int added = toAdd.values().stream().mapToInt(e -> e.getValue().size()).sum();
			if (!users.isEmpty()) this.plugin.getLogger().info(String.format("Synchronized %d users (%d nodes removed, %d nodes added): %s", users.size(), removed, added, String.join(", ", users.entrySet().stream().map(e -> e.getValue() + "-" + e.getKey()).toArray(String[]::new))));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}

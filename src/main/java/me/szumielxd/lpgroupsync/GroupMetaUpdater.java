package me.szumielxd.lpgroupsync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.szumielxd.lpgroupsync.hikari.HikariDB;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.matcher.NodeMatcher;
import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.DisplayNameNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.node.types.WeightNode;

public class GroupMetaUpdater {
	
	
	private final LPGroupSync plugin;
	private final Pattern mapperPattern = Pattern.compile("([^ ]+) +-> +([^ ]+)");
	private Map<String, String> mappings = new HashMap<>();
	private final List<String> names;
	
	
	public GroupMetaUpdater(LPGroupSync plugin) {
		this.plugin = plugin;
		this.plugin.getConfiguration().getStringList(ConfigKey.GROUP_METASYNC_MAPPINGS).forEach(str -> {
			Matcher match = mapperPattern.matcher(str);
			if (match.matches()) this.mappings.put(match.group(1), match.group(2));
			else this.mappings.put(str, str);
		});
		this.names = Collections.unmodifiableList(this.plugin.getConfiguration().getStringList(ConfigKey.GROUP_METASYNC_NAMES));
	}
	
	
	/**
	 * Update group's metadata values.
	 */
	public void updateGroups() {
		try {
			LuckPerms lp = LuckPermsProvider.get();
			
			Map<String, List<Node>> toRemove = new HashMap<>();
			if (names.contains("display")) lp.getGroupManager().searchAll(NodeMatcher.type(NodeType.DISPLAY_NAME)).get().forEach((k, v) -> toRemove.computeIfAbsent(k, s -> new ArrayList<>()).addAll(v)); // get display
			if (names.contains("prefix")) lp.getGroupManager().searchAll(NodeMatcher.type(NodeType.PREFIX)).get().forEach((k, v) -> toRemove.computeIfAbsent(k, s -> new ArrayList<>()).addAll(v)); // get prefix
			if (names.contains("suffix")) lp.getGroupManager().searchAll(NodeMatcher.type(NodeType.SUFFIX)).get().forEach((k, v) -> toRemove.computeIfAbsent(k, s -> new ArrayList<>()).addAll(v)); // get suffix
			if (names.contains("weight")) lp.getGroupManager().searchAll(NodeMatcher.type(NodeType.WEIGHT)).get().forEach((k, v) -> toRemove.computeIfAbsent(k, s -> new ArrayList<>()).addAll(v)); // get weight
			lp.getGroupManager().searchAll(NodeMatcher.type(NodeType.META)).get().forEach((k, v) -> toRemove.computeIfAbsent(k, s -> new ArrayList<>()).addAll(v.stream().filter(m -> names.contains(m.getMetaKey())).collect(Collectors.toList()))); // get other metadata
			
			// remove not synchronizing groups
			toRemove.entrySet().removeIf(e -> !mappings.values().contains(e.getKey()));
			
			// get synchronized nodes to add
			Map<String, Set<Node>> toAdd = new HashMap<>();
			Map<String, Set<Node>> map = new HashMap<>();
			if (!this.plugin.getDB().isEmpty()) for (HikariDB db : this.plugin.getDB()) db.getMetaNodesByGroup(this.names.toArray(new String[0]), this.mappings.keySet().toArray(new String[0]))
				.forEach((k, v) -> this.addAll(map.computeIfAbsent(k, s -> new HashSet<>()), v));
			map.forEach((group, nodes) -> {
				List<Node> removable = toRemove.get(this.mappings.get(group));
				nodes.forEach(node -> {
					if (removable == null || !removable.remove(node)) {
						toAdd.computeIfAbsent(group, k -> new HashSet<>()).add(node);
					}
					if (node instanceof ChatMetaNode && !this.plugin.getConfiguration().getBoolean(ConfigKey.GROUP_METASYNC_OVERRIDE)) removable.removeIf(node.getClass()::isInstance); // do not override previous chat meta
				});
			});
			toRemove.entrySet().removeIf(e -> e.getValue().isEmpty());
			Set<String> groups = new HashSet<>(toRemove.keySet());
			groups.addAll(toAdd.keySet());
			if (!groups.isEmpty()) for (String groupName : new ArrayList<>(groups)) {
				Group group = lp.getGroupManager().getGroup(groupName);
				if (group == null) {
					toRemove.remove(groupName);
					toAdd.remove(groupName);
					groups.remove(groupName);
					this.plugin.getLogger().warning(String.format("Cannot find group with name '%s'. Ignoring...", groupName));
					continue;
				};
				if (toRemove.containsKey(groupName)) toRemove.get(groupName).forEach(group.data()::remove);
				if (toAdd.containsKey(groupName)) toAdd.get(groupName).forEach(group.data()::add);
				lp.getGroupManager().saveGroup(group);
			}
			int removed = toRemove.values().stream().mapToInt(List::size).sum();
			int added = toAdd.values().stream().mapToInt(Set::size).sum();
			if (!groups.isEmpty()) this.plugin.getLogger().info(String.format("Synchronized %d groups (%d meta nodes removed, %d meta nodes added): %s", groups.size(), removed, added, String.join(", ", groups.toArray(new String[0]))));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void addAll(Set<Node> base, Collection<Node> toAdd) {
		HashMap<String, Node> map = new HashMap<>();
		Consumer<Node> putIfCan = node -> {
			if (node instanceof DisplayNameNode) map.putIfAbsent("display", node);
			else if (node instanceof PrefixNode) map.putIfAbsent("prefix", node);
			else if (node instanceof SuffixNode) map.putIfAbsent("suffix", node);
			else if (node instanceof WeightNode) map.putIfAbsent("weight", node);
			else if (node instanceof MetaNode) map.putIfAbsent("meta."+((MetaNode)node).getMetaKey(), node);
		};
		base.forEach(putIfCan);
		toAdd.forEach(putIfCan);
		base.clear();
		base.addAll(map.values());
	}
	

}

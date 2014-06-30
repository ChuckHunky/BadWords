package uk.co.warmlight.andrew.BadWords;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ServerCommandExecutor implements CommandExecutor {
	
	public static BadWords plugin;
	ChatColor RED = ChatColor.RED;
	ChatColor WHITE = ChatColor.WHITE;
	
	public ServerCommandExecutor(BadWords instance) {
		plugin = instance;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		Map<String,String> badWords = plugin.getWordList();
		
		if (cmd.getName().equalsIgnoreCase("badw")) {
			
			if (sender.hasPermission("badw.all")) {
			
				if (args.length > 0) {
					
					String param = args[0].toLowerCase();
							
					if (param.equals("list")) {
						sender.sendMessage(RED+"[BadWords] "+WHITE+"List of banned words:");
											
						for (Map.Entry<String, String> entry : badWords.entrySet()) {						
							String thisWord = entry.getKey();
							String thisReplace = entry.getValue();
							String replaceAppend = (thisReplace.length() > 0) ? ":"+thisReplace : "";
							sender.sendMessage(thisWord+replaceAppend);			
						}
						return true;
					} else if (param.equals("add")) {
						if (args.length > 1) {
							String badWord="";
							if (args.length > 2) {
								StringBuilder sb = new StringBuilder();
								for (int i = 1;i<args.length;i++) {
									sb.append(args[i]);
									if (i<args.length - 1) {
										sb.append(" ");
									}
								}
								badWord = sb.toString();
							} else {
								badWord = args[1];
							}
							if (plugin.addBannedWord(badWord)) { 
								sender.sendMessage(RED+"[BadWords] "+WHITE+"'" + badWord + "' added");
							} else {
								sender.sendMessage(RED+"[BadWords] "+WHITE+"'" + badWord +"' is already in the list");
							}
							return true;
						}
					} else if (param.equals("delete")) {
						if (args.length > 1) {
							String badWord="";
							if (args.length > 2) {
								StringBuilder sb = new StringBuilder();
								for (int i = 1;i<args.length;i++) {
									sb.append(args[i]);
									if (i<args.length - 1) {
										sb.append(" ");
									}
								}
								badWord = sb.toString();
							} else {
								badWord = args[1];
							}
							if (plugin.delBannedWord(badWord)) {							
								sender.sendMessage(RED+"[BadWords] "+WHITE+"'" + badWord + "' deleted");
							} else {
								sender.sendMessage(RED+"[BadWords] "+WHITE+"'" + badWord + "' is not in the list");
							}
							return true;
						}
					} else if (param.equals("ignorelist")) {
						sender.sendMessage(RED+"[BadWords] "+WHITE+"List of currently online players being ignored:");
						ArrayList<String> ignoredPlayers = plugin.getIgnoredPlayers();
						for (String s : ignoredPlayers) {
							UUID uuid = UUID.fromString(s);
							Player p = Bukkit.getPlayer(uuid);
							if (p != null) {
								sender.sendMessage(RED+"[BadWords] "+WHITE+p.getName());
							}
						}
						return true;
					} else if (param.equals("ignore")) {
						if (args.length > 1) {
							String uName = args[1];
							UUID uuid = plugin.getUUIDFromName(uName);
							if (plugin.addIgnoredPlayer(uuid)) {
								sender.sendMessage(RED+"[BadWords] "+WHITE+"'" + uName + "' is now being ignored");
							} else {
								sender.sendMessage(RED+"[BadWords] "+WHITE+"'" + uName + "' is already on the ignore list");
							}
							return true;
						}
					} else if (param.equals("watch")) {
						if (args.length > 1) {
							String uName = args[1];
							UUID uuid  = plugin.getUUIDFromName(uName);
							if (plugin.delIgnoredPlayer(uuid)) {
								sender.sendMessage(RED+"[BadWords] "+WHITE+"'" + uName + "' will now receive warnings");
							} else {
								sender.sendMessage(RED+"[BadWords] "+WHITE+"'" + uName + "' is not currently being ignored");
							}
							return true;
						}
					} else if (param.equals("warnings")) {
						if (args.length > 1) {
							String uName = args[1];
							UUID uuid = plugin.getUUIDFromName(uName);
							Integer remWarn = plugin.getRemWarn(uuid);
							if (remWarn != plugin.getDefaultWarn()) {
								if (remWarn < 0) {
									sender.sendMessage(RED+"[BadWords] "+WHITE+uName + " has been banned by BadWords");
								} else {
									sender.sendMessage(RED+"[BadWords] "+WHITE+uName + " has " + remWarn.toString() + " warnings remaining");
								}
							} else {
								sender.sendMessage(RED+"[BadWords] "+WHITE+uName + " has not been warned");
							}
							return true;
						}
	
					} else if (param.equals("reset")) {
						if (args.length > 1) {
							String uName = args[1];
							resetWarnings(uName, sender);
							return true;
						}
					} else if (param.equals("reload")) {
						plugin.reloadConfig();
						sender.sendMessage(RED+"[BadWords] " +WHITE+ "Configuration reloaded");
						return true;
					} else if (param.equals("update")) {
						@SuppressWarnings("unused")
						Updater updater = new Updater(plugin, plugin.getProjectID(), plugin.getUpdateFile(), Updater.UpdateType.NO_VERSION_CHECK, true);
						return true;
					} else if (param.equals("purge")) {
// Purging is working but look at "purge" only unbanning, then their warnings are reset when they login, this is so they get the message when they login
//						List<String> allWarned = plugin.getAllWarned();
//						plugin.logger.info("" + allWarned.size());
//						Iterator<String> i = allWarned.iterator();
//						Integer c = 0;
//						String uName;
//						plugin.logger.info("[BadWords] Purging expired warnings");
//						while (i.hasNext()) {
//							uName = i.next();
//							plugin.logger.info("->" + uName);
//							if (plugin.getWarnTimeout() != -1 && plugin.hasWarningExpired(uName)) {
//								resetWarnings(uName, sender);
//								plugin.logger.info("[BadWords] Purged warnings for " + uName);
//								c++;
//							}
//						}
//						plugin.logger.info("[BadWords] Purged "+ c + " players with warnings");
//						sender.sendMessage(RED+"[BadWords] "+ c + " player purged and unbanned");
//						return true;
					}
					return false;
				} else {
					return false;
				}
			} else {
				sender.sendMessage(RED+"[BadWords] "+WHITE+" You do not have permission to do this.");
			}
		}
			
		return false;
	}
	
	public boolean resetWarnings(String uName, CommandSender sender) {
		
		UUID uuid = plugin.getUUIDFromName(uName);
		
		if (plugin.wasWarned(uuid)) {
			BanList bl = Bukkit.getBanList(BanList.Type.NAME);
			bl.pardon(uName);
			plugin.removeBanned(uuid);
			sender.sendMessage(RED+"[BadWords] "+WHITE+ uName + " warnings reset and unbanned");
			return true;
		} else {
			sender.sendMessage(RED+"[BadWords] "+WHITE+uName + " has not been warned");
			return false;
		}
		
	}

}

package uk.co.warmlight.andrew.BadWords;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;

public class ServerChatPlayerListener implements Listener {
	public static BadWords plugin;
	
	public ServerChatPlayerListener(BadWords instance) {
		plugin = instance;
	}
	
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent chat) {
		Player p = chat.getPlayer();
		String message = chat.getMessage();
		ChatColor RED = ChatColor.RED;
		ChatColor WHITE = ChatColor.WHITE;
		
		// Get the player's name &  UUID
		String uName = p.getName();
		UUID uuid = p.getUniqueId();
				
		if (plugin.didTheySwear(message)) {
			if (p.isOp() && plugin.getIgnoreOp()) {
				return;
			}
			if (plugin.getIsIgnored(uuid)) {
				return;
			}
			// Log the message (if configured to do so)
			if (plugin.getLogSwear()) {
				plugin.logger.info("[BadWords] "+uName+" just said '"+message+"'");
			}
			// If there is a censorship for the message,
			// do the substitution
			message = plugin.censorMessage(message);
			if (message.length() > 0) {
				chat.setMessage(message);
			} else {
				chat.setCancelled(true);
			}
			
			// Only do the whole warning, kicking / banning etc.
			// If default warnings is not set to -1, which indicates
			// that the admin doesn't want all that stuff
			if (plugin.getDefaultWarn() != -1) {
				Integer warnRemaining = plugin.getRemWarn(uuid);
				warnRemaining--;
				plugin.setRemWarn(uuid, warnRemaining, p);
				if (warnRemaining >= 0) {
					if (plugin.getNotifyPlayer()) {
						String newMessage = plugin.subMessage(plugin.getMessageWarn(), uName, warnRemaining); // You cannot say that
						p.sendMessage(RED + "[BadWords] " + WHITE + newMessage);
					}
					if (plugin.getNotifyOthers() && message.length() == 0) {
						String newMessage = plugin.subMessage(plugin.getMessageBroadcast(), uName, warnRemaining); // Player just said a banned word and received warning
						plugin.getServer().broadcastMessage(RED + "[BadWords] " + WHITE + newMessage);
					}
				} else {
					plugin.kickBadPeople(p, plugin.subMessage(plugin.getMessageDonePlayer(), uName, warnRemaining)); // You have been banned
					if (plugin.getNotifyOthers()) {
						plugin.getServer().broadcastMessage(RED + "[BadWords] " + WHITE + plugin.subMessage(plugin.getMessageDoneBroadcast(), uName, warnRemaining)); // Player has been banned / kicked
					}

					// Ban the player
					if (plugin.getBanAction()) {
						plugin.banPlayer(p);
					} else {
						// Set their remaining warnings back to 0
						// so they'll get kicked again next time
						plugin.setRemWarn(uuid, 0, p);
					}
				}
			}
			
		}
	}
}



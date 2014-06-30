package uk.co.warmlight.andrew.BadWords;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;

public class ServerJoinPlayerListener implements Listener {
	public static BadWords plugin;
	
	public ServerJoinPlayerListener(BadWords instance) {
		plugin = instance;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		
		Player p = event.getPlayer();
		
		ChatColor RED = ChatColor.RED;
		ChatColor WHITE = ChatColor.WHITE;
		
		String uName = event.getPlayer().getName();
		
		UUID uuid = event.getPlayer().getUniqueId();
		
		Integer warnRemaining = plugin.getRemWarn(uuid);
		
		// If they have warnings, check if they have expired
		// and that the admin wants them to expire (i.e. expiry not set to -1)
		if (warnRemaining < plugin.getDefaultWarn()) {
			// If they have, remove them then tell the player
			if (plugin.getWarnTimeout() != -1 && plugin.hasWarningExpired(uuid)) {
				plugin.removeBanned(uuid);
				p.sendMessage(RED + "[BadWords] " + WHITE + plugin.getMessageExpired());
			} else {
				// If they are still applicable, tell the player
				p.sendMessage(RED + "[BadWords] " + WHITE + plugin.subMessage(plugin.getMessageJoin(), uName, warnRemaining));	
			}
			
		}
		
		if (p.isOp() && plugin.getIsUpdate() && plugin.getNotifyOp()) {
			p.sendMessage(RED + "[BadWords] " + WHITE + "[BadWords] ** There is a NEW VERSION of BadWords available **");
			p.sendMessage(RED + "[BadWords] " + WHITE + "[BadWords] ONLY UPDATE IF YOU ARE RUNNING CRAFTBUKKIT 1.7 OR GREATER.");
			p.sendMessage(RED + "[BadWords] " + WHITE + "[BadWords] Please update at http://dev.bukkit.org/server-mods/badwords/");					
			p.sendMessage(RED + "[BadWords] " + WHITE + "[BadWords] or type /badw update to update now");
		}
	}
}
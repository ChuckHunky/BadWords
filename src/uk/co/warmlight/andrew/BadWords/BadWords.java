package uk.co.warmlight.andrew.BadWords;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import uk.co.warmlight.andrew.BadWords.Updater.ReleaseType;

public class BadWords extends JavaPlugin {
	
	public static BadWords plugin;
	public final Logger logger = Logger.getLogger("Minecraft");
	public final ServerChatPlayerListener playerChatListener = new ServerChatPlayerListener(this);
	public final ServerJoinPlayerListener playerJoinListener = new ServerJoinPlayerListener(this);
	private Map<String,String> wordList = new HashMap<String,String>();
	private ArrayList<String> ignoredPlayers = new ArrayList<String>();
	private Boolean isUpdated = false;
	private String updateName;
	private String updateVersion;
	private ReleaseType updateType;
	private String updateLink;
	private Integer projectID = 35346;
	
	@Override
	public void onDisable() {
		PluginDescriptionFile pdffile = this.getDescription();
		this.logger.info(pdffile.getName()+" "+pdffile.getVersion()+" is now disabled");
	}
	
	@Override
	public void onEnable() {
		updateConfigFile();
		getBadWords();
		migrateToUUID();
		loadConfiguration();
		loadIgnoredPlayers();
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(playerChatListener, this);
		pm.registerEvents(playerJoinListener, this);
		getCommand("badw").setExecutor(new ServerCommandExecutor(this));
		PluginDescriptionFile pdffile = this.getDescription();
		if (getConfig().getBoolean("NewVersion.CheckForUpdates")) {
			if (getConfig().getBoolean("NewVersion.auto-update")) {
				@SuppressWarnings("unused")
				Updater updater = new Updater(this, projectID, this.getFile(), Updater.UpdateType.DEFAULT, true);
			} else {
				Updater updater = new Updater(this, projectID, this.getFile(), Updater.UpdateType.NO_DOWNLOAD, false);
				boolean isUpdate = updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE;
				if (isUpdate) {
					setIsUpdated(true);
					setUpdateName(updater.getLatestName());
					setUpdateVersion(updater.getLatestGameVersion());
					setUpdateType(updater.getLatestType());
					setUpdateLink(updater.getLatestFileLink());
					this.logger.info("[BadWords] ** There is a NEW VERSION of BadWords available **");
					this.logger.info("[BadWords] ONLY UPDATE IF YOU ARE RUNNING CRAFTBUKKIT 1.7 OR GREATER.");
					this.logger.info("[BadWords] Please update at http://dev.bukkit.org/server-mods/badwords/");					
					this.logger.info("[BadWords] or type /badw update to update now");
				}
			}
		}
		this.logger.info(pdffile.getName()+" "+pdffile.getVersion()+" is enabled");
	}
	
	public void loadConfiguration() {
		String badWords[] = {"bum","idiot:silly","you are crap"};
		getConfig().addDefault("NewVersion.CheckForUpdates", true);
		getConfig().addDefault("NewVersion.auto-update", true);
		getConfig().addDefault("NewVersion.notifyOp", true);
		getConfig().addDefault("Warnings.default", 3);
		getConfig().addDefault("Warnings.timeout", 168);
		getConfig().addDefault("Ignore.op", true);
		getConfig().addDefault("Action.ban", true);
		getConfig().addDefault("Log.swear", true);
		getConfig().addDefault("Notify.player", true);
		getConfig().addDefault("Notify.others", true);
		getConfig().addDefault("Message.join", "Remember, you have %remwarning% warning%s% left before you will be %action% for bad language.");
		getConfig().addDefault("Message.warn", "You cannot say that, %remwarning% warning%s% left!");
		getConfig().addDefault("Message.doneplayer", "You have been %action% for repeated bad language");
		getConfig().addDefault("Message.donebroadcast", "%player% has been %action% for repeated bad language");
		getConfig().addDefault("Message.banreason", "Repeated bad language");
		getConfig().addDefault("Message.warningsexpired", "Your warnings have expired and have been reset");
		getConfig().addDefault("BannedWords",Arrays.asList(badWords));
		getConfig().options().copyDefaults(true);
	    saveConfig();
	}
	
	public Integer getDefaultWarn() {
		Integer defaultWarnings = getConfig().getInt("Warnings.default", 3);
		return defaultWarnings;
	}
	
	public Integer getRemWarn(UUID uuid) {
			Integer warnRemaining = getConfig().getInt("Warned."+uuid+".remaining", getDefaultWarn());
			return warnRemaining;
	}
	
	public Boolean getBanAction() {
		return getConfig().getBoolean("Action.ban");
	}
	
	public void addUUIDEntry(UUID uuid, Integer warnRemaining, Long timestamp, String pName)
	{
		getConfig().set("Warned."+uuid+".remaining", warnRemaining);
		getConfig().set("Warned."+uuid+".timestamp", timestamp);
		getConfig().set("Warned."+uuid+".name", pName.toLowerCase());
		saveConfig();
	}
	
	public File getUpdateFile() {
		return this.getFile();
	}
	
	public Integer getProjectID() {
		return this.projectID;
	}
	
	public void setIsUpdated (Boolean status) {
		this.isUpdated = status;
	}
	
	public Boolean getIsUpdated() {
		return this.isUpdated;
	}
	
	public void setUpdateName (String un) {
		this.updateName = un;
	}
	
	public String getUpdateName() {
		return updateName;
	}
	
	public void setUpdateVersion (String uv) {
		this.updateVersion = uv;
	}
	
	public String getUpdateVersion() {
		return updateVersion;
	}
	
	public void setUpdateType (ReleaseType ut) {
		this.updateType = ut;
	}
	
	public ReleaseType getUpdateType() {
		return updateType;
	}
	
	public void setUpdateLink (String ul) {
		this.updateLink = ul;
	}
	
	public String getUpdateLink() {
		return updateLink;
	}
	
	public void delPreUUIDEntry(String pName) {
		getConfig().set("Warned."+pName.toLowerCase(), null);
		saveConfig();
	}
	
	public void setRemWarn(UUID uuid, Integer warnRemaining, Player p) {
		// Get the current timestamp, we use this for the warned time
		Long secs = System.currentTimeMillis();
		getConfig().set("Warned."+uuid+".remaining", warnRemaining);
		getConfig().set("Warned."+uuid+".timestamp", secs);
		getConfig().set("Warned."+uuid+".name", p.getName().toLowerCase());
		saveConfig();
	}
	
	public boolean wasWarned(UUID uuid) {
		return getConfig().contains("Warned."+uuid);
	}
	
	public void removeBanned(UUID uuid) {
		getConfig().set("Warned."+uuid, null);
		saveConfig();
	}
	
	public boolean getNotifyPlayer() {
		return getConfig().getBoolean("Notify.player");
	}
	
	public boolean getNotifyOthers() {
		return getConfig().getBoolean("Notify.others");
	}
	
	public boolean getLogSwear() {
		return getConfig().getBoolean("Log.swear");
	}
	
	public Long getLastWarned(UUID uuid) {
		return getConfig().getLong("Warned."+uuid+".timestamp", getDefaultWarn());	
	}
	
	public boolean getIgnoreOp() {
		return getConfig().getBoolean("Ignore.op");
	}
	
	public Double getWarnTimeout() {
		return getConfig().getDouble("Warnings.timeout");
	}
	
	public boolean getIsUpdate() {
		return this.isUpdated;
	}
	
	public boolean getHasUUID(String uuid) {
		return getConfig().contains("Warned."+uuid);
	}
	
	public boolean getHasName(String pName) {
		return getConfig().contains("Warned."+pName.toLowerCase());
	}
	
	public boolean hasWarningExpired(UUID uuid) {
		// What's the timestamp now
		Integer now = (int) System.currentTimeMillis();
		// What's the expiry timeout
		Double timeout = getWarnTimeout() * 3600000;
		// When were they last warned
		Long lastWarned = getLastWarned(uuid);
		
		return (lastWarned + timeout < now) ? true : false;
	}
	
	public boolean addBannedWord(String badWord) {
		
		if (badWord.indexOf(":") > 0) {
			String thisSplut[] = badWord.split(":",2);
			wordList.put(thisSplut[0],thisSplut[1]);
		} else {
			wordList.put(badWord, "");
		}
		saveWordList();
		return true;
	}
	
	public boolean delBannedWord(String badWord) {
		
		if (badWord.indexOf(":") > 0) {
			String thisSplut[] = badWord.split(":",2);
			badWord = thisSplut[0];
		}
		
		if (wordList.containsKey(badWord)) {
			wordList.remove(badWord);
			saveWordList();
			return true;
		} else {
			return false;
		}
	}
	
	public void saveWordList() {
		
		ArrayList<String> tempList = new ArrayList<String>();
		
		// Write a new list
		for (Map.Entry<String, String> entry : wordList.entrySet()) {
			String thisWord = entry.getKey();
			String thisReplace = entry.getValue();
			String writeMe = (thisReplace.length() > 0) ? thisWord+":"+thisReplace : thisWord;
			tempList.add(writeMe);
		}
		getConfig().set("BannedWords", tempList.toArray());
		saveConfig();
	}
	

	
	public void updateConfigFile() {

		// Only do this if the old configuration is present
		if (getConfig().isConfigurationSection("Warnings.warned")) {
			
			// Get all the currently warned players and put them in a HashMap
			Map<String,Object> userList= getConfig().getConfigurationSection("Warnings.warned").getValues(false);
			
			Integer c = 0;
			
			// Iterate the hashmap
			for(Map.Entry<String,Object> entry : userList.entrySet()) {
				
				String name = entry.getKey();
				Integer warn = (Integer) entry.getValue();
				
				// Get the current timestamp, we use this for the warned time
				Long secs = System.currentTimeMillis();
				
				// Write the new entries
				getConfig().set("Warned."+name.toLowerCase()+".remaining", warn);
				getConfig().set("Warned."+name.toLowerCase()+".timestamp", secs);
				
				// Delete the old entry
				getConfig().set("Warnings.warned."+name.toLowerCase(), null);
				
				c++;
			}
			
			// Remove the old section
			getConfig().set("Warnings.warned", null);
			
			// Remove the "kick" option
			getConfig().set("Action.kick", null);
			
			saveConfig();
			
			this.logger.info("[BadWords] ** Updated config file to new format. " + c + " users updated **");

		}
				
	}
	
	public void migrateToUUID() {
		
		// Bail if no-one has ever been warned
		// or this is a first run
		if (!getConfig().contains("Warned")) {
			return;
		}
		
		// Get all the currently warned players and put them in a HashMap
		Map<String,Object> userList = getConfig().getConfigurationSection("Warned").getValues(false);
		
		Integer c = 0;
		
		final List<String> players = new ArrayList<String>();

		// Iterate the hashmap
		for(Map.Entry<String,Object> entry : userList.entrySet()) {
			
			String identifier = entry.getKey();

			// Only do this if the current player isn't already migrated
			if (!getConfig().contains("Warned." + identifier.toLowerCase()+".name")) {
				players.add(identifier);
				c++;
			}
		}
			
		if (c > 0) {
			
			this.logger.info("[BadWords] About to migrate " + c + " players to UUID support");
			
			Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
				@Override
				public void run() {
					UUIDFetcher fetcher = new UUIDFetcher(players, true);
					Map<String, UUID> response = null;
					Integer d = 0;
					try {
						response = fetcher.call();
						for (Map.Entry<String, UUID> entry : response.entrySet()) {
							Integer warn = getConfig().getInt("Warned."+entry.getKey().toLowerCase()+".remaining");
							Long timestamp = getConfig().getLong("Warned."+entry.getKey().toLowerCase()+".timestamp");
							// Add a new entry with the UUID as the key
							addUUIDEntry(entry.getValue(), warn, timestamp, entry.getKey().toLowerCase());
							// Remove the old entry
							delPreUUIDEntry(entry.getKey().toLowerCase());
							//logger.info("[BadWords] Migrated user:" + entry.getKey() + " UUID:" + entry.getValue());
							d++;
						}
					} catch (Exception e) {
						logger.warning("[BadWords] Unable to contact Mojang UUID lookup - aborting migration");
					}
					logger.info("[BadWords] Migrated " + d + " players to UUID support");
				}
			});
		}
		
	}
	
	public boolean didTheySwear(String message) {
		
		boolean found = false;
		
		for (Map.Entry<String, String> entry : wordList.entrySet()) {
			
			String thisWord = entry.getKey();
			String thisRegex = "\\b"+thisWord+"\\b";
			Pattern patt = Pattern.compile(thisRegex, Pattern.CASE_INSENSITIVE);
			Matcher m = patt.matcher(message);
			if (m.find()) {
				found = true;
			}			
		}
		
		return found;
	}
	
	public String censorMessage(String message) {
		
		boolean censored = false;
		boolean uncensored = false;
		
		for (Map.Entry<String, String> entry : wordList.entrySet()) {
			
			String thisWord = entry.getKey();
			String thisValue = entry.getValue();
			String thisRegex = "\\b"+thisWord+"\\b";
			Pattern patt = Pattern.compile(thisRegex, Pattern.CASE_INSENSITIVE);
			Matcher m = patt.matcher(message);
			if (m.find()) {
				if (thisValue.length() > 0) {
					message = message.replaceAll(thisRegex,thisValue);
					censored = true;
				} else {
					uncensored = true;
				}
			}			
		}	
		if ((!censored && !uncensored) || (!uncensored && censored)) {
			return message;
		} else {
			return "";
		}
	}
	
	public void getBadWords() {
		List<String> badW = getConfig().getStringList("BannedWords");
		Iterator<String> i = badW.iterator();
		while (i.hasNext()) {
			String thisLine = i.next();
			if (thisLine.indexOf(":") > 0) {
				String thisSplut[] = thisLine.split(":",2);
				wordList.put(thisSplut[0],thisSplut[1]);
			} else {
				wordList.put(thisLine,"");
			}
		}
	}
	
	public List<String> getAllWarned() {
		return getConfig().getStringList("Warned");
	}
	
	public Map<String,String> getWordList() {
		return wordList;	
	}
	
	public boolean getNotifyOp() {
		return getConfig().getBoolean("NewVersion.notifyOp");
	}
	
	public boolean getCheckForUpdates() {
		return getConfig().getBoolean("NewVersion.CheckForUpdates");
	}
	
	public String subMessage(String message, String pName, Integer remWarnings) {
		String remWarningRegex = "%remwarning%";
		String sRegex = "%s%";
		String nameRegex = "%player%";
		String actionRegex = "%action%";
		String action = (getBanAction()) ? "banned" : "kicked";
		String s = (remWarnings == 1) ? "" : "s";
		message = message.replaceAll(remWarningRegex, remWarnings.toString());
		message = message.replaceAll(sRegex, s);
		message = message.replaceAll(nameRegex, pName);
		message = message.replaceAll(actionRegex, action);
		return message;
	}
	
	public String getMessageJoin() {
		return getConfig().getString("Message.join");
	}
	
	public String getMessageWarn() {
		return getConfig().getString("Message.warn");
	}
	
	public String getMessageExpired() {
		return getConfig().getString("Message.warningsexpired");
	}
	
	public String getMessageBroadcast() {
		return getConfig().getString("Message.broadcast");
	}
	
	public String getMessageDonePlayer() {
		return getConfig().getString("Message.doneplayer");
	}
	
	public String getMessageDoneBroadcast() {
		return getConfig().getString("Message.donebroadcast");
	}
	
	public void kickBadPeople(Player p, String msg) {
		p.kickPlayer(msg);
	}
	
	public void banPlayer(Player p) {
		
		final String pName = p.getName();
		
		this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				BanList bl = Bukkit.getBanList(BanList.Type.NAME);
				bl.addBan(pName, getConfig().getString("Message.banreason"), null, "BadWords");				
			}
		});

	}
	
	public void unbanPlayer(Player p) {
		
		final String pName = p.getName();
		
		this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {		
				BanList bl = Bukkit.getBanList(BanList.Type.NAME);
				bl.pardon(pName);
			}
		});
	}
	
	public UUID getUUIDFromName(String pName) {
		UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(pName));
		Map<String, UUID> response = null;
		try {
			response = fetcher.call();
			UUID uuid = response.values().iterator().next();
			return uuid;
		} catch (Exception e) {
			logger.info("[BadWords] Unable to contact Mojang UUID lookup");
		}
		return null;
	}
	
	public void loadIgnoredPlayers() {
		if (getConfig().contains("Ignore.players")) {
			List<String> ignored = getConfig().getStringList("Ignore.players");
			if (ignored.size() > 0) {
				Iterator<String> i = ignored.iterator();
				while (i.hasNext()) {
					String thisLine = i.next();
					ignoredPlayers.add(thisLine);
				}
			}
		}
	}
	
	public ArrayList<String> getIgnoredPlayers() {
		return ignoredPlayers;
	}
	
	public boolean addIgnoredPlayer(UUID uuid) {
		if (!ignoredPlayers.contains(uuid.toString())) {
			ignoredPlayers.add(uuid.toString());
			saveIgnoredPlayers();
			return true;
		} else {
			return false;
		}
	}
	
	public boolean delIgnoredPlayer(UUID uuid) {
		if (ignoredPlayers.contains(uuid.toString())) {
			Integer i = ignoredPlayers.indexOf(uuid.toString());
			ignoredPlayers.remove(i.intValue());
			saveIgnoredPlayers();
			return true;
		} else {
			return false;
		}
	}
	
	public void saveIgnoredPlayers() {
		getConfig().set("Ignore.players", ignoredPlayers);
		saveConfig();
	}
	
	public boolean getIsIgnored(UUID passedUUID) {
		for (String s : ignoredPlayers) {
			UUID thisUUID = UUID.fromString(s);
			if (thisUUID.equals(passedUUID)) {
				return true;
			}
		}
		return false;
	}
	
}

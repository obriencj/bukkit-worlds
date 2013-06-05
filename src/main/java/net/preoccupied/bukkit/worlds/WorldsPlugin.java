package net.preoccupied.bukkit.worlds;


import java.io.File;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.Configuration;

import net.preoccupied.bukkit.PlayerCommand;


public class WorldsPlugin extends JavaPlugin {


    private Map<String, WorldConfig> worldSettings = null;
    

    public void onEnable() {
	loadWorlds();
	enableWorlds();

	PluginManager pm = getServer().getPluginManager();

	EventExecutor ee;
	Listener junk = new Listener() { };

	ee = new EventExecutor() {
		public void execute(Listener ignored, Event e) {
		    onEntityDamage((EntityDamageEvent) e);
		}
	    };
	pm.registerEvent(EntityDamageEvent.class, junk, EventPriority.LOW, ee, this);

	ee = new EventExecutor() {
		public void execute(Listener ignored, Event e) {
		    onPlayerChangedWorld((PlayerChangedWorldEvent) e);
		}
	    };
	pm.registerEvent(PlayerChangedWorldEvent.class, junk, EventPriority.LOW, ee, this);

	setupCommands();

	getLogger().info(this + " is enabled");
    }



    public void onDisable() {
	getLogger().info(this + " is disabled");
    }



    private void loadWorlds() {
	worldSettings = new HashMap<String, WorldConfig>();

	if(getDataFolder().mkdirs())
	    return;

	for(File f : getDataFolder().listFiles()) {
	    String wn = f.getName();
	    if(wn.endsWith(".yml")) {
		wn = wn.substring(0, wn.length() - 4);

		getLogger().info("loading configuration for " + wn);

		try {
		    WorldConfig wc = WorldConfig.loadConfig(wn, f);
		    worldSettings.put(wn, wc);

		} catch(Exception e) {
		    ; // log it
		}
	    }
	}
    }



    private void enableWorlds() {
	for(Map.Entry<String,WorldConfig> entry : worldSettings.entrySet()) {
	    WorldConfig wc = entry.getValue();
	    if(wc.enabled) {
		wc.createWorld();
	    }
	}
    }



    /*
      Here we catch the event that causes a player to be damaged, and
      depending on the settings for the world, cancel it.

      We differentiate the following types of damage:
      - Player hurting a player (pvp)
      - Monster hurting a player (pvm)
      - Environment hurting a player (pve)
     */
    private void onEntityDamage(EntityDamageEvent ede) {
	if(ede.isCancelled()) return;

	Entity attacker = null;
	Entity defender = ede.getEntity();

	if(! (defender instanceof Player))
	    return;

	if(ede instanceof EntityDamageByEntityEvent) {
	    EntityDamageByEntityEvent ev = (EntityDamageByEntityEvent) ede;
	    attacker = ev.getDamager();
	}

	WorldConfig wc = worldSettings.get(defender.getWorld().getName());

	int kindness = 0;
	if(wc != null) kindness = wc.kindness;

	if(attacker != null) {
	    if(attacker instanceof Player) {
		if(0 != (kindness & WorldConfig.KIND_NO_PLAYER)) {
		    ede.setCancelled(true);
		}
	    } else {
		if(0 != (kindness & WorldConfig.KIND_NO_MONSTER)) {
		    ede.setCancelled(true);
		}
	    }
	} else {
	    if(0 != (kindness & WorldConfig.KIND_NO_ENVIRONMENT)) {
		ede.setCancelled(true);
	    }
	}
    }



    private void checkGameMode(Player player) {
	String world = player.getWorld().getName();
	WorldConfig wc = worldSettings.get(world);

	boolean creative = false;
	//if(wc != null) creative = wc.creative;
	
	GameMode mode = creative? GameMode.CREATIVE: GameMode.SURVIVAL;
	getServer().getLogger().info("setting game mode: " + mode.toString());
	player.setGameMode(mode);
    }



    private void onPlayerChangedWorld(PlayerChangedWorldEvent pcwe) {
	Player player = pcwe.getPlayer();
	checkGameMode(player);
    }


    
    private void setupCommands() {
	
	new PlayerCommand(this, "world") {
	    public boolean run(Player player, String worldname) {
		World world = Bukkit.getServer().getWorld(worldname);
		if(world == null) {
		    msg(player, "No such world:", worldname);
		    return true;
		}

		player.teleport(world.getSpawnLocation());
		return true;
	    }
	};


	new PlayerCommand(this, "world-list") {
	    public boolean run(Player player) {
		msg(player, "World names:");
		for(Map.Entry<String,WorldConfig> entry : worldSettings.entrySet()) {
		    String name = entry.getKey();
		    World world = Bukkit.getServer().getWorld(name);
		    msg(player, " ", entry.getKey(), ((world==null)? "[disabled]":"[enabled]"));
		}
		return true;
	    }
	};


	new PlayerCommand(this, "world-info") {
	    public boolean run(Player player, String worldname) {
		WorldConfig conf = worldSettings.get(worldname);
		World world = Bukkit.getServer().getWorld(worldname);

		if(world == null && conf == null) {
		    msg(player, "No such world:", worldname);
		    return true;
		}

		msg(player, "Information for World:", worldname);
		msg(player, " Title:", conf.title);
		msg(player, " Status:", ((world==null)? "disabled": "enabled"));

		if(world != null) {
		    msg(player, " Environment:", world.getEnvironment());
		}
		
		return true;
	    }
	};

    }

}


/* The end. */

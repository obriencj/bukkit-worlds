package net.preoccupied.bukkit.worlds;


import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageByProjectileEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import net.preoccupied.bukkit.permissions.PermissionCommand;


public class WorldsPlugin extends JavaPlugin {


    /* environment kindness levels */
    private static final int
	KIND_NO_PLAYER = 1,      /* can't be hurt by another player */
	KIND_NO_MONSTER = 2,     /* can't be hurt by a monster */
	KIND_NO_ENVIRONMENT = 4; /* can't be hurt by falling, cactus, tnt */ 


    private Map<String, Configuration> worldSettings = null;
    
    private Map<World, Integer> worldKindness = null;



    public void onEnable() {
	loadWorlds();

	PluginManager pm = getServer().getPluginManager();

	EventExecutor ee;
	ee = new EventExecutor() {
		public void execute(Listener ignored, Event e) {
		    onEntityDamage((EntityDamageEvent) e);
		}
	    };
	pm.registerEvent(Event.Type.ENTITY_DAMAGE, null, ee, Priority.Normal, this);

	setupCommands();

	getServer().getLogger().info(this + " is enabled");
    }



    public void onDisable() {
	getServer().getLogger().info(this + " is disabled");
    }



    private void loadWorlds() {
	worldSettings = new HashMap<String, Configuration>();
	worldKindness = new HashMap<World, Integer>();

	if(getDataFolder().mkdirs())
	    return;

	for(File f : getDataFolder().listFiles()) {
	    String wn = f.getName();
	    if(wn.endsWith(".yml")) {
		wn = wn.substring(0, wn.length() - 4);
		Configuration wc = new Configuration(f);
		worldSettings.put(wn, wc);
	    }
	}
	
	for(Map.Entry<String,Configuration> entry : worldSettings.entrySet()) {
	    Configuration wc = entry.getValue();
	    wc.load();
	    loadWorld(entry.getKey(), wc, false);
	}
    }



    private void loadWorld(String name, Configuration conf, boolean force) {
	boolean animals = true, monsters = true;
	boolean pvp = true, pvm = true, pve = true;
	boolean enabled = true;
	String envs = "NORMAL";
	String seed = null;
	String title = name;

	enabled = conf.getBoolean("world.enabled", enabled) || force;
	if(! enabled) return;

	animals = conf.getBoolean("world.animals", animals);
	monsters = conf.getBoolean("world.monsters", monsters);
	pvp = conf.getBoolean("world.pvp", pvp);
	pvm = conf.getBoolean("world.pvm", pvm);
	pve = conf.getBoolean("world.pve", pve);
	envs = conf.getString("world.environment", envs);
	seed = conf.getString("world.seed", seed);
	title = conf.getString("world.title", title);

	Environment env = Environment.valueOf(envs.toUpperCase());

	World world = null;
	if(seed == null) {
	    world = Bukkit.getServer().createWorld(name, env);
	} else {
	    world = Bukkit.getServer().createWorld(name, env, Long.parseLong(seed));
	}

	int kind = 0;
	kind |= pvp? 0: KIND_NO_PLAYER;
	kind |= pvm? 0: KIND_NO_MONSTER;
	kind |= pve? 0: KIND_NO_ENVIRONMENT;
	worldKindness.put(world, kind);

	world.setPVP(pvp);
	world.setSpawnFlags(monsters, animals);

	System.out.println("loaded configuration for \"" + name + "\"");
    }



    public void loadWord(String name, boolean force) {
	Configuration wc = worldSettings.get(name);
	if(wc != null) loadWorld(name, wc, force);
    }



    /*
      Here we catch the event that causes a player to be damaged, and
      depending on the settings for the world, cancel it.

      We differentiate the following types of damage:
      - Player hurting a player (pvp)
      - Monster hurting a player (pvm)
      - Environment hurting a player (pve)

      The last is a bit off, since I left Lava and Drowning options
      enabled, however I do turn off cactus, fall damage, and
      explosions.
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

	} else if(ede instanceof EntityDamageByProjectileEvent) {
	    EntityDamageByProjectileEvent ev = (EntityDamageByProjectileEvent) ede;
	    attacker = ev.getDamager();
	}

	Integer kindint = worldKindness.get(defender.getWorld());
	int kindness = 0;
	if(kindint != null) kindness = kindint;

	if(attacker != null) {
	    if(attacker instanceof Player) {
		if(0 != (kindness & KIND_NO_PLAYER)) {
		    ede.setCancelled(true);
		}
	    } else {
		if(0 != (kindness & KIND_NO_MONSTER)) {
		    ede.setCancelled(true);
		}
	    }
	} else {
	    if(0 != (kindness & KIND_NO_ENVIRONMENT)) {
		DamageCause cause = ede.getCause();
		if(cause == DamageCause.CONTACT || cause == DamageCause.FALL) {
		    ede.setCancelled(true);
		}
	    }
	}
    }


    
    private void setupCommands() {
	
	new PermissionCommand(this, "world") {
	    public boolean run(Player player, String worldname) {
		World world = Bukkit.getServer().getWorld(worldname);
		if(world == null) {
		    msg(player, "No such world: " + worldname);
		    return true;
		}

		player.teleport(world.getSpawnLocation());
		return true;
	    }
	};


	new PermissionCommand(this, "world-list") {
	    public boolean run(Player player) {
		msg(player, "World names:");
		for(Map.Entry<String,Configuration> entry : worldSettings.entrySet()) {
		    String name = entry.getKey();
		    World world = Bukkit.getServer().getWorld(name);
		    msg(player, " " + entry.getKey() + ((world==null)?" [disabled]":" [enabled]"));
		}
		return true;
	    }
	};


	new PermissionCommand(this, "world-info") {
	    public boolean run(Player player, String worldname) {
		Configuration conf = worldSettings.get(worldname);
		World world = Bukkit.getServer().getWorld(worldname);

		if(world == null && conf == null) {
		    msg(player, "No such world: " + worldname);
		    return true;
		}

		msg(player, "Information for World: " + worldname);
		msg(player, " Title: " + conf.getString("title", worldname));
		msg(player, " Status: " + ((world==null)? "disabled": "enabled"));

		if(world != null) {
		    msg(player, " Environment: " + world.getEnvironment());
		}
		
		return true;
	    }
	};

    }

}


/* The end. */

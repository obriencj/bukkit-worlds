package net.preoccupied.bukkit.worlds;


import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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

import org.bukkit.craftbukkit.CraftWorld;

import net.preoccupied.bukkit.permissions.PermissionCommand;


public class WorldsPlugin extends JavaPlugin {


    private static final int
	KIND_NO_PLAYER = 1,
	KIND_NO_MONSTER = 2,
	KIND_NO_ENVIRONMENT = 4;


    private Map<String, Configuration> worldSettings = null;
    
    private Map<World, Integer> worldKindness = null;



    public void onEnable() {
	PluginManager pm = getServer().getPluginManager();

	EventExecutor ee;
	ee = new EventExecutor() {
		public void execute(Listener ignored, Event e) {
		    onEntityDamage((EntityDamageEvent) e);
		}
	    };
	pm.registerEvent(Event.Type.ENTITY_DAMAGE, null, ee, Priority.Normal, this);

	setupCommands();
    }



    public void onDisable() {
	;
    }



    public void onLoad() {
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
	boolean hell = false;
	boolean pvp = true, pvm = true, pve = true;
	boolean enabled = true;
	String seed = null;

	enabled = conf.getBoolean("world.enabled", enabled) || force;
	if(! enabled) return;

	animals = conf.getBoolean("world.animals", animals);
	monsters = conf.getBoolean("world.monsters", monsters);
	pvp = conf.getBoolean("world.pvp", pvp);
	pvm = conf.getBoolean("world.pvm", pvm);
	pve = conf.getBoolean("world.pve", pve);
	hell = conf.getBoolean("world.hellworld", hell);
	seed = conf.getString("world.seed", seed);

	Environment env = hell? Environment.NETHER: Environment.NORMAL;

	World world = null;
	if(seed == null) {
	    world = Bukkit.getServer().createWorld(name, env);
	} else {
	    world = Bukkit.getServer().createWorld(name, env, Long.parseLong(seed));
	}
	System.out.println("loaded level \"" + name + "\"");

	int kind = 0;
	kind |= pvp? 0: KIND_NO_PLAYER;
	kind |= pvm? 0: KIND_NO_MONSTER;
	kind |= pve? 0: KIND_NO_ENVIRONMENT;
	worldKindness.put(world, kind);

	// this hasn't been exposed in vanilla Bukkit yet.
	CraftWorld cworld = (CraftWorld) world;
	cworld.getHandle().setSpawnFlags(monsters, animals);
    }



    public void loadWord(String name, boolean force) {
	Configuration wc = worldSettings.get(name);
	if(wc != null) loadWorld(name, wc, force);
    }



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
		    msg(player, " " + entry.getKey() + ((world==null)?"[disabled]":"[enabled]"));
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
		msg(player, " Status: " + ((world==null)?"disabled":"enabled"));
		// todo: monsters, environment, etc.
		
		return true;
	    }
	};

    }

}


/* The end. */

package net.preoccupied.bukkit.worlds;

import java.io.File;
import java.io.IOException;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;


public class WorldConfig {

    /* environment kindness levels */
    public static final int
	KIND_NO_PLAYER = 1,      /* can't be hurt by another player */
	KIND_NO_MONSTER = 2,     /* can't be hurt by a monster */
	KIND_NO_ENVIRONMENT = 4; /* can't be hurt by falling, cactus, tnt */ 


    final String name;
    boolean enabled = true;

    boolean structures = true;
    boolean animals = true, monsters = true;
    boolean pvp = true, pvm = true, pve = true;

    int kindness = 0;

    String environment = null;
    String worldtype = null;
    String seed = null;
    String generator = null;
    String title = null;


    public WorldConfig(String name) {
	this.name = name;
    }


    public void load(Configuration conf) {
	enabled = conf.getBoolean("world.enabled", enabled);
	
	structures = conf.getBoolean("world.structures", structures);
	animals = conf.getBoolean("world.animals", animals);
	monsters = conf.getBoolean("world.monsters", monsters);
	pvp = conf.getBoolean("world.pvp", pvp);
	pvm = conf.getBoolean("world.pvm", pvm);
	pve = conf.getBoolean("world.pve", pve);
	environment = conf.getString("world.environment", environment);
	worldtype = conf.getString("world.type", worldtype);
	seed = conf.getString("world.seed", seed);
	generator = conf.getString("world.generator", generator);
	title = conf.getString("world.title", title);
	
	int kind = 0;
	kind |= pvp? 0: KIND_NO_PLAYER;
	kind |= pvm? 0: KIND_NO_MONSTER;
	kind |= pve? 0: KIND_NO_ENVIRONMENT;
	kindness = kind;
    }

    
    public void load(File confFile) {
	YamlConfiguration conf = new YamlConfiguration();

	try {
	    conf.load(confFile);
	} catch(IOException ioe) {
	    ;
	} catch(InvalidConfigurationException ice) {
	    ;
	}

	load(conf);
    }


    public World createWorld() {
	WorldCreator wc = new WorldCreator(name);
	if(seed != null) {
	    wc.seed(Long.parseLong(seed));
	}
	if(generator != null) {
	    wc.generator(generator);
	}
	if(environment != null) {
	    wc.environment(Environment.valueOf(environment.toUpperCase()));
	}
	if(worldtype != null) {
	    wc.type(WorldType.valueOf(worldtype.toUpperCase()));
	}
	wc.generateStructures(structures);
	
	World world = wc.createWorld();
	world.setPVP(pvp);
	world.setSpawnFlags(monsters, animals);

	return world;
    }


    public static WorldConfig loadConfig(String name, File confFile) {
	WorldConfig wc = new WorldConfig(name);
	wc.load(confFile);
	return wc;
    }

}


/* The end. */

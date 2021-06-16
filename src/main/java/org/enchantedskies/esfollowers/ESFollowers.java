package org.enchantedskies.esfollowers;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.enchantedskies.esfollowers.commands.FollowerCmd;
import org.enchantedskies.esfollowers.commands.GetHexArmorCmd;
import org.enchantedskies.esfollowers.datamanager.ConfigManager;
import org.enchantedskies.esfollowers.datamanager.DataManager;
import org.enchantedskies.esfollowers.datamanager.FollowerManager;
import org.enchantedskies.esfollowers.events.EssentialsEvents;
import org.enchantedskies.esfollowers.events.FollowerGUIEvents;
import org.enchantedskies.esfollowers.events.FollowerUserEvents;
import org.enchantedskies.esfollowers.utils.SkullCreator;

import java.util.HashSet;
import java.util.UUID;

public final class ESFollowers extends JavaPlugin implements Listener {
    private static ESFollowers plugin;
    public static DataManager dataManager;
    public static ConfigManager configManager;
    public static FollowerManager followerManager;
    public static SkullCreator skullCreator = new SkullCreator();
    private final HashSet<UUID> guiPlayerSet = new HashSet<>();
    private final NamespacedKey followerKey = new NamespacedKey(this, "ESFollower");

    @Override
    public void onEnable() {
        plugin = this;
        configManager = new ConfigManager();
        dataManager = new DataManager();
        followerManager = new FollowerManager();

        Listener[] listeners = new Listener[] {
            this,
            new FollowerUserEvents(),
            new FollowerGUIEvents(guiPlayerSet),
            new FollowerCreator()
        };
        registerEvents(listeners);

        PluginManager pluginManager = getServer().getPluginManager();
        if (pluginManager.getPlugin("Essentials") != null) {
            pluginManager.registerEvents(new EssentialsEvents(), this);
        } else {
            getLogger().info("Essentials plugin not found. Continuing without Essentials.");
        }
        getCommand("followers").setExecutor(new FollowerCmd(guiPlayerSet));
        getCommand("gethexarmor").setExecutor(new GetHexArmorCmd());

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    if (entity.getPersistentDataContainer().has(followerKey, PersistentDataType.STRING)) entity.remove();
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (dataManager.getPlayerFollowerMap().containsValue(entity.getUniqueId())) continue;
            if (entity.getPersistentDataContainer().has(followerKey, PersistentDataType.STRING)) entity.remove();
        }
    }

    public static ESFollowers getInstance() {
        return plugin;
    }

    public void registerEvents(Listener[] listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }
}

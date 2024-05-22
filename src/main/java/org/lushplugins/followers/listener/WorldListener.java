package org.lushplugins.followers.listener;

import org.lushplugins.followers.Followers;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.lushplugins.lushlib.listener.EventListener;

public class WorldListener implements EventListener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityLoad(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.ARMOR_STAND) {
            return;
        }

        if (Followers.getInstance().getDataManager().getActiveArmorStandsSet().contains(entity.getUniqueId())) {
            // Ran if the entity is an active FollowerEntity
            if (event.isCancelled() && Followers.getInstance().getConfigManager().shouldForceSpawn()) {
                event.setCancelled(false);
            }
        } else if (entity.getPersistentDataContainer().has(Followers.getInstance().getFollowerKey(), PersistentDataType.STRING)) {
            // Ran if the entity is an inactive FollowerEntity
            entity.remove();
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        int[] entityLoadAttempt = new int[1];
        Bukkit.getScheduler().runTaskTimer(Followers.getInstance(), (task) -> {
            entityLoadAttempt[0] += 1;
            if (entityLoadAttempt[0] >= 24) {
                task.cancel();
                return;
            }
            if (event.getChunk().isEntitiesLoaded()) {
                Entity[] entities = event.getChunk().getEntities();
                for (Entity entity : entities) {
                    if (entity.getType() != EntityType.ARMOR_STAND) {
                        continue;
                    }

                    if (Followers.getInstance().getDataManager().getActiveArmorStandsSet().contains(entity.getUniqueId())) {
                        continue;
                    }

                    if (entity.getPersistentDataContainer().has(Followers.getInstance().getFollowerKey(), PersistentDataType.STRING)) {
                        entity.remove();
                    }
                }
                task.cancel();
            }
        }, 50, 100);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (event.getChunk().isEntitiesLoaded()) {
            Entity[] entities = event.getChunk().getEntities();
            for (Entity entity : entities) {
                if (entity.getType() != EntityType.ARMOR_STAND) {
                    continue;
                }

                Followers.getInstance().getDataManager().removeActiveArmorStand(entity.getUniqueId());
                if (entity.getPersistentDataContainer().has(Followers.getInstance().getFollowerKey(), PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
    }
}

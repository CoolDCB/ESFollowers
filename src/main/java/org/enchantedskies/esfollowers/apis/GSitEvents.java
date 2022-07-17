package org.enchantedskies.esfollowers.apis;

import dev.geco.gsit.api.event.PlayerGetUpSitEvent;
import dev.geco.gsit.api.event.PlayerSitEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.enchantedskies.esfollowers.ESFollowers;
import org.enchantedskies.esfollowers.FollowerEntity;

public class GSitEvents implements Listener {

    @EventHandler
    public void onPlayerSit(PlayerSitEvent event) {
        Player player = event.getPlayer();
        FollowerEntity follower = ESFollowers.dataManager.getPlayerFollowerMap().get(player.getUniqueId());
        if (follower == null) return;
        follower.setPose("sitting");
    }

    @EventHandler
    public void onPlayerGetUpFromSeat(PlayerGetUpSitEvent event) {
        Player player = event.getPlayer();
        FollowerEntity follower = ESFollowers.dataManager.getPlayerFollowerMap().get(player.getUniqueId());
        if (follower == null) return;
        follower.setPose("default");
    }
}

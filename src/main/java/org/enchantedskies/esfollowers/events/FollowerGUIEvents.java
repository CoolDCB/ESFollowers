package org.enchantedskies.esfollowers.events;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.enchantedskies.esfollowers.ESFollowers;
import org.enchantedskies.esfollowers.FollowerEntity;
import org.enchantedskies.esfollowers.FollowerGUI;
import org.enchantedskies.esfollowers.datamanager.FollowerUser;
import org.enchantedskies.esfollowers.utils.SignMenuFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class FollowerGUIEvents implements Listener {
    private final SignMenuFactory signMenuFactory = new SignMenuFactory();
    private final ESFollowers plugin = ESFollowers.getInstance();
    private final HashSet<UUID> openInvPlayerSet;
    private final HashMap<UUID, FollowerEntity> playerFollowerMap;
    private final ItemStack noFollowers = new ItemStack(Material.BARRIER);
    private final ItemStack nextPage = new ItemStack(Material.ARROW);
    private final ItemStack previousPage = new ItemStack(Material.ARROW);
    private final ItemStack followerToggleEnabled = new ItemStack(Material.LIME_WOOL);
    private final ItemStack followerToggleDisabled = new ItemStack(Material.RED_WOOL);

    public FollowerGUIEvents(HashSet<UUID> playerSet) {
        this.openInvPlayerSet = playerSet;
        this.playerFollowerMap = ESFollowers.dataManager.getPlayerFollowerMap();

        ItemMeta barrierMeta = noFollowers.getItemMeta();
        barrierMeta.setDisplayName("§cYou don't own any followers!");
        noFollowers.setItemMeta(barrierMeta);

        ItemMeta nextPageMeta = nextPage.getItemMeta();
        nextPageMeta.setDisplayName("§eNext Page ->");
        nextPage.setItemMeta(nextPageMeta);

        ItemMeta previousPageMeta = previousPage.getItemMeta();
        previousPageMeta.setDisplayName("§e<- Previous Page");
        previousPage.setItemMeta(previousPageMeta);

        ItemMeta followerToggleEnabledMeta = followerToggleEnabled.getItemMeta();
        followerToggleEnabledMeta.setDisplayName("§eFollower: §aEnabled");
        followerToggleEnabled.setItemMeta(followerToggleEnabledMeta);


        ItemMeta followerToggleDisabledMeta = followerToggleDisabled.getItemMeta();
        followerToggleDisabledMeta.setDisplayName("§eFollower: §cDisabled");
        followerToggleDisabled.setItemMeta(followerToggleDisabledMeta);
    }

    @EventHandler
    public void onItemClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();
        if (!openInvPlayerSet.contains(playerUUID)) return;
        event.setCancelled(true);
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;
        int page = getPageNum(clickedInv);
        if (clickedInv.getType() != InventoryType.CHEST) return;
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;
        NamespacedKey pageNumKey = new NamespacedKey(plugin, "page");
        if (clickedItem.isSimilar(noFollowers) || clickedItem.getItemMeta().getPersistentDataContainer().has(pageNumKey, PersistentDataType.INTEGER)) return;
        else if (clickedItem.isSimilar(followerToggleEnabled) || clickedItem.isSimilar(followerToggleDisabled)) {
            FollowerUser followerUser = ESFollowers.dataManager.getFollowerUser(player.getUniqueId());
            if (!followerUser.isFollowerEnabled()) {
                String followerName = followerUser.getFollower();
                if (!playerFollowerMap.containsKey(playerUUID)) new FollowerEntity(player, followerName);
            } else {
                FollowerEntity followerEntity = playerFollowerMap.get(playerUUID);
                followerEntity.disable();
            }
            FollowerGUI followerInv = new FollowerGUI(player, page, openInvPlayerSet);
            followerInv.openInventory(player);
            return;
        } else if (clickedItem.isSimilar(nextPage)) {
            FollowerGUI followerInv = new FollowerGUI(player, page + 1, openInvPlayerSet);
            followerInv.openInventory(player);
            return;
        } else if (clickedItem.isSimilar(previousPage)) {
            FollowerGUI followerInv = new FollowerGUI(player, page - 1, openInvPlayerSet);
            followerInv.openInventory(player);
            return;
        } else if (clickedItem.getType() == Material.NAME_TAG && clickedItem.getItemMeta().getDisplayName().startsWith("§eFollower Name:")) {
            FollowerEntity followerEntity = ESFollowers.dataManager.getPlayerFollowerMap().get(player.getUniqueId());
            FollowerUser followerUser = ESFollowers.dataManager.getFollowerUser(player.getUniqueId());
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                followerEntity.setDisplayNameVisible(!followerUser.isDisplayNameEnabled());
                FollowerGUI followerInv = new FollowerGUI(player, page, openInvPlayerSet);
                followerInv.openInventory(player);
                return;
            }
            SignMenuFactory.Menu menu = signMenuFactory.newMenu(Arrays.asList("", "^^^^^^^^^^^", "Enter a name", "for the Follower"))
                .reopenIfFail(true)
                .response((thisPlayer, strings) -> {
                    if (strings[0].equals("")) strings[0] = " ";
                    followerEntity.setDisplayName(strings[0]);
                    return true;
                });
            player.closeInventory();
            menu.open(player);
            return;
        }
        String followerName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        FollowerGUI followerInv = new FollowerGUI(player, page, openInvPlayerSet);
        followerInv.openInventory(player);
        if (playerFollowerMap.containsKey(player.getUniqueId())) {
            FollowerEntity followerEntity = playerFollowerMap.get(player.getUniqueId());
            followerEntity.setFollower(followerName);
            return;
        }
        new FollowerEntity(player, followerName);
        player.sendMessage(ESFollowers.configManager.getPrefix() + "§aFollower Spawned.");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        new BukkitRunnable() {
            public void run() {
                if (event.getPlayer().getOpenInventory().getType() != InventoryType.CHEST) {
                    UUID playerUUID = event.getPlayer().getUniqueId();
                    if (!openInvPlayerSet.contains(playerUUID)) return;
                    openInvPlayerSet.remove(playerUUID);
                }
            }
        }.runTaskLater(plugin, 1);
    }

    private int getPageNum(Inventory inventory) {
        NamespacedKey pageNumKey = new NamespacedKey(plugin, "page");
        ItemStack item = inventory.getItem(0);
        if (item == null) return 0;
        ItemMeta itemMeta = item.getItemMeta();
        return itemMeta.getPersistentDataContainer().get(pageNumKey, PersistentDataType.INTEGER);
    }
}
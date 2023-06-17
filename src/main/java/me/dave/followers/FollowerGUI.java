package me.dave.followers;

import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import me.dave.followers.data.FollowerUser;

import java.util.*;

public class FollowerGUI {
    private final Inventory inventory;
    private final HashSet<UUID> openInvPlayerSet;

    public FollowerGUI(Player player, int page, HashSet<UUID> playerSet) {
        this.openInvPlayerSet = playerSet;
        inventory = Bukkit.createInventory(null, 54, ChatColorHandler.translateAlternateColorCodes(Followers.configManager.getGuiTitle()));
        ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta emptyMeta = empty.getItemMeta();
        NamespacedKey pageNumKey = new NamespacedKey(Followers.getInstance(), "page");
        emptyMeta.getPersistentDataContainer().set(pageNumKey, PersistentDataType.INTEGER, page);
        emptyMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes("&r"));
        empty.setItemMeta(emptyMeta);
        for (int i = 0; i < 18; i++) {
            if (i <= 8) inventory.setItem(i, empty);
            else inventory.setItem(i + 36, empty);
        }
        List<String> followerSet = new ArrayList<>();
        for (String followerName : Followers.followerManager.getFollowerNames()) {
            if (!player.hasPermission("followers." + followerName.toLowerCase().replaceAll(" ", "_"))) continue;
            followerSet.add(followerName);
        }
        int setStartPos = (page - 1) * 36;
        for (int i = 0; i < 36; i++, setStartPos++) {
            if (setStartPos >= followerSet.size() || followerSet.isEmpty()) break;
            String followerName = followerSet.get(setStartPos);
            ItemStack headItem = Followers.followerManager.getFollower(followerName).getHead();
            if (headItem == null || headItem.getType() == Material.AIR) headItem = new ItemStack(Material.ARMOR_STAND);
            ItemMeta headItemMeta = headItem.getItemMeta();
            headItemMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(Followers.configManager.getGuiFollowerFormat().replaceAll("%follower%", followerName)));
            headItem.setItemMeta(headItemMeta);
            inventory.setItem(i + 9, headItem);
        }
        FollowerUser followerUser = Followers.dataManager.getFollowerUser(player.getUniqueId());
        if (!followerSet.isEmpty()) {
            ItemStack followerToggle;
            if (followerUser.isFollowerEnabled()) followerToggle = Followers.configManager.getGuiItem("follower-toggle.enabled", Material.LIME_WOOL);
            else followerToggle = Followers.configManager.getGuiItem("follower-toggle.disabled", Material.RED_WOOL);
            inventory.setItem(49, followerToggle);
        } else {
            ItemStack noFollowers = Followers.configManager.getGuiItem("no-followers", Material.BARRIER);
            inventory.setItem(22, noFollowers);
        }

        if (followerSet.size() > page * 36) {
            ItemStack nextPage = Followers.configManager.getGuiItem("next-page", Material.ARROW);
            inventory.setItem(50, nextPage);
        }
        if (page > 1) {
            ItemStack previousPage = Followers.configManager.getGuiItem("previous-page", Material.ARROW);
            inventory.setItem(48, previousPage);
        }
        if (player.hasPermission("follower.name")) {
            ItemStack followerName;
            if (followerUser.isDisplayNameEnabled()) followerName = Followers.configManager.getGuiItem("nickname.shown", Material.NAME_TAG);
            else followerName = Followers.configManager.getGuiItem("nickname.hidden", Material.NAME_TAG);

            ItemMeta itemMeta = followerName.getItemMeta();
            itemMeta.setDisplayName(itemMeta.getDisplayName().replaceAll("%nickname%", followerUser.getDisplayName()));
            followerName.setItemMeta(itemMeta);

            inventory.setItem(45, followerName);
        }
        if (player.hasPermission("follower.random")) {
            ItemStack followerName;
            if (followerUser.isRandomType()) followerName = Followers.configManager.getGuiItem("random.enabled", Material.CONDUIT);
            else followerName = Followers.configManager.getGuiItem("random.disabled", Material.CONDUIT);
            inventory.setItem(46, followerName);
        }
    }

    public void openInventory(Player player) {
        openInvPlayerSet.add(player.getUniqueId());
        player.openInventory(inventory);
    }
}
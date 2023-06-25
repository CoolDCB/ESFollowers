package me.dave.followers.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.dave.followers.Followers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.UUID;

public class SkullCreator {
    private static Method skullMetaSetProfileMethod;
    private static Field skullMetaProfileField;

    public static ItemStack getPlayerSkull(UUID uuid) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getCustomSkull(String texture) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta meta) {
            mutateItemMeta(meta, texture);
            item.setItemMeta(meta);
            return item;
        } else {
            return null;
        }
    }

    public static String getB64(ItemStack itemStack) {
        try {
            if (itemStack.getType() == Material.PLAYER_HEAD && itemStack.hasItemMeta()) {
                SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
                if (skullMetaProfileField == null) {
                    skullMetaProfileField = skullMeta.getClass().getDeclaredField("profile");
                    skullMetaProfileField.setAccessible(true);
                }
                GameProfile gameProfile = (GameProfile) skullMetaProfileField.get(skullMeta);
                Iterator<Property> iterator = gameProfile.getProperties().get("textures").iterator();
                if (iterator.hasNext()) {
                    Property property = iterator.next();
                    return property.getValue();
                }
            }
            return "";
        } catch (Exception exception) {
            return "";
        }
    }

    private static void mutateItemMeta(SkullMeta meta, String b64) {
        try {
            if (skullMetaSetProfileMethod == null) {
                skullMetaSetProfileMethod = meta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
                skullMetaSetProfileMethod.setAccessible(true);
            }

            skullMetaSetProfileMethod.invoke(meta, makeProfile(b64));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException var5) {
            try {
                if (skullMetaProfileField == null) {
                    skullMetaProfileField = meta.getClass().getDeclaredField("profile");
                    skullMetaProfileField.setAccessible(true);
                }

                skullMetaProfileField.set(meta, makeProfile(b64));
            } catch (IllegalAccessException | NoSuchFieldException var4) {
                var4.printStackTrace();
            }
        }

    }

    private static GameProfile makeProfile(String b64) {
        UUID id = null;
        try {
            id = new UUID(b64.substring(b64.length() - 20).hashCode(), b64.substring(b64.length() - 10).hashCode());
        } catch (StringIndexOutOfBoundsException ex) {
            if (b64.length() == 0)
                Followers.getInstance().getLogger().warning("Missing base64 texture found - check your config");
            else
                Followers.getInstance().getLogger().warning("Invalid base64 texture (" + b64 + ") found - check your config");
        }
        GameProfile profile = new GameProfile(id, "Player");
        profile.getProperties().put("textures", new Property("textures", b64));
        return profile;
    }
}

package com.willfp.ecoenchants.display;

import com.google.common.collect.Lists;
import com.willfp.ecoenchants.EcoEnchantsPlugin;
import com.willfp.ecoenchants.config.ConfigManager;
import com.willfp.ecoenchants.enchantments.EcoEnchant;
import com.willfp.ecoenchants.enchantments.EcoEnchants;
import com.willfp.ecoenchants.enchantments.meta.EnchantmentRarity;
import com.willfp.ecoenchants.enchantments.meta.EnchantmentTarget;
import com.willfp.ecoenchants.util.Logger;
import com.willfp.ecoenchants.util.NumberUtils;
import com.willfp.ecoenchants.util.Pair;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * All methods and fields pertaining to showing players the enchantments on their items.
 */
public final class EnchantDisplay {

    /**
     * The meta key of the length of enchantments in lore (for legacy support)
     * @deprecated This is no longer used due to a change in the lore storage mechanism
     */
    @Deprecated
    public static final NamespacedKey KEY = new NamespacedKey(EcoEnchantsPlugin.getInstance(), "ecoenchantlore-len");

    /**
     * The meta key to hide enchantments in lore
     *
     * Only used for parity in {@link com.willfp.ecoenchants.display.packets.PacketSetCreativeSlot}.
     * More robust method to be introduced
     *
     * @deprecated Temporary fix
     */
    @Deprecated
    public static final NamespacedKey KEY_SKIP = new NamespacedKey(EcoEnchantsPlugin.getInstance(), "ecoenchantlore-skip");

    /**
     * The meta key to notify the server that an item is from a villager trade
     *
     * Bit of a bodge - plan on making it better.
     */
    public static final NamespacedKey KEY_V = new NamespacedKey(EcoEnchantsPlugin.getInstance(), "ecoenchantlore-v");

    /**
     * Cached enchantment descriptions and names
     */
    public static final Map<Enchantment, Pair<String, List<String>>> CACHE = new HashMap<>();

    private static final String prefix = "§w";

    private static String normalColor;
    private static String curseColor;
    private static String specialColor;
    private static String artifactColor;
    public static String descriptionColor;

    private static int numbersThreshold;
    private static boolean useNumerals;

    private static int describeThreshold;
    private static boolean useDescribe;

    private static int shrinkThreshold;
    private static int shrinkPerLine;
    private static boolean useShrink;

    /**
     * Update config values
     */
    public static void update() {
        descriptionColor = ChatColor.translateAlternateColorCodes('&', ConfigManager.getLang().getString("description-color"));
        curseColor = ChatColor.translateAlternateColorCodes('&', ConfigManager.getLang().getString("curse-color"));
        specialColor = ChatColor.translateAlternateColorCodes('&', ConfigManager.getLang().getString("special-color"));
        artifactColor = ChatColor.translateAlternateColorCodes('&', ConfigManager.getLang().getString("artifact-color"));
        normalColor = ChatColor.translateAlternateColorCodes('&', ConfigManager.getLang().getString("not-curse-color"));

        CACHE.clear();
        Arrays.asList(Enchantment.values()).parallelStream().forEach(enchantment -> {
            String name;
            String color;
            EcoEnchant.EnchantmentType type;
            List<String> description;
            if(EcoEnchants.getFromEnchantment(enchantment) != null) {
                EcoEnchant ecoEnchant = EcoEnchants.getFromEnchantment(enchantment);
                description = ecoEnchant.getDescription();
                name = ecoEnchant.getName();
                type = ecoEnchant.getType();
            } else {
                description = Arrays.asList(
                        WordUtils.wrap(
                                String.valueOf(ConfigManager.getLang().getString("enchantments." + enchantment.getKey().getKey().toLowerCase() + ".description")),
                                ConfigManager.getConfig().getInt("lore.describe.wrap"),
                                "\n", false
                        ).split("\\r?\\n")
                );
                name = String.valueOf(ConfigManager.getLang().getString("enchantments." + enchantment.getKey().getKey().toLowerCase() + ".name"));
                type = enchantment.isCursed() ? EcoEnchant.EnchantmentType.CURSE : EcoEnchant.EnchantmentType.NORMAL;
            }

            switch(type) {
                case ARTIFACT:
                    color = artifactColor;
                    break;
                case SPECIAL:
                    color = specialColor;
                    break;
                case CURSE:
                    color = curseColor;
                    break;
                default:
                    color = normalColor;
                    break;
            }

            if(EcoEnchants.getFromEnchantment(enchantment) != null) {
                EnchantmentRarity rarity = EcoEnchants.getFromEnchantment(enchantment).getRarity();
                if(rarity != null) {
                    if (rarity.hasCustomColor() && type != EcoEnchant.EnchantmentType.CURSE) {
                        color = rarity.getCustomColor();
                    }
                } else {
                    Logger.warn("Enchantment " + enchantment.getKey().getKey() + " has an invalid rarity");
                }
            }

            name = color + name;
            description.replaceAll(line -> prefix + descriptionColor + line);
            CACHE.put(enchantment, new Pair<>(name, description));
        });

        useNumerals = ConfigManager.getConfig().getBool("lore.use-numerals");
        numbersThreshold = ConfigManager.getConfig().getInt("lore.use-numbers-above-threshold");

        describeThreshold = ConfigManager.getConfig().getInt("lore.describe.before-lines");
        useDescribe = ConfigManager.getConfig().getBool("lore.describe.enabled");

        shrinkThreshold = ConfigManager.getConfig().getInt("lore.shrink.after-lines");
        useShrink = ConfigManager.getConfig().getBool("lore.shrink.enabled");
        shrinkPerLine = ConfigManager.getConfig().getInt("lore.shrink.maximum-per-line");
    }

    /**
     * Bodge to fix hidden enchantments from villagers.
     *
     * It isn't recommended to mess with this unless you <b>really</b> know your way around EcoEnchants.
     *
     * @param item The item to modify
     * @return The item, with KEY_V
     */
    public static ItemStack addV(ItemStack item) {
        if(item == null || item.getItemMeta() == null) return item;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_V, PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Revert display
     * @param item The item to revert
     * @return The item, updated
     */
    public static ItemStack revertDisplay(ItemStack item) {
        if(item == null || !EnchantmentTarget.ALL.getMaterials().contains(item.getType()) || item.getItemMeta() == null) return item;

        ItemMeta meta = item.getItemMeta();
        List<String> itemLore;

        if(meta.hasLore())
            itemLore = meta.getLore();
        else
            itemLore = new ArrayList<>();

        if(itemLore == null) itemLore = new ArrayList<>();

        try {
            if(meta.getPersistentDataContainer().has(KEY, PersistentDataType.INTEGER)) {
                int enchantLoreLength = meta.getPersistentDataContainer().get(KEY, PersistentDataType.INTEGER);
                if(itemLore.size() >= enchantLoreLength)
                    itemLore.subList(0, enchantLoreLength).clear();
            }
        } catch(NullPointerException ignored) { }

        meta.getPersistentDataContainer().remove(KEY);
        itemLore.removeIf((s) -> s.startsWith(prefix));

        if(!meta.getPersistentDataContainer().has(KEY_SKIP, PersistentDataType.INTEGER)) {
            if (meta instanceof EnchantmentStorageMeta)
                meta.removeItemFlags(ItemFlag.HIDE_POTION_EFFECTS); // Thanks ShaneBee!
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.getPersistentDataContainer().remove(KEY_SKIP);
        meta.setLore(itemLore);
        item.setItemMeta(meta);

        return item;
    }

    public static ItemStack displayEnchantments(ItemStack item) {
        return displayEnchantments(item, false);
    }

    /**
     * Show all enchantments in item lore
     * @param item The item to update
     * @return The item, updated
     */
    public static ItemStack displayEnchantments(ItemStack item, boolean hideEnchants) {
        if(item == null || item.getItemMeta() == null || !EnchantmentTarget.ALL.getMaterials().contains(item.getType()))
            return item;

        if(hideEnchants && item.getItemMeta().getPersistentDataContainer().has(KEY_V, PersistentDataType.INTEGER)) {
            hideEnchants = false;
            item.getItemMeta().getPersistentDataContainer().remove(KEY_V);
        }

        item = revertDisplay(item);

        ItemMeta meta = item.getItemMeta();
        if(meta == null) return item;
        List<String> itemLore = new ArrayList<>();

        if(hideEnchants || meta.getPersistentDataContainer().has(KEY_SKIP, PersistentDataType.INTEGER)) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            meta.getPersistentDataContainer().set(KEY_SKIP, PersistentDataType.INTEGER, 1);
            item.setItemMeta(meta);
            return item;
        }

        if(meta.hasLore())
            itemLore = meta.getLore();

        if(itemLore == null) itemLore = new ArrayList<>();

        List<String> lore = new ArrayList<>();

        Map<Enchantment, Integer> enchantments;
        List<Enchantment> forRemoval = new ArrayList<>();

        if(meta instanceof EnchantmentStorageMeta) {
            enchantments = ((EnchantmentStorageMeta) meta).getStoredEnchants();
        } else {
            enchantments = meta.getEnchants();
        }

        final ItemStack finalItem = item;
        enchantments.forEach((enchantment, level) -> {
            boolean isEcoEnchant = EcoEnchants.getFromEnchantment(enchantment) != null;

            if(CACHE.get(enchantment) == null) return;

            String name = CACHE.get(enchantment).getKey();

            if(isEcoEnchant) {
                if(!EcoEnchants.getFromEnchantment(enchantment).isEnabled()) forRemoval.add(enchantment);
            }

            if(!(enchantment.getMaxLevel() == 1 && level == 1)) {
                if(useNumerals && finalItem.getEnchantmentLevel(enchantment) < numbersThreshold) {
                    name += " " + NumberUtils.toNumeral(level);
                } else {
                    name += " " + level;
                }
            }

            lore.add(prefix + name);
            if(enchantments.size() <= describeThreshold && useDescribe)
                lore.addAll(CACHE.get(enchantment).getValue());
        });

        if (useShrink && (enchantments.size() > shrinkThreshold)) {
            List<List<String>> partitionedCombinedLoreList = Lists.partition(lore, shrinkPerLine);
            List<String> newLore = new ArrayList<>();
            partitionedCombinedLoreList.forEach((list) -> {
                StringBuilder builder = new StringBuilder();
                for(String s : list) {
                    builder.append(s);
                    builder.append(", ");
                }
                String line = builder.toString();
                line = line.substring(0, line.length() - 2);
                newLore.add(line);
            });
            lore.clear();
            lore.addAll(newLore);
        }

        if (meta instanceof EnchantmentStorageMeta) {
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            forRemoval.forEach(((EnchantmentStorageMeta) meta)::removeStoredEnchant);
        } else {
            forRemoval.forEach(meta::removeEnchant);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        lore.addAll(itemLore);
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }
}

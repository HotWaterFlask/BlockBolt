package top.hotwaterflask.blockbolt.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

import top.hotwaterflask.blockbolt.AttackType;
import top.hotwaterflask.blockbolt.ProtectionType;
import top.hotwaterflask.blockbolt.impl.updater.UpdatePreference;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

final class Config {

    private final static class Key {
        private final static String LANGUAGE_FILE = "languageFile",
                PROTECTABLE_STANDARD = "protectableStandard",
                PROTECTABLE_CONTAINERS = "protectableContainers",
                PROTECTABLE_DOORS = "protectableDoors",
                PROTECTABLE_TRAP_DOORS = "protectableTrapDoors",
                PROTECTABLE_ATTACHABLES = "protectableAttachables",
                DEFAULT_DOOR_OPEN_SECONDS = "defaultDoorOpenSeconds",
                UPDATER = "updater",
                CHAIN_STANDARD_BLOCKS = "chainStandardBlocks",
                CHAIN_DOORS = "chainDoors",
                CHAIN_ATTACHABLES = "chainAttachables",
                GROUP_CONTAINERS_VERTICALLY = "groupContainersVertically",
                GROUP_FURNACES = "groupFurnaces",
                GROUP_DISPENSERS = "groupDispensers",
                GROUP_CAULDRONS = "groupCauldrons",
                GROUP_ENCHANTMENT_TABLES = "groupEnchantmentTables",
                GROUP_BREWING_STANDS = "groupBrewingStands",
                AUTO_EXPIRE_DAYS = "autoExpireDays",
                ALLOW_DESTROY_BY = "allowDestroyBy",
                CONFIG_VERSION = "configVersion";
    }

    static final String DEFAULT_TRANSLATIONS_FILE = "translations-zh.yml";

    private final Set<AttackType> allowDestroyBy;
    private final int autoExpireDays;
    private final boolean chainStandardBlocks;
    private final boolean chainDoors;
    private final boolean chainAttachables;
    private final boolean groupContainersVertically;
    private final boolean groupFurnaces;
    private final boolean groupDispensers;
    private final boolean groupCauldrons;
    private final boolean groupEnchantmentTables;
    private final boolean groupBrewingStands;
    private final int defaultDoorOpenSeconds;
    private final String languageFile;
    private final Logger logger;
    private final Map<ProtectionType, MaterialSet> protectableMaterialsMap;
    /**
     * Combination of the sets of all individual protection types.
     */
    private final Set<Material> protectableMaterialsSet;
    private final UpdatePreference updatePreference;

    Config(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        logger = plugin.getLogger();

        languageFile = config.getString(Key.LANGUAGE_FILE, DEFAULT_TRANSLATIONS_FILE);
        defaultDoorOpenSeconds = config.getInt(Key.DEFAULT_DOOR_OPEN_SECONDS, 0);
        updatePreference = readUpdatePreference(config.getString(Key.UPDATER, "disabled"));
        chainStandardBlocks = config.getBoolean(Key.CHAIN_STANDARD_BLOCKS, true);
        chainDoors = config.getBoolean(Key.CHAIN_DOORS, true);
        chainAttachables = config.getBoolean(Key.CHAIN_ATTACHABLES, true);
        groupContainersVertically = config.getBoolean(Key.GROUP_CONTAINERS_VERTICALLY, true);
        groupFurnaces = config.getBoolean(Key.GROUP_FURNACES, true);
        groupDispensers = config.getBoolean(Key.GROUP_DISPENSERS, true);
        groupCauldrons = config.getBoolean(Key.GROUP_CAULDRONS, true);
        groupEnchantmentTables = config.getBoolean(Key.GROUP_ENCHANTMENT_TABLES, true);
        groupBrewingStands = config.getBoolean(Key.GROUP_BREWING_STANDS, true);
        autoExpireDays = config.getInt(Key.AUTO_EXPIRE_DAYS);
        allowDestroyBy = readAttackTypeSet(config.getStringList(Key.ALLOW_DESTROY_BY));

        // Materials
        protectableMaterialsMap = new EnumMap<>(ProtectionType.class);
        List<String> standardList = config.contains(Key.PROTECTABLE_STANDARD)
                ? config.getStringList(Key.PROTECTABLE_STANDARD)
                : config.getStringList(Key.PROTECTABLE_CONTAINERS);
        protectableMaterialsMap.put(ProtectionType.CONTAINER, readMaterialSet(standardList));
        protectableMaterialsMap.put(ProtectionType.DOOR, readMaterialSet(config.getStringList(Key.PROTECTABLE_DOORS)));
        if (config.contains(Key.PROTECTABLE_TRAP_DOORS)) {
            // Still support old name:
            protectableMaterialsMap.put(ProtectionType.ATTACHABLE,
                    readMaterialSet(config.getStringList(Key.PROTECTABLE_TRAP_DOORS)));
        } else {
            protectableMaterialsMap.put(ProtectionType.ATTACHABLE,
                    readMaterialSet(config.getStringList(Key.PROTECTABLE_ATTACHABLES)));
        }

        // Create combined set
        protectableMaterialsSet = new HashSet<>();
        for (MaterialSet protectableByType : protectableMaterialsMap.values()) {
            protectableMaterialsSet.addAll(protectableByType.getAllFlattened());
        }

        // Config upgrades
        int version = config.getInt(Key.CONFIG_VERSION, 1);
        if (version < 2) {
            logger.info("Upgrading configuration...");
            // We load the default configuration, apply our settings, and then save it
            try (InputStream configStream = Objects.requireNonNull(plugin.getResource("config.yml"))) {
                config = YamlConfiguration.loadConfiguration(new InputStreamReader(configStream, StandardCharsets.UTF_8));
                writeToConfig(config);
                config.save(new File(plugin.getDataFolder(), "config.yml"));
                plugin.reloadConfig();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to read default config", e);
            }
        }
    }

    /**
     * Writes out all the current settings to a configuration.
     * @param config The config to write to.
     */
    private void writeToConfig(FileConfiguration config) {
        config.set(Key.LANGUAGE_FILE, this.languageFile);
        config.set(Key.DEFAULT_DOOR_OPEN_SECONDS, this.defaultDoorOpenSeconds);
        config.set(Key.UPDATER, this.updatePreference.toString());
        config.set(Key.GROUP_CONTAINERS_VERTICALLY, this.groupContainersVertically);
        config.set(Key.GROUP_FURNACES, this.groupFurnaces);
        config.set(Key.GROUP_DISPENSERS, this.groupDispensers);
        config.set(Key.GROUP_CAULDRONS, this.groupCauldrons);
        config.set(Key.GROUP_ENCHANTMENT_TABLES, this.groupEnchantmentTables);
        config.set(Key.GROUP_BREWING_STANDS, this.groupBrewingStands);
        config.set(Key.AUTO_EXPIRE_DAYS, this.autoExpireDays);
        config.set(Key.ALLOW_DESTROY_BY, this.allowDestroyBy.stream().map(AttackType::toString).toList());
        config.set(Key.PROTECTABLE_CONTAINERS, writeMaterialSet(protectableMaterialsMap.get(ProtectionType.CONTAINER)));
        config.set(Key.PROTECTABLE_DOORS, writeMaterialSet(protectableMaterialsMap.get(ProtectionType.DOOR)));
        config.set(Key.PROTECTABLE_ATTACHABLES, writeMaterialSet(protectableMaterialsMap.get(ProtectionType.ATTACHABLE)));
    }

    /**
     * Writes a material set to a string list, suitable for the configuration.
     * @param materials The materials.
     * @return The material list. Will be empty if {@code materials} is null.
     */
    private List<String> writeMaterialSet(@Nullable MaterialSet materials) {
        if (materials == null) {
            return Collections.emptyList();
        }
        return materials.toConfigStringList();
    }

    /**
     * Gets whether the given attack type can destroy protections.
     *
     * @param attackType
     *            The attack type.
     * @return True if the attack type can destroy protections, false otherwise.
     */
    boolean allowDestroyBy(AttackType attackType) {
        return allowDestroyBy.contains(attackType);
    }

    /**
     * Gets whether the material can be protected by any type.
     *
     * @param block
     *            Block to check.
     * @return True if the material can be protected by the given type, false
     *         otherwise.
     */
    boolean canProtect(Block block) {
        return protectableMaterialsSet.contains(block.getType());
    }

    /**
     * Gets whether the material can be protected by the given type.
     *
     * @param type
     *            Protection type that must be checked for being able to protect
     *            this type.
     * @param block
     *            Block to check.
     *
     * @return True if the material can be protected by the given type, false
     *         otherwise.
     */
    boolean canProtect(ProtectionType type, Block block) {
        MaterialSet materials = this.protectableMaterialsMap.get(type);
        if (materials == null) {
            return false;
        }
        return materials.contains(block.getType());
    }

    /**
     * Gets the amount of days that a chest owner has to be offline for a chest
     * to expire. 0 or negative means that chests never expire.
     *
     * @return The amount of days.
     */
    int getAutoExpireDays() {
        return autoExpireDays;
    }

    /**
     * Gets whether containers of the given type should be grouped together when
     * connected (Deadbolt-style chaining). One sign will then protect all
     * connected blocks.
     *
     * @param material
     *            The material of the container.
     * @return True if the material should be grouped, false otherwise.
     */
    boolean connectsContainers(Material material) {
        if (material == Material.FURNACE || material == Material.BLAST_FURNACE
                || material == Material.SMOKER) {
            return this.groupFurnaces;
        }
        if (material == Material.DISPENSER) {
            return this.groupDispensers;
        }
        if (material == Material.BREWING_STAND) {
            return this.groupBrewingStands;
        }
        if (material == Material.ENCHANTING_TABLE) {
            return this.groupEnchantmentTables;
        }
        if (material == Material.CAULDRON) {
            return this.groupCauldrons;
        }
        if (material == Material.CHEST || material == Material.TRAPPED_CHEST
                || material.name().endsWith("_CHEST")) {
            return true;
        }
        return false;
    }

    /**
     * Gets whether containers of the same type should be grouped vertically
     * (stacked on top of each other). For chests this follows
     * {@code groupContainersVertically}; the other groupable containers
     * (furnaces, dispensers, etc.) always chain vertically when enabled.
     *
     * @param material
     *            The material of the container.
     * @return True if vertical grouping is enabled, false otherwise.
     */
    boolean connectsContainersVertically(Material material) {
        if (material == Material.CHEST || material == Material.TRAPPED_CHEST
                || material.name().endsWith("_CHEST")) {
            return this.groupContainersVertically;
        }
        return connectsContainers(material);
    }

    /**
     * Gets the default amount of ticks a door stays open before automatically
     * closing. 0 or negative values will make the door never close
     * automatically.
     *
     * @return The default amount of ticks.
     */
    int getDefaultDoorOpenSeconds() {
        return defaultDoorOpenSeconds;
    }

    boolean isChainStandardBlocks() {
        return chainStandardBlocks;
    }

    boolean isChainDoors() {
        return chainDoors;
    }

    boolean isChainAttachables() {
        return chainAttachables;
    }

    /**
     * Gets the file name of the selected language.
     *
     * @return The language.
     */
    String getLanguageFileName() {
        return languageFile;
    }

    /**
     * Gets the update preference.
     *
     * @return The update preference.
     */
    UpdatePreference getUpdatePreference() {
        return updatePreference;
    }

    private Set<AttackType> readAttackTypeSet(List<String> strings) {
        Set<AttackType> materials = EnumSet.noneOf(AttackType.class);
        for (String string : strings) {
            try {
                materials.add(AttackType.valueOf(string.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                logger.warning("Cannot recognize attack type '" + string + "', ignoring it");
                continue;
            }
        }
        return materials;
    }

    /**
     * Transforms the string collection into a material set, by parsing each
     * string using {@link Material#matchMaterial(String)}. Strings starting
     * with {@code #} are treated as block tags, so you can add an entire group
     * of blocks at once (for example {@code #minecraft:wooden_doors}). The
     * resulting set will be mutable. All strings that cannot be parsed are
     * logged and then ignored.
     *
     * @param strings
     *            The string collection.
     * @return The material set.
     */
    private MaterialSet readMaterialSet(Collection<String> strings) {
        MaterialSet materialSet = new MaterialSet();
        for (String string : strings) {
            if (string.startsWith("#")) {
                // 方块标签写法，例如 "#minecraft:wooden_doors"
                NamespacedKey key = NamespacedKey.fromString(string.substring(1));
                if (key == null) {
                    logger.warning("Cannot parse tag '" + string + "', ignoring it");
                    continue;
                }
                Tag<Material> tag = Bukkit.getTag("blocks", key, Material.class);
                if (tag == null) {
                    logger.warning("Cannot recognize tag '" + string + "', ignoring it");
                    continue;
                }
                materialSet.addTag(tag);
                continue;
            }
            Material material = Material.matchMaterial(string);
            if (material == null) {
                material = Material.matchMaterial(string, true);
            }
            if (material == null) {
                logger.warning("Cannot recognize material '" + string + "', ignoring it");
                continue;
            }
            materialSet.addMaterial(material);
        }
        return materialSet;
    }

    private UpdatePreference readUpdatePreference(@Nullable String string) {
        if (string == null) {
            return UpdatePreference.DISABLED;
        }
        Optional<UpdatePreference> updatePreference = UpdatePreference.parse(string);
        if (updatePreference.isPresent()) {
            return updatePreference.get();
        } else {
            logger.warning("Unknown update setting: " + string + ". Disabling automatic updater.");
            return UpdatePreference.DISABLED;
        }
    }
}

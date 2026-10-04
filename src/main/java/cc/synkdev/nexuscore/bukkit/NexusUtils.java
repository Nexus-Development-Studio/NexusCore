package cc.synkdev.nexuscore.bukkit;

import cc.synkdev.nexuscore.components.NexusPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class NexusUtils {
    private NexusUtils() {
        /* This utility class should not be instantiated */
    }

    //New class for subplugins methods
    public static void initLang(NexusPlugin plugin, Map<String, String> langMap, String lang) {
        langMap.clear();
        langMap.putAll(Lang.init(plugin, new File(((Plugin) plugin).getDataFolder(), "lang.json"), lang));
    }

    public static YamlConfiguration updateConfig(Plugin plugin, String... skipKeys) {
        YamlConfiguration config;
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            try {
                throw new IOException("Failed to create plugin data folder!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            if (!configFile.exists()) {
                try {
                    Files.copy(Objects.requireNonNull(plugin.getResource("config.yml")), configFile.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                File temp = new File(plugin.getDataFolder(), "temp-config-"+System.currentTimeMillis()+".yml");
                try {
                    Files.copy(Objects.requireNonNull(plugin.getResource("config.yml")), temp.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                FileConfiguration tempConfig = YamlConfiguration.loadConfiguration(temp);
                config = YamlConfiguration.loadConfiguration(configFile);
                boolean changed = false;
                for (String key : tempConfig.getKeys(true)) {
                    if (!config.contains(key) && Arrays.stream(skipKeys).noneMatch(key::startsWith)) {
                        config.set(key, tempConfig.get(key));
                        changed = true;
                    }
                }

                if (changed) {
                    config.save(configFile);
                }

                Files.delete(temp.toPath());
            }
            return YamlConfiguration.loadConfiguration(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

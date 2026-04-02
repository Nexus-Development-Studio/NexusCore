package cc.synkdev.nexusCore.bukkit;

import cc.synkdev.nexusCore.bukkit.commands.NcCmd;
import cc.synkdev.nexusCore.bukkit.commands.ReportCmd;
import cc.synkdev.nexusCore.bukkit.objects.AnalyticsReport;
import cc.synkdev.nexusCore.components.NexusPlugin;
import cc.synkdev.nexusCore.components.folia.NexusScheduler;
import cc.synkdev.nexusCore.components.folia.Platform;
import cc.synkdev.nexusCore.components.PluginUpdate;
import co.aikar.commands.BukkitCommandManager;
import lombok.Getter;
import lombok.Setter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class NexusCore extends JavaPlugin implements NexusPlugin {
    @Getter private static NexusCore instance;
    @Setter String prefix = ChatColor.translateAlternateColorCodes('&', "&8[&6NexusCore&8] » &r");
    @Setter @Getter static NexusPlugin pl = null;
    public static Map<NexusPlugin, String> availableUpdates = new HashMap<>();
    private final File configFile = new File(getDataFolder(), "config.yml");
    public FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    public static String lang = "en";
    @Getter @Setter private static Boolean loopReport = false;
    public static Map<String, String> langMap = new HashMap<>();
    public List<PluginUpdate> outdated = new ArrayList<>();
    public Boolean doAnalytics = true;
    public Boolean doAutoUpdate = true;
    public UUID serverUUID;
    public AnalyticsReport report;
    public List<JavaPlugin> pls = new ArrayList<>();
    @Getter @Setter private List<String> plugins = new ArrayList<>();
    public Map<String, String> versions = new HashMap<>();

    @Override
    public void onLoad() {
        instance = this;
        setPl(this);
    }

    @Override
    public void onEnable() {
        loadConfig();
        loadAnalytics();

        langMap.clear();
        langMap.putAll(Lang.init(this, new File(getDataFolder(), "lang.json")));

        BukkitCommandManager pcm = new BukkitCommandManager(this);

        setPl(this);
        Metrics metrics = new Metrics(this, 23015);
        metrics.addCustomChart(new SimplePie("lang", () -> config.getString("lang", "en")));
        Bukkit.getPluginManager().registerEvents(new Utils(this), this);

        pcm.registerCommand(new ReportCmd(this));
        pcm.registerCommand(new NcCmd(this));

        NexusScheduler.runTaskTimer(this, () -> {
            outdated.clear();
            outdated.addAll(UpdateChecker.checkOutated());
            if (!outdated.isEmpty() && doAutoUpdate) UpdateChecker.update(outdated);
        }, 1L, 60 * 60 * 20L, true);

            /* if (doAnalytics) {
                NexusScheduler.runTaskTimer(this, Analytics::sendReport, 1L, 10 * 60 * 20L);
            }*/

        NexusScheduler.runTaskLater(this, () -> {
            Utils.log("&b──────────────────────────────────────────────────&r", false);
            Utils.log("&f  _   _  &b ____   &1 ____  &r", false);
            Utils.log("&f | \\ | | &b|  _ \\  &1/ ___| &r", false);
            Utils.log("&f |  \\| | &b| | | | &1\\___ \\ &r", false);
            Utils.log("&f | |\\  | &b| |_| | &1 ___) |&r", false);
            Utils.log("&f |_| \\_| &b|____/  &1|____/ &r", false);
            Utils.log("&f         &b        &1       &r", false);
            Utils.log("&b──────────────────────────────────────────────────&r", false);
            Utils.log("&b NexusCore v" + ver() + "&r", false);
            Utils.log("&b Running on " + Bukkit.getServer().getBukkitVersion(), false);
            Utils.log("&b NDS | Nexus Development Studios &r", false);
            Utils.log("&b You are currently using &e" + getPlugins().size() + " &bof our plugins" + (getPlugins().isEmpty() ? "" : ": &e" + String.join(", ", getPlugins())), false);
            Utils.log("&b Note: This plugin and all of the ones listed above have an auto update feature. Visit the NexusCore config to disable it.", false);
            Utils.log("&b Visit our Discord for support: https://discord.gg/KxPE2bK5Bu", false);
            Utils.log("&b──────────────────────────────────────────────────&r", false);
        }, 30L, true);

    }

    public void loadConfig() {
        try {
            File slFolder = new File(getDataFolder().getParentFile(), "SynkLibs");
            if (slFolder.exists()) {
                slFolder.renameTo(getDataFolder());
            }
            File slJar = new File(getDataFolder().getParentFile(), "SynkLibs.jar");
            if (slJar.exists()) slJar.renameTo(new File(getDataFolder().getParentFile(), "NexusCore.jar"));

            if (!configFile.getParentFile().exists()) configFile.getParentFile().mkdirs();
            if (!configFile.exists()) {
                configFile.createNewFile();
            }

            config = YamlConfiguration.loadConfiguration(configFile);
            config = Utils.loadWebConfig("https://synkdev.cc/storage/config-libs.php", configFile);
            lang = config.getString("lang");
            doAutoUpdate = config.getBoolean("autoupdate");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadAnalytics() {
        File analyticsFile = new File(getDataFolder(), "analytics.yml");

        try {
            if (!analyticsFile.exists()) {
                Files.copy(getResource("analytics.yml"), analyticsFile.toPath());
            }
            YamlConfiguration analyticsConfig = YamlConfiguration.loadConfiguration(analyticsFile);
            doAnalytics = analyticsConfig.getBoolean("agree");
            String uuid = analyticsConfig.getString("uuid");
            UUID uid;
            boolean changed = false;
            try {
                uid = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {
                uid = UUID.randomUUID();
                changed = true;
            }
            serverUUID = uid;
            if (changed) {
                analyticsConfig.set("uuid", serverUUID.toString());
                analyticsConfig.save(analyticsFile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        Analytics.sendReport();
    }

    @Override
    public String name() {
        return "NexusCore";
    }

    @Override
    public String ver() {
        return "1.11.1";
    }

    @Override
    public String dlLink() {
        return "https://modrinth.com/plugin/nexuscore";
    }

    @Override
    public String prefix() {
        return prefix;
    }

    @Override
    public String lang() {
        return "https://synkdev.cc/storage/translations/lang-pld/NexusCore/lang-core.json";
    }

    @Override
    public Map<String, String> langMap() {
        return langMap;
    }
}

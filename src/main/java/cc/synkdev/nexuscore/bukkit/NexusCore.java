package cc.synkdev.nexuscore.bukkit;

import cc.synkdev.nexuscore.bukkit.commands.NcCmd;
import cc.synkdev.nexuscore.bukkit.commands.ReportCmd;
import cc.synkdev.nexuscore.bukkit.objects.PluginData;
import cc.synkdev.nexuscore.components.NexusPlugin;
import cc.synkdev.nexuscore.components.folia.NexusScheduler;
import co.aikar.commands.BukkitCommandManager;
import dev.faststats.ErrorTracker;
import dev.faststats.bukkit.BukkitContext;
import dev.faststats.data.Metric;
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
import java.util.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class NexusCore extends JavaPlugin implements NexusPlugin {
    @Getter private static NexusCore instance;
    @Setter String prefix = ChatColor.translateAlternateColorCodes('&', "&8[&6NexusCore&8] » &r");
    @Setter @Getter static NexusPlugin pl = null;
    static final Map<NexusPlugin, String> availableUpdates = new HashMap<>();
    private final File configFile = new File(getDataFolder(), "config.yml");
    public FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    public static String language = "en";
    @Getter @Setter private static Boolean loopReport = false;
    private static final Map<String, String> localLangMap = new HashMap<>();
    public final List<PluginData> outdated = new ArrayList<>();
    public Boolean doAutoUpdate = true;
    @Getter @Setter private List<String> plugins = new ArrayList<>();
    public final Map<String, String> versions = new HashMap<>();
    public boolean debug = false;

    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware();
    public BukkitContext context;

    @Override
    public void onLoad() {
        instance = this;
        setPl(this);
    }

    @Override
    public void onEnable() {
        loadConfig();

        localLangMap.clear();
        localLangMap.putAll(Lang.init(this, new File(getDataFolder(), "lang.json"), language));

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

        context = new BukkitContext.Factory(this, "172604b0e1a3e0f025d351a50261f4ae")
                .errorTrackerService(ERROR_TRACKER)
                .metrics(factory -> factory.addMetric(Metric.bool("autoupdate", () -> doAutoUpdate))
                        .addMetric(Metric.string("lang", () -> config.getString("lang", "en")))
                        .addMetric(Metric.stringArray("nexus_plugins", () -> getPlugins().toArray(new String[0])))
                        .create())
                .create();
        context.ready();

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
            if (slFolder.exists() && !slFolder.renameTo(getDataFolder())) {
                    throw new IOException("Failed to rename SynkLibs folder to NexusCore!");
                }

            File slJar = new File(getDataFolder().getParentFile(), "SynkLibs.jar");
            if (slJar.exists()) {
                if (slJar.renameTo(new File(getDataFolder().getParentFile(), "NexusCore.jar"))) {
                    Utils.log("&bRenamed SynkLibs.jar to NexusCore.jar", true);
                } else {
                    Utils.log("&cFailed to rename SynkLibs.jar to NexusCore.jar", false);
                }
            }

            if (!configFile.getParentFile().exists()) configFile.getParentFile().mkdirs();
            if (!configFile.exists() && !configFile.createNewFile()) {
                    throw new IOException("Failed to create config.yml file!");
                }


            config = YamlConfiguration.loadConfiguration(configFile);
            config = Utils.loadWebConfig("https://synkdev.cc/storage/config-libs.php", configFile);
            language = config.getString("lang");
            doAutoUpdate = config.getBoolean("autoupdate");
            debug = config.contains("debug") && config.getBoolean("debug");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void onDisable() {
        context.shutdown();
    }

    @Override
    public String name() {
        return "NexusCore";
    }

    @Override
    public String ver() {
        return "2.1.6.1";
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
        return localLangMap;
    }
}

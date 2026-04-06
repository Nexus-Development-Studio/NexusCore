package cc.synkdev.nexusCore.bukkit.objects;

import cc.synkdev.nexusCore.bukkit.NexusCore;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter @Setter
public class AnalyticsReport {
    private NexusCore core = NexusCore.getInstance();
    UUID uuid;
    Long timestamp;
    int onlinePlayers;
    String serverType;
    String serverVersion;
    String javaVersion;
    String os;
    int coreCount;
    String architecture;
    String language;
    Boolean onlineMode;
    String libsVersion;
    Map<String, String> plugins;
    Map<String, PluginData> synkPlugins;

    public AnalyticsReport() {
        this.uuid = core.serverUUID;
        this.serverType = getServerSoftware();
        this.serverVersion = Bukkit.getVersion();
        this.javaVersion = System.getProperty("java.version")+" "+ System.getProperty("java.vendor")+" "+System.getProperty("java.vm.name");
        this.os = System.getProperty("os.name") + " " + System.getProperty("os.version");
        this.coreCount = Runtime.getRuntime().availableProcessors();
        this.architecture = System.getProperty("os.arch");
        this.language = NexusCore.lang;
        this.libsVersion = core.ver();
        this.onlineMode = Bukkit.getOnlineMode();

        this.plugins = new HashMap<>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            this.plugins.put(plugin.getName(), plugin.getDescription().getVersion());
        }

        this.synkPlugins = new HashMap<>();
        for (JavaPlugin pl : core.pls) {
            synkPlugins.put(pl.getName(), new PluginData(pl));
        }
    }

    private String getServerSoftware() {
        String serverName = Bukkit.getServer().getName();
        switch (serverName) {
            case "Purpur": return "Purpur";
            case "Paper": return "Paper";
            case "Spigot": return "Spigot";
            case "CraftBukkit": return "CraftBukkit";
            default: return "Unknown (" + serverName + ")";
        }
    }


    public JSONObject export() {
        JSONObject obj = new JSONObject();
        obj.put("uuid", uuid.toString());
        obj.put("timestamp", timestamp);
        obj.put("online_players", onlinePlayers);
        obj.put("server_type", serverType);
        obj.put("server_version", serverVersion);
        obj.put("java_version", javaVersion);
        obj.put("os", os);
        obj.put("core_count", coreCount);
        obj.put("system_architecture", architecture);
        obj.put("language", language);
        obj.put("online_mode", onlineMode);
        obj.put("libs_version", libsVersion);

        JSONArray pls = new JSONArray();
        for (Map.Entry<String, String> entry : plugins.entrySet()) {
            JSONObject o = new JSONObject();
            o.put("name", entry.getKey());
            o.put("version", entry.getValue());
            pls.put(o);
        }
        obj.put("plugins", pls);

        JSONArray spls = new JSONArray();
        for (Map.Entry<String, PluginData> entry : synkPlugins.entrySet()) {
            JSONObject o = new JSONObject();
            o.put("name", entry.getKey());
            o.put("version", entry.getValue().getVersionCurr());

            JSONArray cmds = new JSONArray();
            for (Map.Entry<String, Integer> mapEntry : entry.getValue().getCommandUses().entrySet()) {
                JSONObject oo = new JSONObject();
                oo.put("command", mapEntry.getKey());
                oo.put("uses", mapEntry.getValue());
                cmds.put(oo);
            }
            o.put("commands", cmds);

            JSONObject fields = new JSONObject();
            for (Map.Entry<String, Object> mapEntry : entry.getValue().getFields().entrySet()) {
                fields.put(mapEntry.getKey(), mapEntry.getValue());
            }
            o.put("fields", fields);
            spls.put(o);
        }
        obj.put("synk_plugins", spls);
        return obj;
    }
}

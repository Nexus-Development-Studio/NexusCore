package cc.synkdev.nexusCore.bukkit;

import cc.synkdev.nexusCore.bukkit.commands.ReportCmd;
import cc.synkdev.nexusCore.bukkit.objects.PluginData;
import cc.synkdev.nexusCore.components.DiscordWebhook;
import cc.synkdev.nexusCore.components.NexusPlugin;
import cc.synkdev.nexusCore.components.PluginUpdate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Utils implements Listener {
    private final NexusCore core = NexusCore.getInstance();
    private static final NexusCore sCore = NexusCore.getInstance();
    NexusPlugin spl;
    public Utils(NexusPlugin spl) {
        this.spl = spl;
    }
    public static void log(String s) {
        Bukkit.getConsoleSender().sendMessage(NexusCore.getPl().prefix()+" "+ChatColor.translateAlternateColorCodes('&', s));
    }
    public static void log(String s, boolean prefix) {
        Bukkit.getConsoleSender().sendMessage((prefix ? NexusCore.getPl().prefix() : "")+" "+ChatColor.translateAlternateColorCodes('&', s));
    }

    public static FileConfiguration loadWebConfig(String url, File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        File temp = new File(file.getParentFile(), "temp-"+System.currentTimeMillis()+".yml");
        try {
            URL uri = new URL(url);
            if (!temp.exists()) temp.createNewFile();
            BufferedReader reader = new BufferedReader(new InputStreamReader(uri.openStream()));

            BufferedWriter writer = new BufferedWriter(new FileWriter(temp));

            String line;
            while ((line = reader.readLine()) != null) {
                String[] lines = line.split("<br>");
                for (String liness : lines) {
                    String[] split = liness.split(";");
                    if (split.length == 3) {
                        if (!config.contains(split[1])) {
                            writer.write("# " + split[0]);
                            writer.newLine();
                            writer.write(split[1] + ": " + split[2]);
                            writer.newLine();
                        } else {
                            writer.write("# " + split[0]);
                            writer.newLine();
                            writer.write(split[1] + ": " + config.get(split[1]));
                            writer.newLine();
                        }
                    } else {
                        if (!config.contains(split[0])) {
                            writer.write(split[0] + ": " + split[1]);
                            writer.newLine();
                        } else {
                            writer.write(split[0] + ": " + config.get(split[0]));
                            writer.newLine();
                        }
                    }
                }
            }

            reader.close();
            writer.close();

            copyFile(temp, file);

            temp.delete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    public static void copyFile(File source, File destination) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(destination)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    public static File getFile(String pl) {
        File file = null;
        for (File lF : sCore.getDataFolder().getParentFile().listFiles()) {
            if (file == null) {
                if (lF.getName().contains(pl) && lF.getName().contains(".jar")) {
                    file = lF;
                }
            }
        }
        if (file == null) {
            file = new File(sCore.getDataFolder().getParentFile(), pl+".jar");
        }
        return file;
    }

    @EventHandler
    public void join (PlayerJoinEvent event) {
        if (event.getPlayer().isOp()) NexusCore.availableUpdates.forEach((s, s2) -> {
            Player p = event.getPlayer();
            p.sendMessage(core.prefix+ChatColor.GOLD+Lang.translate("updateAvailable", s) + " "+s+"!");
            p.sendMessage(core.prefix+ChatColor.GOLD+Lang.translate("downloadHere", s)+": "+s.dlLink());
        });
    }

    public static Map<String, Plugin> getActivePlugins() {
        Map<String, Plugin> map = new HashMap<>();
        sCore.getPlugins().clear();
        for (Plugin pl : Bukkit.getPluginManager().getPlugins()) {
            if (pl.getDescription().getAuthors().contains("Synk") || pl.getDescription().getAuthors().contains("Riddles")) {
                map.put(pl.getName(), pl);
                if (!sCore.versions.containsKey(pl.getName())) sCore.versions.put(pl.getName(), pl.getDescription().getVersion());
                sCore.getPlugins().add(pl.getDescription().getName());
            }
        }
        return map;
    }

    public static List<PluginData> getPlugins() {
        List<PluginData> list = new ArrayList<>();
        JSONObject obj = UpdateChecker.readData();
        List<String> active = new ArrayList<>(getActivePlugins().keySet());
        for (String s : obj.keySet()) {
            JSONObject pObj = obj.getJSONObject(s);
            if (s.equals("SynkLibs")) continue;
            if (active.stream().anyMatch(plugin -> plugin.equalsIgnoreCase(s))) {
                Map<Integer, PluginUpdate> javaVer = fetchJavaVer(pObj, s);
                list.add(new PluginData(s, getActivePlugins().get(s).getDescription().getVersion(), pObj.getString("version"), pObj.getString("link"), javaVer, new HashMap<>(), new HashMap<>(), false));
            } else {
                Utils.debug("Plugin detected as disabled/not present "+s);
                File file = getFile(s);
                if (file == null) continue;
                Utils.debug("Plugin detected as disabled and present "+s);
                Map<Integer, PluginUpdate> javaVer = fetchJavaVer(pObj, s);
                int curr = Runtime.version().feature();
                String ver = pObj.getString("version");
                String dl = pObj.getString("link");
                for (Integer i : javaVer.keySet()) {
                    if (curr < i) {
                        Utils.debug("Lower java version available "+i);
                        ver = pObj.getJSONObject("advanced").getJSONObject("java").getJSONObject(i.toString()).getString("version");
                        dl = pObj.getJSONObject("advanced").getJSONObject("java").getJSONObject(i.toString()).getString("link");
                    }
                }

                String currentVersion = getVersionFromJar(file);
                list.add(new PluginData(s, currentVersion, ver, dl, javaVer, new HashMap<>(), new HashMap<>(), true));
            }
        }
        return list;
    }

    private static Map<Integer, PluginUpdate> fetchJavaVer(JSONObject obj, String s) {
        Map<Integer, PluginUpdate> javaVer = new HashMap<>();
        if (obj.has("advanced") && obj.getJSONObject("advanced").has("java")) {
            JSONObject javaObj = obj.getJSONObject("advanced").getJSONObject("java");
            for (String javaNum : javaObj.keySet()) {
                javaVer.put(Integer.parseInt(javaNum), new PluginUpdate(javaObj.getJSONObject(javaNum).getString("version"), s, javaObj.getJSONObject(javaNum).getString("link"), getFile(s)));
            }
        }
        return javaVer;
    }

    public static void debug(String s) {
        if (sCore.debug) log(ChatColor.translateAlternateColorCodes('&', "&8[DEBUG] &e"+s));
    }

    private static String getVersionFromJar(File file) {
        try (JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("plugin.yml");
            if (entry == null) return null;
            InputStream is = jar.getInputStream(entry);
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(new InputStreamReader(is));
            return yml.getString("version");
        } catch (IOException e) {
            Utils.debug("Couldn't read version from jar: " + file.getName());
            return null;
        }
    }

    public static void reportError(String s, Exception e) {
        DiscordWebhook wh = new DiscordWebhook(WebhookUrl.URL);
        wh.setContent("<@&1498246565704241162>");

        wh.addEmbed(new DiscordWebhook.EmbedObject()
                .setTitle("Exception detected")
                .setDescription(s)
                .addField("Message", e.getMessage(), false)
                .addField("Dump", "https://synkdev.cc/dump/"+new ReportCmd(sCore).send(), false));
        try {
            wh.execute();
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }
}

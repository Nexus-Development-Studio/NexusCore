package cc.synkdev.nexusCore.bukkit;

import cc.synkdev.nexusCore.bukkit.objects.PluginData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class UpdateChecker {
    private static final NexusCore core = NexusCore.getInstance();
    public static JSONObject readData() {
        try {
            URL url = new URL("https://synkdev.cc/storage/versions.json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String ln;
            while ((ln = in.readLine()) != null) {
                content.append(ln);
            }
            in.close();
            conn.disconnect();
            return new JSONObject(content.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<PluginData> checkOutated() {
        NexusCore.setPl(NexusCore.getInstance());
        List<Plugin> synkPlugs = new ArrayList<>();
        core.getPlugins().clear();
        for (Plugin pl : Bukkit.getPluginManager().getPlugins()) {
            if (pl.getDescription().getAuthors().contains("Synk") || pl.getDescription().getAuthors().contains("Riddles")) {
                synkPlugs.add(pl);
                if (!core.versions.containsKey(pl.getName())) core.versions.put(pl.getName(), pl.getDescription().getVersion());
                core.getPlugins().add(pl.getDescription().getName());
            }
        }

        return Utils.getPlugins().stream().filter(pluginData -> pluginData.getVersionCurr() == null || !pluginData.getVersionCurr().equals(pluginData.getVersionNew())).collect(Collectors.toList());
    }
    public static void update(List<PluginData> list) {
        AtomicInteger skipped = new AtomicInteger();
        for (PluginData pd : list) {
            AtomicBoolean doUpdate = new AtomicBoolean(true);
            try {
                File original = Utils.getFile(pd.getName());
                if (pd.getDl() == null) continue;
                if (!pd.getDisabled() && !pd.getJavaVer().isEmpty()) {
                    pd.getJavaVer().forEach((integer, pluginUpdate) -> {
                        if (Runtime.version().feature() < integer) {
                            pd.setVersionNew(pluginUpdate.getNum());
                            pd.setDl(pluginUpdate.getDl());
                            if (pd.getVersionNew().equals(pd.getVersionCurr())) {
                                doUpdate.set(false);
                                skipped.getAndIncrement();
                            }
                        }
                    });
                }
                if (doUpdate.get()) {
                    URL url = new URL(pd.getDl());
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    boolean valid = true;

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        try (InputStream in = conn.getInputStream();
                             OutputStream out = Files.newOutputStream(new File(Utils.getFile(pd.getName()).getParentFile(), pd.getName() + ".jar").toPath())) {
                            Utils.debug("Downloading " + pd.getName() + " v" + pd.getVersionNew() + " from " + pd.getDl());

                            byte[] buffer = new byte[4096];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                            out.flush();
                            core.versions.put(pd.getName(), pd.getVersionNew());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (NullPointerException ee) {
                            Utils.debug("Aut-updater NPE");
                            Utils.reportError("Auto-updater NPE", ee);
                            valid = false;
                        }

                    }
                    conn.disconnect();
                    if (valid) original.delete();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (skipped.get()<list.size()) Utils.log(ChatColor.GOLD+"New plugin versions have been downloaded! Restart your server for the changes to apply.");
    }

}

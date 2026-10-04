package cc.synkdev.nexuscore.bukkit;

import cc.synkdev.nexuscore.components.NexusPlugin;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Lang {
    private Lang() {
        /* This utility class should not be instantiated */
    }

    public static Map<String, String> init(NexusPlugin plugin, File langFile) {
        String globLang = Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("NexusCore")).getConfig().getString("lang", "en");
        return init(plugin, langFile, globLang);
    }

    public static Map<String, String> init(NexusPlugin plugin, File langFile, String lang) {
        Map<String, String> map = new HashMap<>();
        if (lang.equalsIgnoreCase("custom")) {
            if (langFile.exists()) {
                try {
                    Map<String, String> curr = load(langFile);
                    File temp = new File(langFile.getParentFile(), "temp-" + System.currentTimeMillis() + ".json");
                    if (!temp.createNewFile()) throw new IOException("Failed to create temp file!");

                    writeToFile(plugin, temp);

                    Map<String, String> tempMap = new HashMap<>(load(temp));
                    for (Map.Entry<String, String> entry : tempMap.entrySet()) {
                        if (!curr.containsKey(entry.getKey())) {
                            curr.put(entry.getKey(), entry.getValue());
                        }
                    }
                    Files.delete(temp.toPath());
                    save(langFile, curr);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    if (!langFile.getParentFile().exists() && !langFile.getParentFile().mkdirs()) {
                            throw new IOException("Failed to create lang folder!");
                        }

                    if (!langFile.createNewFile()) throw new IOException("Failed to create lang.json file!");

                    writeToFile(plugin, langFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            map.putAll(load(langFile));
        } else {
            if (!folderExists(lang)) {
                Utils.log(ChatColor.RED+"The language "+lang+" doesn't exist! Visit https://synkdev.cc/storage/translations for the full list! Using english as a fallback language.");
                lang = "en";
            }
            try {
                File temp = new File(langFile.getParentFile(), "temp-"+System.currentTimeMillis()+".json");
                if (!temp.getParentFile().exists() && !temp.getParentFile().mkdirs()) throw new IOException("Failed to create temp folder!");

                if (!temp.createNewFile()) throw new IOException("Failed to create temp file!");

                BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
                BufferedReader reader = new BufferedReader(new InputStreamReader(URI.create(plugin.lang().replace("lang-pld", lang)).toURL().openStream()));

                String ln;
                while ((ln = reader.readLine()) != null) {
                    writer.write(ln);
                    writer.newLine();
                }
                writer.close();
                map.putAll(load(temp));
                Files.delete(temp.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return map;
    }

    private static void writeToFile(NexusPlugin plugin, File langFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(langFile));
        BufferedReader reader = new BufferedReader(new InputStreamReader(URI.create(plugin.lang().replace("lang-pld", "en")).toURL().openStream()));

        String ln;
        while ((ln = reader.readLine()) != null) {
            writer.write(ln);
            writer.newLine();
        }
        writer.close();
    }

    public static void save(File file, Map<String, String> map) throws IOException {
        JSONObject obj = new JSONObject();
        map.forEach(obj::put);
        Files.writeString(file.toPath(), obj.toString(2));
    }

    public static Boolean folderExists(String lang) {
        try {
            URL url = URI.create("https://synkdev.cc/storage/translations/"+lang).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int responseCode = conn.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_FORBIDDEN;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static Map<String, String> load(File file) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(file)) {
            //noinspection unchecked
            return new HashMap<String, String>(gson.fromJson(reader, HashMap.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String translate(String key, NexusPlugin spl, String... placeholders) {
        String translatedString = Utils.color(spl.langMap().getOrDefault(key, "Invalid translation!"));
        try {
            for (int i = 0; i < placeholders.length; i++) {
                translatedString = translatedString.replace("%s" + (i + 1) + "%", placeholders[i]);
            }
        } catch (Exception _) {
            // Ignore any exceptions that may occur during placeholder replacement
        }
        if (translatedString.equals("Invalid translation!")) Utils.debug("Invalid translation for key "+key);
        return translatedString;
    }

}

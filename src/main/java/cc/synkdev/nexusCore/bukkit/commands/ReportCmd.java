package cc.synkdev.nexusCore.bukkit.commands;

import cc.synkdev.nexusCore.bukkit.NexusCore;
import cc.synkdev.nexusCore.components.folia.Platform;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@CommandAlias("ncreport|ncdump|nexuscorereport|nexuscoredump")
public class ReportCmd extends BaseCommand {
    private final NexusCore core;
    public ReportCmd(NexusCore core) {
        this.core = core;
    }

    @Default
    @CommandPermission("nexuscore.report")
    @Description("Send informations about your server for support")
    public void onReport(CommandSender sender) {
    String uuid = send();
    if (uuid != null) {
            TextComponent comp = new TextComponent(core.prefix() + ChatColor.GREEN + "Your report has been exported!\n"+core.prefix()+ChatColor.GREEN+"Please save this link somewhere as it will be used by the support team: " + ChatColor.GOLD);
            TextComponent uuidComp = new TextComponent(ChatColor.GOLD+"https://synkdev.cc/dump/"+uuid);
            comp.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://synkdev.cc/dump/"+uuid));
            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent("Click to copy or open it")}));
            comp.addExtra(uuidComp);

            if (sender instanceof Player) {
                ((Player) sender).spigot().sendMessage(comp);
            } else {
                sender.sendMessage(core.prefix() + ChatColor.GREEN + "Your report has been exported!\n" + core.prefix() + ChatColor.GREEN + "Please save this URL somewhere as it will be used by the support team: " + ChatColor.GOLD + "https://synkdev.cc/dump/"+uuid);
            }
        } else {
            sender.sendMessage(core.prefix() + ChatColor.RED + "There was an error while uploading your report!");
        }
    }

    public String send() {
        try {
            URL url = new URL("https://dump.synkdev.cc/upload");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            OutputStream os = connection.getOutputStream();
            os.write(getJsonString().getBytes());
            os.flush();
            os.close();

            int resp = connection.getResponseCode();
            if (resp == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                org.json.JSONObject jsonResponse = new JSONObject(response.toString());
                return jsonResponse.getString("uuid");

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private String getJsonString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("serverVersion", Bukkit.getServer().getVersion());
        jsonObject.put("serverSoftware", Platform.isFolia() ? "Folia" : Bukkit.getServer().getBukkitVersion());
        jsonObject.put("operatingSystem", System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
        jsonObject.put("javaVersion", System.getProperty("java.version")+" "+ System.getProperty("java.vendor")+" "+System.getProperty("java.vm.name"));

        JSONArray pluginsArray = new JSONArray();
        for (Plugin pl : Bukkit.getPluginManager().getPlugins()) {
            JSONObject pluginObject = new JSONObject();
            pluginObject.put("name", pl.getName());
            pluginObject.put("version", pl.getDescription().getVersion());
            pluginsArray.put(pluginObject);
        }
        jsonObject.put("plugins", pluginsArray);

        JSONObject additionalInfo = new JSONObject();
        additionalInfo.put("online-mode", Bukkit.getServer().getOnlineMode());
        jsonObject.put("additionalInfo", additionalInfo);

        JSONArray configsArray = new JSONArray();
        for (Plugin pl : Bukkit.getPluginManager().getPlugins()) {
            if (pl.getDescription().getAuthors().contains("Synk")) {
                JSONObject configObject = new JSONObject();
                configObject.put("name", pl.getName());
                configObject.put("config", pl.getConfig().saveToString());
                configsArray.put(configObject);
            }
        }
        jsonObject.put("configs", configsArray);

        return jsonObject.toString(2);
    }
}

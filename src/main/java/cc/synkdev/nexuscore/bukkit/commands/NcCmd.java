package cc.synkdev.nexuscore.bukkit.commands;


import cc.synkdev.nexuscore.bukkit.Lang;
import cc.synkdev.nexuscore.bukkit.NexusCore;
import cc.synkdev.nexuscore.bukkit.UpdateChecker;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;

@CommandAlias("nc|nexuscore")
public class NcCmd extends BaseCommand {
    private final NexusCore core;
    public NcCmd(NexusCore core) {
        this.core = core;
    }

    @Subcommand("reload")
    @CommandPermission("nexuscore.command.reload")
    public void onReload(CommandSender sender) {
        core.loadConfig();
        sender.sendMessage(core.prefix()+ ChatColor.GREEN+ Lang.translate("reloaded", core));
    }

    @Subcommand("checkUpdates")
    @CommandPermission("nexuscore.command.checkupdates")
    public void onCheckUpdates(CommandSender sender) {
        boolean old = core.doAutoUpdate;
        core.doAutoUpdate = true;
        UpdateChecker.update(UpdateChecker.checkOutated());
        core.doAutoUpdate = old;
        sender.sendMessage(core.prefix()+Lang.translate("checkedUpdate", core));
    }

    public static String getPublicIp() {
        try {
            URL url = URI.create("https://api.ipify.org").toURL();
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            return br.readLine();
        } catch (Exception _) {
            return "Unknown";
        }
    }

    @Subcommand("support")
    @CommandPermission("nexuscore.command.support")
    public void onSupport(CommandSender sender) {
        TextComponent comp = new TextComponent(core.prefix()+Lang.translate("supportLink", core));
        TextComponent link = new TextComponent(ChatColor.GREEN+" https://discord.gg/KxPE2bK5Bu");
        comp.addExtra(link);
        sender.spigot().sendMessage(comp);
    }

    @Subcommand("support start")
    @CommandPermission("nexuscore.command.support")
    public void onSupportStart(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(core.prefix()+Lang.translate("missingArgs", core, "/support start <support id>"));
            return;
        }

        String uuid = args[0];
        String ip = getPublicIp();
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create("https://analytics.synkdev.cc/api/support/verify?uuid="+uuid+"&ip="+ip).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);

            int code = conn.getResponseCode();
            if (code != 200) {
                switch (code){
                    case 403:
                        sender.sendMessage(core.prefix()+Lang.translate("support403", core));
                        break;
                    case 401:
                        sender.sendMessage(core.prefix()+Lang.translate("support401", core));
                        break;
                    default:
                        sender.sendMessage(core.prefix()+Lang.translate("support500", core));
                        break;
                }
                return;
            }

            conn = (HttpURLConnection) URI.create("https://analytics.synkdev.cc/api/support/download?uuid="+uuid+"&ip="+ip).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);

            conn.getResponseCode();

            try (InputStream in = conn.getInputStream();
                 OutputStream out = Files.newOutputStream(new File(core.getDataFolder().getParentFile(), "RemoteSupportNexus.jar").toPath())) {

                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            File codeFile = new File(core.getDataFolder(), "support_code");
            Files.writeString(codeFile.toPath(), uuid);

            sender.sendMessage(core.prefix()+Lang.translate("supportDownloaded", core));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Subcommand("support end")
    @CommandPermission("nexuscore.command.support")
    public void onSupportEnd(CommandSender sender) throws IOException {
        File codeFile = new File(core.getDataFolder(), "support_code");
        if (!codeFile.exists()) {
            sender.sendMessage(core.prefix()+Lang.translate("noSupport", core));
            return;
        }
        BufferedReader reader = new BufferedReader(new FileReader(codeFile));
        String uuid = reader.readLine();
        reader.close();
        HttpURLConnection conn = (HttpURLConnection) URI.create("https://analytics.synkdev.cc/api/support/finish?uuid="+uuid+"&ip="+getPublicIp()).toURL().openConnection();
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
        if (code == 403) {
            sender.sendMessage(core.prefix()+Lang.translate("support401", core));
        } else if (code != HttpURLConnection.HTTP_OK) {
            sender.sendMessage(core.prefix()+Lang.translate("supportFinishError", core));
        }

       Files.delete(codeFile.toPath());
       Files.delete(new File(core.getDataFolder().getParentFile(), "RemoteSupportNexus.jar").toPath());

       sender.sendMessage(core.prefix()+Lang.translate("supportFinish", core));
    }
}

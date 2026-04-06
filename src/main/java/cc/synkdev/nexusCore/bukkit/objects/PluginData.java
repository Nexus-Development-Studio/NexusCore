package cc.synkdev.nexusCore.bukkit.objects;

import cc.synkdev.nexusCore.components.PluginUpdate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

@Getter @Setter @AllArgsConstructor
public class PluginData {
    private String name;
    private String versionCurr;
    private String versionNew;
    private String dl;
    private Map<Integer, PluginUpdate> javaVer = new HashMap<>();
    private Map<String, Integer> commandUses;
    private Map<String, Object> fields;
    private Boolean disabled;
    public PluginData(JavaPlugin plugin) {
        this.name = plugin.getDescription().getName();
        this.versionCurr = plugin.getDescription().getVersion();
        this.commandUses = new HashMap<>();
        this.fields = new HashMap<>();
    }
}

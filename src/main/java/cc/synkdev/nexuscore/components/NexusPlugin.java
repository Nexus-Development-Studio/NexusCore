package cc.synkdev.nexuscore.components;

import java.util.Map;

public interface NexusPlugin {
    String name();
    String ver();
    String dlLink();
    String prefix();
    String lang();
    Map<String, String> langMap();
}

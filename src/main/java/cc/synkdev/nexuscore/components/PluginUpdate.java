package cc.synkdev.nexuscore.components;

import java.io.File;

public record PluginUpdate(String num, String plugin, String dl, File current) {
}

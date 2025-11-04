package dev.khoa.lifecyclewebhook;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VersionStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public VersionStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "versions.yml");
    }

    public void load() {
        if (!file.exists()) return;
        FileConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String k : yml.getKeys(false)) {
            cache.put(k, yml.getString(k));
        }
    }

    public void save() {
        try {
            FileConfiguration yml = new YamlConfiguration();
            for (Map.Entry<String, String> e : cache.entrySet()) {
                yml.set(e.getKey(), e.getValue());
            }
            yml.save(file);
        } catch (IOException ignored) {}
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public String getVersion(String pluginName) { return cache.get(pluginName); }
    public void setVersion(String pluginName, String version) { cache.put(pluginName, version); }
}

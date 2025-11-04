package dev.khoa.lifecyclewebhook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.HashSet;
import java.util.Set;

public class LifecycleWebhook extends JavaPlugin implements Listener {
    private WebhookClient client;
    private VersionStore store;

    private String hook;
    private boolean watchAll;
    private Set<String> watchList;
    private int colorOnline, colorOffline, colorEnable, colorDisable, colorUpdate;
    private boolean includeVersion, includeTps;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        hook = getConfig().getString("webhook_url", "");
        if (hook == null || hook.isBlank()) {
            getLogger().severe("Missing webhook_url in config.yml");
        }
        watchAll = getConfig().getBoolean("watch_all_plugins", true);
        watchList = new HashSet<>(getConfig().getStringList("watch_list"));

        colorOnline  = getConfig().getInt("colors.online", 65280);
        colorOffline = getConfig().getInt("colors.offline", 16711680);
        colorEnable  = getConfig().getInt("colors.plugin_enable", 3447003);
        colorDisable = getConfig().getInt("colors.plugin_disable", 15158332);
        colorUpdate  = getConfig().getInt("colors.plugin_update", 15844367);

        includeVersion = getConfig().getBoolean("include_server_version", true);
        includeTps     = getConfig().getBoolean("include_tps", true);

        client = new WebhookClient(hook, getLogger());
        store  = new VersionStore(this);
        store.load();

        Bukkit.getPluginManager().registerEvents(this, this);

        sendOnline();
        snapshotAll();
    }

    @Override
    public void onDisable() {
        sendOffline();
        store.save();
    }

    private void sendOnline() {
        WebhookClient.EmbedBuilder eb = WebhookClient.embed()
            .title("✅ Server Online")
            .description("Máy chủ đã khởi động.")
            .color(colorOnline)
            .timestampNow();

        if (includeVersion) {
            eb.field("Version", "**" + Bukkit.getVersion() + "**", true);
        }
        if (includeTps) {
            double tps = getTpsSafe();
            eb.field("TPS", "**" + formatTps(tps) + "**", true);
        }
        client.postEmbed(eb.build());
    }

    private void sendOffline() {
        WebhookClient.EmbedBuilder eb = WebhookClient.embed()
            .title("🛑 Server Offline")
            .description("Máy chủ đã tắt.")
            .color(colorOffline)
            .timestampNow();
        client.postEmbed(eb.build());
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent e) {
        Plugin p = e.getPlugin();
        if (!shouldWatch(p.getName())) return;

        String newVer = safeVer(p);
        String oldVer = store.getVersion(p.getName());

        if (oldVer == null) {
            sendPluginEnable(p.getName(), newVer);
        } else if (!oldVer.equals(newVer)) {
            sendPluginUpdate(p.getName(), oldVer, newVer);
        } else {
            sendPluginEnable(p.getName(), newVer);
        }
        store.setVersion(p.getName(), newVer);
        store.saveAsync();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent e) {
        Plugin p = e.getPlugin();
        if (!shouldWatch(p.getName())) return;

        String ver = store.getVersion(p.getName());
        sendPluginDisable(p.getName(), ver != null ? ver : safeVer(p));
    }

    private void sendPluginEnable(String name, String ver) {
        WebhookClient.EmbedBuilder eb = WebhookClient.embed()
            .title("🔌 Plugin Enabled")
            .description("`" + name + "`")
            .color(colorEnable)
            .timestampNow();
        if (ver != null) eb.field("Version", "**" + ver + "**", true);
        client.postEmbed(eb.build());
    }

    private void sendPluginDisable(String name, String ver) {
        WebhookClient.EmbedBuilder eb = WebhookClient.embed()
            .title("⛔ Plugin Disabled")
            .description("`" + name + "`")
            .color(colorDisable)
            .timestampNow();
        if (ver != null) eb.field("Version", "**" + ver + "**", true);
        client.postEmbed(eb.build());
    }

    private void sendPluginUpdate(String name, String fromVer, String toVer) {
        WebhookClient.EmbedBuilder eb = WebhookClient.embed()
            .title("🛠️ Plugin Updated")
            .description("`" + name + "`")
            .color(colorUpdate)
            .timestampNow()
            .field("From", "`" + nullSafe(fromVer) + "`", true)
            .field("To", "`" + nullSafe(toVer) + "`", true);
        client.postEmbed(eb.build());
    }

    private boolean shouldWatch(String pluginName) {
        return watchAll || watchList.contains(pluginName);
    }

    private String safeVer(Plugin p) {
        try {
            return p.getDescription().getVersion();
        } catch (Exception ex) {
            return null;
        }
    }

    private String nullSafe(String s) { return s == null ? "unknown" : s; }

    private void snapshotAll() {
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            store.setVersion(p.getName(), safeVer(p));
        }
        store.saveAsync();
    }

    private double getTpsSafe() {
        try {
            double[] tps = Bukkit.getServer().getTPS();
            return tps != null && tps.length > 0 ? tps[0] : -1.0;
        } catch (Throwable ignored) {
            return -1.0;
        }
    }
    private String formatTps(double v) {
        if (v < 0) return "N/A";
        return String.format("%.2f", Math.min(v, 20.0));
    }
}

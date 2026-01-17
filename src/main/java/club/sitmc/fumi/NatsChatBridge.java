package club.sitmc.fumi;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class NatsChatBridge implements Listener {

    private final FumiSpigot plugin;
    private Connection natsConnection;
    private Dispatcher dispatcher; // 保存 Dispatcher
    private String subject;
    private String chatFormat;
    private boolean forwardPlayerChat;
    private String sourceName;
    private String natsToken;

    public NatsChatBridge(FumiSpigot plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        plugin.saveDefaultConfig();
        setupDefaultConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadConfigValues();
        connectToNatsWithRetry();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NatsPlaceholderExpansion(this).register();
            plugin.getLogger().info("PlaceholderAPI detected. Registered placeholders.");
        }
    }

    public void disable() {
        disconnectNats();
    }

    public void reload() {
        plugin.reloadConfig();
        loadConfigValues();
        disconnectNats(); // 断开原有连接和订阅
        connectToNatsWithRetry(); // 重新连接
    }

    private void setupDefaultConfig() {
        FileConfiguration config = plugin.getConfig();
        config.addDefault("nats.url", "nats://localhost:4222");
        config.addDefault("nats.token", ""); // 新增 token
        config.addDefault("nats.subject", "minecraft.chat");
        config.addDefault("minecraft.source-name", "minecraft");
        config.addDefault("minecraft.chat-format", "&7[{source}] &f{username}: &e{message}");
        config.addDefault("minecraft.forward-player-chat", true);
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    private void loadConfigValues() {
        FileConfiguration config = plugin.getConfig();
        subject = config.getString("nats.subject");
        chatFormat = ChatColor.translateAlternateColorCodes('&', config.getString("minecraft.chat-format"));
        forwardPlayerChat = config.getBoolean("minecraft.forward-player-chat");
        sourceName = config.getString("minecraft.source-name", "minecraft");
        natsToken = config.getString("nats.token", "");
        plugin.getLogger().info("Loaded config: subject=" + subject + ", chatFormat=" + chatFormat +
                ", forwardPlayerChat=" + forwardPlayerChat + ", sourceName=" + sourceName + ", token=" + (natsToken.isEmpty() ? "none" : "****"));
    }

    private void connectToNatsWithRetry() {
        disconnectNats(); // 确保旧连接已断开

        String url = plugin.getConfig().getString("nats.url");
        int maxRetries = 5;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                plugin.getLogger().info("Connecting to NATS (attempt " + attempt + "/" + maxRetries + "): " + url);

                Options.Builder builder = new Options.Builder()
                        .server(url)
                        .connectionTimeout(Duration.ofSeconds(3));

                if (!natsToken.isEmpty()) {
                    builder.token(natsToken);
                }

                natsConnection = Nats.connect(builder.build());

                dispatcher = natsConnection.createDispatcher(msg -> {
                    String raw = new String(msg.getData(), StandardCharsets.UTF_8);
                    Bukkit.getScheduler().runTask(plugin, () -> handleIncomingMessage(raw));
                });
                dispatcher.subscribe(subject);

                plugin.getLogger().info("Connected to NATS and subscribed to subject: " + subject);
                return;

            } catch (Exception e) {
                plugin.getLogger().warning("NATS connection failed: " + e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ignored) {}
                }
            }
        }
        plugin.getLogger().severe("Could not connect to NATS after " + maxRetries + " attempts.");
    }

    private void disconnectNats() {
        try {
            if (dispatcher != null) {
                dispatcher.unsubscribe(subject);
                dispatcher = null;
            }
            if (natsConnection != null) {
                natsConnection.close();
                natsConnection = null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error disconnecting NATS: " + e.getMessage());
        }
    }

    private void handleIncomingMessage(String raw) {
        try {
            JSONObject json = new JSONObject(raw);
            String source = json.optString("source", "unknown");
            String username = json.optString("username");
            String message = json.optString("message");

            if (sourceName.equalsIgnoreCase(source)) return;
            if (username.isEmpty() || message.isEmpty()) return;

            String formatted = chatFormat
                    .replace("{source}", source)
                    .replace("{username}", username)
                    .replace("{message}", message);
            Bukkit.broadcastMessage(formatted);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse incoming NATS message: " + raw);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!forwardPlayerChat || natsConnection == null) return;

        JSONObject json = new JSONObject();
        json.put("source", sourceName);
        json.put("username", event.getPlayer().getName());
        json.put("message", event.getMessage());

        try {
            natsConnection.publish(subject, json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to publish message to NATS: " + e.getMessage());
        }
    }

    public String getChatFormat() {
        return chatFormat;
    }
}

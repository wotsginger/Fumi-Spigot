package club.sitmc.fumi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class NatsPlaceholderExpansion extends PlaceholderExpansion {

    private final NatsChatBridge bridge;

    public NatsPlaceholderExpansion(NatsChatBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public String getIdentifier() {
        return "fumi";
    }

    @Override
    public String getAuthor() {
        return "上海应用技术大学Minecraft社";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        switch (identifier.toLowerCase()) {
            case "format":
                return bridge.getChatFormat();
            default:
                return null;
        }
    }
}

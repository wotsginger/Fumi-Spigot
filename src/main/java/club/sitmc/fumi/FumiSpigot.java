package club.sitmc.fumi;

import org.bukkit.plugin.java.JavaPlugin;

public final class FumiSpigot extends JavaPlugin {

    private NatsChatBridge natsChatBridge;

    @Override
    public void onEnable() {
        // 初始化并启动 NATS 聊天桥
        natsChatBridge = new NatsChatBridge(this);
        natsChatBridge.enable();

        getCommand("fumi").setExecutor((sender, command, label, args) -> {
            if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
                if (!sender.hasPermission("fumi.reload")) {
                    sender.sendMessage("§c你没有权限执行该命令！");
                    return true;
                }
                natsChatBridge.reload();
                sender.sendMessage("§aFumi 配置文件已重载，并尝试重新连接 NATS 服务器！");
                return true;
            }
            sender.sendMessage("§e用法: /fumi reload");
            return true;
        });
    }

    @Override
    public void onDisable() {
        if (natsChatBridge != null) {
            natsChatBridge.disable();
        }
    }
}

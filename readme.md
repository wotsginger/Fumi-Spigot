# 🌸 Fumi Spigot

> **致力于在不同消息平台间传递消息的信使o(*￣▽￣*)ブ**

Fumi 是一个基于 [NATS 服务器](https://github.com/nats-io) 的消息转发工具，旨在为不同消息平台之间构建一条转发通道。

我们假定消息分布在多个平台，并且存在多个来源，通过内置的 API、协议库、机器人进行相互的消息转发。

所有消息会按照统一的格式发送到 NATS 中心服务器上，再分别由各个客户端进行解析。

目前实现的平台和即将进行适配的平台。

| 平台                  | 支持状况 | 项目地址 |
|---------------------|------|---|
| Bikkit/Spigot/Paper | 已支持  |你在这里|
| QQ（Standalone）      | 已支持  |[地址](https://github.com/wotsginger/Fumi-Standalone)
| QQ（Nonebot）         | 已支持  |暂未发布|
| Forge               | 已支持  |暂未发布|
| Fabric              | 已支持  |[地址](https://github.com/wotsginger/Fumi-Fabric)|
| KOOK                | 计划中  |暂未发布|
| Oopz                | 计划中  |暂未发布|
| Discord             | 计划中  |暂未发布|

实际上来说，只需要支持将消息以 `{"source":"","message":"","username":""}` 发送到NATS并支持解析从NATS接收到的信息并发送到聊天中就可以适配对应的平台，如果您有能力完全可以开发对应的第三方客户端。

---

## 🚀 快速开始

### 1. 环境准备

从我们的 [官网](https：//www.sitmc.club/download ) 下载构建好的 Spigot Jar，放入到服务器插件中。

### 2. 配置文件

启动服务器生成配置文件。配置文件位于 /plugins/Fumi-Spigot/config.yml

在配置中填入对应的 NATS 的服务器地址和 Token；可以使用我社提供的 nats://web.sitmc.club:4222。如果对于信息安全有所顾虑，可以自行部署 NATS 服务器。

subject 是设置转发对应的频道， subject 相同的频道内消息会相互转发，subject 也可以是子节点的形式，例如设置为sitmc.chat。 

source-name 则是用于标注自身的身份，即消息来源。

所有消息都会以 {"source":"","message":"","username":""} 的格式发送到 NATS 服务器对应的 subject 交由其他客户端解析。

chat-format 是消息发送到游戏内的格式，变量为 {source}、{username}、{message}，支持原生的颜色代码。

forward-player-chat 可以设置为是否转发消息，但是实际上可以直接把插件禁用停止转发。

注意：如果两个服务器的 subject 相同，那么它们之间的消息也会被相互转发，你也可以用这个办法将多个服务器连接在一起。

```
nats:
  url: "nats://web.sitmc.club:4222"
  token: ""
  subject: "minecraft"

minecraft:
  source-name: "minecraft"
  chat-format: "&e[{source}] {username}: {message}"
  forward-player-chat: true
```

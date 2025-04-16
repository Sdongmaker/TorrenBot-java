# TorrentBot 配置说明

本文档详细说明了 TorrentBot 的配置参数，帮助您正确设置和使用该系统。TorrentBot 是一个结合了 Telegram 机器人、qBittorrent 和 Alist 的自动化文件下载和整理系统。

## 配置概览

配置分为两个主要部分：`BotOptions` 和 `AlistOptions`。前者负责配置 Telegram 机器人和 qBittorrent 下载客户端，后者负责配置 Alist 文件管理系统。

## BotOptions 配置详解

```json
"BotOptions": {
"Admins": "1111",
"BotToken": "xxx:xxx",
"RedisConnectionString": "127.0.0.1:6379,password=redis_x1237KTcQ,ssl=false,abortConnect=false",
"QbHostUrl": "http://127.0.0.1:8080",
"QbUsername": "admin",
"QbPassword": "admin",
"QbTag": "tg-download",
"QbCategory": "DS",
"QbDownloadPath": "/home/root/qbittorrent/Downloads/DS"
}
```

### 参数说明

- **Admins**: Telegram 管理员用户 ID，只有该 ID 的用户才能控制机器人。可以设置多个管理员（用逗号分隔）。

- **BotToken**: Telegram 机器人的访问令牌，通过 [@BotFather](https://t.me/BotFather) 创建机器人时获取。

- **RedisConnectionString**: Redis 数据库连接字符串，用于存储机器人状态和临时数据。
  - 格式：`服务器地址:端口,password=密码,ssl=是否启用SSL,abortConnect=连接失败时是否终止`

- **QbHostUrl**: qBittorrent WebUI 的访问地址，包括协议（http/https）、IP 和端口。

- **QbUsername**: qBittorrent 的登录用户名。

- **QbPassword**: qBittorrent 的登录密码。

- **QbTag**: 下载任务的默认标签，用于标识由机器人创建的下载任务。

- **QbCategory**: 下载任务的默认分类，用于在 qBittorrent 中组织下载内容。

- **QbDownloadPath**: qBittorrent 的默认下载路径，文件会被下载到此路径下。

## AlistOptions 配置详解

```json
"AlistOptions": {
  "Host": "http://127.0.0.1:5244",
  "Username": "admin",
  "Password": "admin",
  "SrcPath": "/下载/",
  "TargetPath": "/media/google/default"
}
```

### 参数说明

- **Host**: Alist 服务的访问地址，包括协议（http/https）、IP 和端口。

- **Username**: Alist 的登录用户名。

- **Password**: Alist 的登录密码。

- **SrcPath**: 源路径，表示下载完成的文件在 Alist 中的路径。

- **TargetPath**: 目标路径，表示文件整理后要移动到的 Alist 路径。

## 工作流程

1. 用户通过 Telegram 向机器人发送种子文件或磁力链接
2. 机器人将下载任务添加到 qBittorrent 并标记为 `QbTag`
3. 下载完成后，FileCopy 服务检测到完成的任务
4. 大文件（默认超过 1GB）会被复制从 `SrcPath` 到 `TargetPath`
5. 复制完成后，种子会被标记为"已整理"

## 安全建议

- 请妥善保管您的密码和令牌信息
- 建议将 qBittorrent 和 Alist 服务设置在内网或使用防火墙限制访问
- 定期更改密码以提高安全性
- 使用强密码并避免在多处使用相同的密码

## 注意事项

- Redis 连接字符串中的 `ssl=false` 表示不使用 SSL 加密连接，如果在公网环境使用，建议启用 SSL
- qBittorrent 的 WebUI 需要先在设置中启用才能使用
- 确保 Alist 中的路径格式正确，通常以 `/` 开头和结尾
- `QbDownloadPath` 需要与 qBittorrent 配置文件中的下载路径一致

## TODO 功能清单

- **MongoDB 集成**
  - 对接 MongoDB 数据库
  - 记录用户信息与种子下载历史
  - 建立下载任务与用户的关联关系

- **用户通知系统**
  - 下载完成后通过 Telegram 私信通知用户
  - 入库成功后发送通知消息
  - 可选择提供下载详情（文件大小、下载用时等）

- **资源管理优化**
  - 下载完成并整理后自动删除 qBittorrent 中的任务
  - 释放硬盘空间，避免重复存储
  - 添加定期清理临时文件的功能

- **媒体库集成**
  - 对接 Emby/Jellyfin 媒体服务器
  - 下载完成后自动触发媒体库扫描任务
  - 入库成功后通过机器人发送确认消息

- **重复检测机制**
  - 下载前检查媒体库中是否已存在相同视频
  - 如已存在则不添加新的下载任务
  - 向用户反馈媒体已存在的信息和观看链接

- **高级搜索功能**
  - 集成外部种子搜索引擎 API（如 Jackett、Prowlarr）
  - 通过关键词直接搜索并下载资源
  - 支持按类型（电影、剧集）、分辨率和年份等筛选

- **订阅追剧功能**
  - 支持用户订阅电视剧或动漫系列
  - 自动下载新发布的剧集
  - 集成 Sonarr/Radarr 进行智能媒体管理

- **用户权限管理**
  - 多级用户权限系统（管理员、普通用户、VIP 用户）
  - 基于权限限制下载数量和优先级
  - 支持邀请码注册机制

- **资源元数据增强**
  - 自动获取媒体详细信息（剧情、演员、评分）
  - 整合 TMDB/IMDB/豆瓣等数据源
  - 提供更丰富的媒体信息展示

- **多语言支持**
  - 支持多种语言界面（中文、英文等）
  - 自动检测字幕并下载匹配字幕
  - 支持用户设置首选语言

- **系统监控与统计**
  - 提供系统资源使用情况监控
  - 统计下载量、用户活跃度等数据
  - 发送定期报告给管理员

- **频道自动下载**
  - 监控特定 Telegram 频道的新资源
  - 自动下载符合条件的种子文件
  - 支持自定义过滤规则

- **WebUI 管理界面**
  - 开发 Web 管理界面，提供图形化操作
  - 查看下载历史和当前状态
  - 调整系统设置和管理用户

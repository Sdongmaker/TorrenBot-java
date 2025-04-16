package tdck.qdz.torrentbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tdck.qdz.torrentbot.event.AddTorrentEvent;
import tdck.qdz.torrentbot.event.ListTasksEvent;
import tdck.qdz.torrentbot.model.QbTorrent;
import tdck.qdz.torrentbot.model.TorrentTask;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 服务类，用于处理种子任务相关的业务逻辑。
 * 包括任务的保存、查询、状态更新以及定时检查下载状态等功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TorrentTaskService {
    /**
     * 注入的MongoTemplate对象，用于与MongoDB数据库进行交互。
     */
    private final MongoTemplate mongoTemplate;

    /**
     * 注入的qBittorrent服务对象，用于与qBittorrent进行交互。
     */
    private final QbService qbService;

    /**
     * 注入的通知服务对象，用于发送消息通知。
     */
    private final NotificationService notificationService;

    /**
     * 处理添加种子事件，将种子任务保存到数据库并尝试更新任务的哈希值。
     *
     * @param event 添加种子事件对象，包含磁力链接、用户ID和聊天ID等信息
     */
    @EventListener
    public void handleAddTorrentEvent(AddTorrentEvent event) {
        TorrentTask task = new TorrentTask();
        task.setMagnetUrl(event.getMagnetUrl());
        task.setUserId(event.getUserId());
        task.setChatId(event.getChatId());
        task.setStatus(TorrentTask.TaskStatus.PENDING);
        task.setCreateTime(LocalDateTime.now());
        
        // 先保存任务，获取ID
        saveTask(task);
        log.info("通过事件添加任务: {}", task);
        
        // 等待一段时间，让qBittorrent处理种子
        try {
            Thread.sleep(3000);
            
            // 尝试根据磁力链接或最近添加的任务查找对应的种子hash
            String hash = findTorrentHash(event.getMagnetUrl());
            if (hash != null) {
                updateTaskHash(task.getId(), hash);
                log.info("更新任务hash成功: id={}, hash={}", task.getId(), hash);
            } else {
                log.warn("无法找到任务对应的种子hash: {}", task);
            }
        } catch (Exception e) {
            log.error("更新任务hash失败: {}", e.getMessage());
        }
    }
    
    /**
     * 处理列出任务事件，查询所有任务并发送任务列表到指定聊天会话。
     *
     * @param event 列出任务事件对象，包含聊天ID等信息
     */
    @EventListener
    public void handleListTasksEvent(ListTasksEvent event) {
        try {
            List<TorrentTask> tasks = getAllTasks();
            StringBuilder sb = new StringBuilder();
            
            if (tasks.isEmpty()) {
                sb.append("暂无下载任务。");
            } else {
                sb.append("下载任务列表：\n");
                for (TorrentTask task : tasks) {
                    sb.append(String.format("名称：%s\n状态：%s\n创建时间：%s\n\n",
                            task.getName(),
                            task.getStatus(),
                            task.getCreateTime()));
                }
            }
            
            notificationService.sendMessage(event.getChatId(), sb.toString());
        } catch (Exception e) {
            log.error("获取任务列表失败", e);
            notificationService.sendMessage(event.getChatId(), "获取任务列表失败，请稍后重试。");
        }
    }

    /**
     * 保存种子任务到数据库。
     *
     * @param task 要保存的种子任务对象
     */
    public void saveTask(TorrentTask task) {
        mongoTemplate.save(task);
        log.info("保存下载任务: {}", task);
    }

    /**
     * 查询所有种子任务。
     *
     * @return 所有种子任务的列表
     */
    public List<TorrentTask> getAllTasks() {
        return mongoTemplate.findAll(TorrentTask.class);
    }

    /**
     * 根据哈希值查询种子任务。
     *
     * @param hash 种子任务的哈希值
     * @return 匹配的种子任务对象，如果未找到则返回null
     */
    public TorrentTask getTaskByHash(String hash) {
        Query query = new Query(Criteria.where("hash").is(hash));
        return mongoTemplate.findOne(query, TorrentTask.class);
    }

    /**
     * 根据文件名查询种子任务。
     *
     * @param fileName 种子任务的文件名
     * @return 匹配的种子任务对象，如果未找到则返回null
     */
    public TorrentTask getTaskByFileName(String fileName) {
        Query query = new Query(Criteria.where("fileName").is(fileName));
        return mongoTemplate.findOne(query, TorrentTask.class);
    }

    /**
     * 更新种子任务的状态。
     *
     * @param hash   种子任务的哈希值
     * @param status 要更新的任务状态
     */
    public void updateTaskStatus(String hash, TorrentTask.TaskStatus status) {
        Query query = new Query(Criteria.where("hash").is(hash));
        Update update = new Update()
                .set("status", status)
                .set("updateTime", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, TorrentTask.class);
        log.info("更新任务状态: hash={}, status={}", hash, status);
    }

    /**
     * 更新种子任务的哈希值。
     *
     * @param id   种子任务的ID
     * @param hash 要更新的哈希值
     */
    public void updateTaskHash(String id, String hash) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update()
                .set("hash", hash)
                .set("updateTime", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, TorrentTask.class);
        log.info("更新任务hash: id={}, hash={}", id, hash);
    }

    /**
     * 定时检查下载状态，每30秒执行一次。
     * 遍历所有待处理的任务，更新其状态并处理下载完成的任务。
     */
    @Scheduled(fixedRate = 30000)
    public void checkDownloadStatus() {
        List<TorrentTask> pendingTasks = mongoTemplate.findAll(TorrentTask.class);
        for (TorrentTask task : pendingTasks) {
            try {
                // 如果hash为空，跳过此任务
                if (task.getHash() == null || task.getHash().isEmpty()) {
                    continue;
                }
                
                QbTorrent torrent = qbService.getTorrent(task.getHash());
                if (torrent != null) {
                    updateTaskStatus(task, torrent);
                }
            } catch (Exception e) {
                log.error("检查下载状态失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 更新种子任务的状态信息，并检查是否下载完成。
     *
     * @param task    种子任务对象
     * @param torrent qBittorrent中的种子信息
     */
    private void updateTaskStatus(TorrentTask task, QbTorrent torrent) {
        // 更新任务信息
        task.setName(torrent.getName());
        task.setSize(torrent.getSize());
        task.setDownloadSpeed(torrent.getDownloadSpeed());
        task.setSeeders(torrent.getSeeders());
        task.setLeechers(torrent.getLeechers());
        task.setSavePath(torrent.getSavePath());
        task.setUpdateTime(LocalDateTime.now());

        // 检查是否下载完成
        if ("completed".equals(torrent.getState()) && task.getStatus() != TorrentTask.TaskStatus.COMPLETED) {
            task.setStatus(TorrentTask.TaskStatus.COMPLETED);
            task.setCompletionTime(LocalDateTime.now());
            task.setDownloadTime(Duration.between(task.getCreateTime(), task.getCompletionTime()).getSeconds());
            
            // 发送下载完成通知
            sendDownloadCompleteNotification(task);
        }

        mongoTemplate.save(task);
    }

    /**
     * 发送下载完成通知到指定聊天会话。
     *
     * @param task 下载完成的种子任务对象
     */
    private void sendDownloadCompleteNotification(TorrentTask task) {
        String message = String.format(
            "✅ 下载完成通知\n\n" +
            "名称: %s\n" +
            "大小: %s\n" +
            "用时: %s\n" +
            "平均速度: %s/s\n" +
            "保存路径: %s",
            task.getName(),
            formatSize(task.getSize()),
            formatDuration(task.getDownloadTime()),
            formatSize(task.getDownloadSpeed()),
            task.getSavePath()
        );

        notificationService.sendMessage(String.valueOf(task.getChatId()), message);
    }

    /**
     * 格式化文件大小为可读的字符串形式。
     *
     * @param bytes 文件大小（字节）
     * @return 格式化后的文件大小字符串
     */
    private String formatSize(Long bytes) {
        if (bytes == null) return "0 B";
        if (bytes < 1024) return String.valueOf(bytes) + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 格式化时间为可读的字符串形式。
     *
     * @param seconds 时间（秒）
     * @return 格式化后的时间字符串
     */
    private String formatDuration(Long seconds) {
        if (seconds == null) return "0秒";
        if (seconds < 60) return String.valueOf(seconds) + "秒";
        if (seconds < 3600) return String.format("%d分%d秒", seconds / 60, seconds % 60);
        return String.format("%d小时%d分%d秒", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    /**
     * 根据磁力链接或最近添加的任务查找对应的种子哈希值。
     *
     * @param magnetUrl 磁力链接
     * @return 找到的种子哈希值，如果未找到则返回null
     * @throws IOException 如果获取种子列表时发生IO异常，则抛出此异常
     */
    private String findTorrentHash(String magnetUrl) throws IOException {
        // 获取所有种子
        List<QbTorrent> torrents = qbService.getTorrents();
        
        // 如果有磁力链接，可以尝试从中提取hash
        if (magnetUrl != null && !magnetUrl.isEmpty()) {
            // 磁力链接中的hash格式通常是：magnet:?xt=urn:btih:HASH
            Pattern pattern = Pattern.compile("magnet:\\?xt=urn:btih:([a-zA-Z0-9]+)");
            Matcher matcher = pattern.matcher(magnetUrl);
            if (matcher.find()) {
                String expectedHash = matcher.group(1).toLowerCase();
                
                // 在qBittorrent中查找匹配的种子
                for (QbTorrent torrent : torrents) {
                    if (torrent.getHash().toLowerCase().startsWith(expectedHash)) {
                        return torrent.getHash();
                    }
                }
            }
        }
        
        // 如果无法从磁力链接提取hash，则尝试获取最近添加的种子
        if (!torrents.isEmpty()) {
            // 按添加时间排序，获取最新的种子
            QbTorrent latestTorrent = torrents.stream()
                    .sorted((t1, t2) -> Long.compare(t2.getAddedOn(), t1.getAddedOn()))
                    .findFirst()
                    .orElse(null);
            
            if (latestTorrent != null) {
                return latestTorrent.getHash();
            }
        }
        
        return null;
    }
} 
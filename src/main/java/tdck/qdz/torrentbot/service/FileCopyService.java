package tdck.qdz.torrentbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tdck.qdz.torrentbot.config.AlistConfig;
import tdck.qdz.torrentbot.config.BotConfig;
import tdck.qdz.torrentbot.event.DownloadCompleteEvent;
import tdck.qdz.torrentbot.model.QbTorrent;
import tdck.qdz.torrentbot.model.TorrentTask;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 服务类，用于处理文件复制相关的业务逻辑。
 * 包括监听下载完成事件、解析文件名、执行文件复制操作以及定期检查下载任务等。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileCopyService {
    private final QbService qbService;
    private final @Lazy TorrentTaskService torrentTaskService;
    private final AlistConfig alistConfig;
    private final BotConfig botConfig;
    private final NotificationService notificationService;

    /**
     * 大文件的阈值大小（1GB），超过此大小的文件将被视为大文件并单独处理。
     */
    private static final long LARGE_FILE_SIZE = 1024 * 1024 * 1024; // 1GB

    /**
     * 正则表达式模式，用于从消息中提取文件名。
     * 匹配规则：以"文件名: "开头，捕获后续内容直到换行符或字符串结束。
     */
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("文件名: (.+?)(?=\\n|$)");

    /**
     * 监听下载完成事件，解析文件名并执行文件复制操作。
     *
     * @param event 下载完成事件对象，包含聊天ID和下载完成的消息内容
     */
    @EventListener
    public void handleDownloadComplete(DownloadCompleteEvent event) {
        try {
            String message = event.getMessage();
            String chatId = event.getChatId();
            
            // 解析消息中的文件名
            String fileName = parseFileName(message);
            if (fileName == null) {
                log.error("无法从消息中解析文件名: {}", message);
                return;
            }

            // 获取任务信息
            TorrentTask task = torrentTaskService.getTaskByFileName(fileName);
            if (task == null) {
                log.error("未找到对应的任务信息: {}", fileName);
                return;
            }

            // 执行文件复制
            copyFile(task.getSourcePath(), task.getTargetPath());

            // 发送完成通知
            sendNotification(chatId, "文件复制完成: " + fileName);
        } catch (Exception e) {
            log.error("处理下载完成事件失败", e);
        }
    }

    /**
     * 解析消息中的文件名。
     *
     * @param message 下载完成的消息内容
     * @return 提取的文件名，若无法解析则返回null
     */
    private String parseFileName(String message) {
        Matcher matcher = FILE_NAME_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 执行文件复制操作。
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @throws IOException 如果文件复制过程中发生IO异常，则抛出此异常
     */
    private void copyFile(String sourcePath, String targetPath) throws IOException {
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);

        if (!Files.exists(source)) {
            throw new IOException("源文件不存在: " + sourcePath);
        }

        // 确保目标目录存在
        Files.createDirectories(target.getParent());

        // 复制文件
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("文件复制完成: {} -> {}", sourcePath, targetPath);
    }

    /**
     * 发送通知消息到指定的聊天会话。
     *
     * @param chatId  聊天ID
     * @param message 要发送的通知内容
     */
    private void sendNotification(String chatId, String message) {
        notificationService.sendMessage(chatId, message);
        log.info("发送通知到 {}: {}", chatId, message);
    }

    /**
     * 定期检查下载任务，并对符合条件的任务执行文件复制操作。
     * 每分钟执行一次，检查所有处于"uploading"或"pausedUP"状态的任务。
     */
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void checkAndCopyFiles() {
        try {
            List<QbTorrent> torrents = qbService.getTorrents();
            for (QbTorrent torrent : torrents) {
                if (torrent.getState().equals("uploading") || torrent.getState().equals("pausedUP")) {
                    TorrentTask task = torrentTaskService.getTaskByHash(torrent.getHash());
                    if (task != null && task.getStatus() != TorrentTask.TaskStatus.ORGANIZED) {
                        copyFiles(torrent, task);
                    }
                }
            }
        } catch (IOException e) {
            log.error("检查下载任务失败", e);
        }
    }

    /**
     * 复制指定种子任务中的大文件到目标路径。
     *
     * @param torrent 种子任务对象
     * @param task    任务信息对象
     */
    private void copyFiles(QbTorrent torrent, TorrentTask task) {
        try {
            Path sourcePath = Paths.get(torrent.getSavePath());
            Path targetPath = Paths.get(alistConfig.getTargetPath());

            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.size() > LARGE_FILE_SIZE) {
                        Path targetFile = targetPath.resolve(sourcePath.relativize(file));
                        Files.createDirectories(targetFile.getParent());
                        Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        log.info("复制大文件: {} -> {}", file, targetFile);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            torrentTaskService.updateTaskStatus(torrent.getHash(), TorrentTask.TaskStatus.ORGANIZED);
            log.info("文件整理完成: {}", torrent.getName());
        } catch (IOException e) {
            log.error("复制文件失败: {}", torrent.getName(), e);
        }
    }
} 
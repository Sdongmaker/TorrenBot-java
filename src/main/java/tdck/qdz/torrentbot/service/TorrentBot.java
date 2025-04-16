package tdck.qdz.torrentbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import tdck.qdz.torrentbot.config.BotConfig;
import tdck.qdz.torrentbot.config.TelegramBotConfig;
import tdck.qdz.torrentbot.model.QbTorrent;
import tdck.qdz.torrentbot.model.TorrentTask;
import tdck.qdz.torrentbot.event.DownloadCompleteEvent;
import tdck.qdz.torrentbot.event.AddTorrentEvent;
import tdck.qdz.torrentbot.event.ListTasksEvent;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 服务类，用于实现Telegram Bot的核心功能。
 * 该类继承自TelegramLongPollingBot，负责处理用户消息、执行命令、添加下载任务等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TorrentBot extends TelegramLongPollingBot {
    /**
     * 注入的Bot配置对象，包含Bot相关的配置信息。
     */
    private final BotConfig botConfig;

    /**
     * 注入的qBittorrent服务对象，用于与qBittorrent进行交互。
     */
    private final QbService qbService;

    /**
     * 注入的事件发布器，用于发布和处理应用事件。
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 正则表达式模式，用于匹配磁力链接。
     */
    private static final Pattern MAGNET_PATTERN = Pattern.compile("^magnet:\\?xt=urn:btih:[a-zA-Z0-9]{40}.*$");

    /**
     * 获取Bot的用户名。
     *
     * @return Bot的用户名
     */
    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    /**
     * 获取Bot的令牌。
     *
     * @return Bot的令牌
     */
    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    /**
     * 处理接收到的用户更新（消息）。
     *
     * @param update 包含用户消息的Update对象
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            String chatId = update.getMessage().getChatId().toString();
            String userId = update.getMessage().getFrom().getId().toString();

            // 检查是否是管理员
            if (!isAdmin(userId)) {
                sendMessage(chatId, "抱歉，您没有权限使用此机器人。");
                return;
            }

            // 处理命令
            if (update.getMessage().hasText() && update.getMessage().getText().startsWith("/")) {
                handleCommand(chatId, update.getMessage().getText());
                return;
            }

            // 处理磁力链接
            if (update.getMessage().hasText() && isMagnetLink(update.getMessage().getText())) {
                handleMagnetLink(chatId, update.getMessage().getText(), userId);
                return;
            }

            // 处理种子文件
            if (update.getMessage().hasDocument()) {
                handleTorrentFile(chatId, update.getMessage().getDocument(), userId);
                return;
            }

            // 发布下载完成事件
            if (update.getMessage().hasText() && update.getMessage().getText().contains("✅ 下载完成通知")) {
                eventPublisher.publishEvent(new DownloadCompleteEvent(this, chatId, update.getMessage().getText()));
            }

            sendMessage(chatId, "请发送磁力链接或种子文件，或输入 /help 查看可用命令。");
        }
    }

    /**
     * 检查用户是否为管理员。
     *
     * @param userId 用户ID
     * @return 如果用户是管理员则返回true，否则返回false
     */
    private boolean isAdmin(String userId) {
        return botConfig.getAdmins().contains(userId);
    }

    /**
     * 检查文本是否为有效的磁力链接。
     *
     * @param text 要检查的文本
     * @return 如果是有效的磁力链接则返回true，否则返回false
     */
    private boolean isMagnetLink(String text) {
        return MAGNET_PATTERN.matcher(text).matches();
    }

    /**
     * 处理用户输入的命令。
     *
     * @param chatId  聊天ID
     * @param command 用户输入的命令
     */
    private void handleCommand(String chatId, String command) {
        switch (command) {
            case "/start":
                sendMessage(chatId, "欢迎使用 TorrentBot！\n输入 /help 查看可用命令。");
                break;
            case "/help":
                sendMessage(chatId, "可用命令：\n" +
                        "/start - 开始使用机器人\n" +
                        "/help - 显示帮助信息\n" +
                        "/status - 查看当前下载状态\n" +
                        "/list - 列出所有下载任务");
                break;
            case "/status":
                try {
                    String statusMessage = getStatusMessage(qbService.getTorrents());
                    sendMessage(chatId, statusMessage);
                } catch (IOException e) {
                    log.error("获取下载状态失败", e);
                    sendMessage(chatId, "获取下载状态失败，请稍后重试。");
                }
                break;
            case "/list":
                // 发布一个事件，请求获取任务列表
                eventPublisher.publishEvent(new ListTasksEvent(this, chatId));
                break;
            default:
                sendMessage(chatId, "未知命令，请输入 /help 查看可用命令。");
        }
    }

    /**
     * 生成当前下载任务的状态信息。
     *
     * @param torrents 当前的下载任务列表
     * @return 包含状态信息的字符串
     * @throws IOException 如果获取任务列表时发生IO异常，则抛出此异常
     */
    private String getStatusMessage(List<QbTorrent> torrents) throws IOException {
        if (torrents.isEmpty()) {
            return "当前没有下载任务。";
        } else {
            StringBuilder sb = new StringBuilder("当前下载任务：\n");
            for (QbTorrent torrent : torrents) {
                sb.append(String.format("名称：%s\n进度：%.2f%%\n状态：%s\n\n",
                        torrent.getName(),
                        torrent.getProgress() * 100,
                        torrent.getState()));
            }
            return sb.toString();
        }
    }

    /**
     * 处理磁力链接，添加到qBittorrent并发布事件。
     *
     * @param chatId   聊天ID
     * @param magnetUrl 磁力链接
     * @param userId   用户ID
     */
    private void handleMagnetLink(String chatId, String magnetUrl, String userId) {
        // 发布添加Torrent事件，而不是直接调用TorrentTaskService
        Long userIdLong = Long.valueOf(userId);
        Long chatIdLong = Long.valueOf(chatId);
        
        // 添加到qBittorrent并发布事件
        qbService.addTorrent(magnetUrl);
        eventPublisher.publishEvent(new AddTorrentEvent(this, magnetUrl, userIdLong, chatIdLong));
        
        sendMessage(chatId, "已添加下载任务，请使用 /status 查看下载状态。");
    }

    /**
     * 处理种子文件，下载文件并添加到qBittorrent。
     *
     * @param chatId   聊天ID
     * @param document 用户发送的种子文件
     * @param userId   用户ID
     */
    private void handleTorrentFile(String chatId, Document document, String userId) {
        try {
            // 下载种子文件
            java.io.File torrentFile = downloadFile(document);
            
            // 发布添加Torrent事件，而不是直接调用TorrentTaskService
            Long userIdLong = Long.valueOf(userId);
            Long chatIdLong = Long.valueOf(chatId);
            
            // 添加到qBittorrent并发布事件
            qbService.addTorrent(torrentFile);
            eventPublisher.publishEvent(new AddTorrentEvent(this, null, userIdLong, chatIdLong));
            
            sendMessage(chatId, "已添加下载任务，请使用 /status 查看下载状态。");
        } catch (Exception e) {
            log.error("添加种子文件失败", e);
            sendMessage(chatId, "添加下载任务失败，请稍后重试。");
        }
    }

    /**
     * 下载Telegram中的文件。
     *
     * @param document 要下载的文件对象
     * @return 下载后的本地文件
     * @throws TelegramApiException 如果获取文件信息时发生异常，则抛出此异常
     * @throws IOException          如果下载文件时发生IO异常，则抛出此异常
     */
    private java.io.File downloadFile(Document document) throws TelegramApiException, IOException {
        // 获取文件信息
        GetFile getFile = new GetFile();
        getFile.setFileId(document.getFileId());
        File file = execute(getFile);
        
        // 下载文件
        String fileUrl = file.getFileUrl(getBotToken());
        java.io.File localFile = new java.io.File("temp/" + document.getFileName());
        localFile.getParentFile().mkdirs();
        
        try (InputStream in = new java.net.URL(fileUrl).openStream()) {
            Files.copy(in, localFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        
        return localFile;
    }

    /**
     * 发送消息到指定的聊天会话。
     *
     * @param chatId 聊天ID
     * @param text   要发送的消息内容
     */
    void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
            log.info("发送消息 - 聊天ID: {}, 内容: {}", chatId, text);
        } catch (TelegramApiException e) {
            log.error("发送消息失败", e);
        }
    }
} 
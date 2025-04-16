package tdck.qdz.torrentbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 服务类，用于处理通知相关的业务逻辑。
 * 提供发送消息到指定聊天会话的功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    /**
     * 注入的Telegram Bot实例，用于发送消息。
     */
    private final TorrentBot torrentBot;

    /**
     * 发送消息到指定的聊天会话。
     *
     * @param chatId  聊天ID，标识目标聊天会话
     * @param message 要发送的消息内容
     */
    public void sendMessage(String chatId, String message) {
        torrentBot.sendMessage(chatId, message);
        log.info("发送通知到 {}: {}", chatId, message);
    }
}
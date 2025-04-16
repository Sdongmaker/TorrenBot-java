package tdck.qdz.torrentbot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import tdck.qdz.torrentbot.service.TorrentBot;

/**
 * 配置类，用于初始化和注册Telegram Bot。
 * 该类负责创建TelegramBotsApi实例，并将TorrentBot注册到Telegram平台。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TelegramBotConfig {
    /**
     * 注入的Bot配置对象，包含Bot相关的配置信息。
     */
    private final BotConfig botConfig;

    /**
     * 创建并注册Telegram Bot的Bean。
     * 该方法初始化TelegramBotsApi实例，并将TorrentBot注册到Telegram平台。
     *
     * @param torrentBot 要注册的TorrentBot实例
     * @return 初始化后的TelegramBotsApi实例
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(TorrentBot torrentBot) {
        try {
            // 创建TelegramBotsApi实例，使用DefaultBotSession作为会话类型
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            // 将TorrentBot注册到Telegram平台
            botsApi.registerBot(torrentBot);
            log.info("Telegram Bot 注册成功");
            return botsApi;
        } catch (TelegramApiException e) {
            // 捕获并记录注册失败的异常
            log.error("Telegram Bot 注册失败", e);
            // 抛出运行时异常，确保注册失败时应用程序能够立即停止
            throw new RuntimeException("Telegram Bot 注册失败", e);
        }
    }
}
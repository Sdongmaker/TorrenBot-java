package tdck.qdz.torrentbot.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 事件类，用于表示下载完成的事件。
 * 包含聊天ID和下载完成的消息内容等信息。
 */
@Getter
public class DownloadCompleteEvent extends ApplicationEvent {
    /**
     * 聊天ID，标识触发事件的聊天会话。
     */
    private final String chatId;

    /**
     * 下载完成的消息内容，通常包含下载结果或相关信息。
     */
    private final String message;

    /**
     * 构造函数，初始化事件对象。
     *
     * @param source  事件源对象
     * @param chatId  聊天ID
     * @param message 下载完成的消息内容
     */
    public DownloadCompleteEvent(Object source, String chatId, String message) {
        super(source);
        this.chatId = chatId;
        this.message = message;
    }
}
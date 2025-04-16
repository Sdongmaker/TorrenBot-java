package tdck.qdz.torrentbot.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 事件类，用于表示添加种子的事件。
 * 包含种子的磁力链接、用户ID和聊天ID等信息。
 */
@Getter
public class AddTorrentEvent extends ApplicationEvent {
    /**
     * 种子的磁力链接。
     */
    private final String magnetUrl;

    /**
     * 用户ID，标识触发事件的用户。
     */
    private final Long userId;

    /**
     * 聊天ID，标识触发事件的聊天会话。
     */
    private final Long chatId;

    /**
     * 构造函数，初始化事件对象。
     *
     * @param source    事件源对象
     * @param magnetUrl 种子的磁力链接
     * @param userId    用户ID
     * @param chatId    聊天ID
     */
    public AddTorrentEvent(Object source, String magnetUrl, Long userId, Long chatId) {
        super(source);
        this.magnetUrl = magnetUrl;
        this.userId = userId;
        this.chatId = chatId;
    }
}
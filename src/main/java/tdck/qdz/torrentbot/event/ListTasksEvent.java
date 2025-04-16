package tdck.qdz.torrentbot.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 事件类，用于表示列出任务的事件。
 * 包含聊天ID等信息，用于标识触发事件的聊天会话。
 */
@Getter
public class ListTasksEvent extends ApplicationEvent {
    /**
     * 聊天ID，标识触发事件的聊天会话。
     */
    private final String chatId;

    /**
     * 构造函数，初始化事件对象。
     *
     * @param source 事件源对象
     * @param chatId 聊天ID
     */
    public ListTasksEvent(Object source, String chatId) {
        super(source);
        this.chatId = chatId;
    }
}
package tdck.qdz.torrentbot.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 模型类，用于表示种子任务的详细信息。
 * 包含任务的基本信息（如哈希值、名称、状态等）以及下载详情（如文件大小、下载速度等）。
 */
@Data
@Document(collection = "torrent_tasks")
public class TorrentTask {
    /**
     * 主键ID，唯一标识每个种子任务。
     */
    @Id
    private String id;

    /**
     * 种子的唯一标识符（哈希值），用于区分不同的种子任务。
     */
    private String hash;

    /**
     * 种子的名称，通常由用户或系统生成。
     */
    private String name;

    /**
     * 种子的磁力链接，用于从网络下载种子文件。
     */
    private String magnetUrl;

    /**
     * 用户ID，标识触发任务的用户。
     */
    private Long userId;

    /**
     * 聊天ID，标识触发任务的聊天会话。
     */
    private Long chatId;

    /**
     * 任务状态，表示任务当前的运行状态。
     * 可选值包括：PENDING（待处理）、DOWNLOADING（下载中）、COMPLETED（已完成）、FAILED（失败）、ORGANIZED（已整理）。
     */
    private TaskStatus status;

    /**
     * 任务的创建时间，记录任务被创建的时间点。
     */
    private LocalDateTime createTime;

    /**
     * 任务的更新时间，记录任务最后一次状态更新的时间点。
     */
    private LocalDateTime updateTime;

    /**
     * 错误信息，当任务状态为FAILED时，记录失败的具体原因。
     */
    private String errorMessage;

    // 下载详情

    /**
     * 文件大小（字节），表示种子文件的容量。
     */
    private Long size;

    /**
     * 下载用时（秒），表示种子从开始下载到完成所需的时间。
     */
    private Long downloadTime;

    /**
     * 平均下载速度（字节/秒），表示下载速率。
     */
    private Long downloadSpeed;

    /**
     * 做种者数量，表示正在上传该种子的用户数。
     */
    private Integer seeders;

    /**
     * 下载者数量，表示正在下载该种子的用户数。
     */
    private Integer leechers;

    /**
     * 保存路径，表示种子文件在服务器上的存储位置。
     */
    private String savePath;

    /**
     * 完成时间，表示种子完成下载的时间点。
     */
    private LocalDateTime completionTime;

    /**
     * 文件名，表示种子文件的名称。
     */
    private String fileName;

    /**
     * 源路径，表示文件操作前的原始路径。
     */
    private String sourcePath;

    /**
     * 目标路径，表示文件操作后的目标路径。
     */
    private String targetPath;

    /**
     * 枚举类型，用于表示任务的状态。
     * - PENDING：待处理
     * - DOWNLOADING：下载中
     * - COMPLETED：已完成
     * - FAILED：失败
     * - ORGANIZED：已整理
     */
    public enum TaskStatus {
        PENDING,
        DOWNLOADING,
        COMPLETED,
        FAILED,
        ORGANIZED
    }
}
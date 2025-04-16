package tdck.qdz.torrentbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.List;

/**
 * 模型类，用于表示Qbittorrent中的种子任务信息。
 * 包含种子的哈希值、名称、大小、进度、下载速度、上传速度等详细信息。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QbTorrent {
    /**
     * 种子的唯一标识符（哈希值），用于区分不同的种子任务。
     */
    @JsonProperty("hash")
    private String hash;

    /**
     * 种子的名称，通常由用户或系统生成。
     */
    @JsonProperty("name")
    private String name;

    /**
     * 种子的总大小（字节），表示种子文件的容量。
     */
    @JsonProperty("size")
    private long size;

    /**
     * 种子的下载进度（百分比），范围为0到100。
     */
    @JsonProperty("progress")
    private double progress;

    /**
     * 种子的当前下载速度（字节/秒），表示下载速率。
     */
    @JsonProperty("dlspeed")
    private long downloadSpeed;

    /**
     * 种子的当前上传速度（字节/秒），表示上传速率。
     */
    @JsonProperty("upspeed")
    private long uploadSpeed;

    /**
     * 种子的剩余完成时间（秒），表示预计完成下载所需的时间。
     */
    @JsonProperty("eta")
    private long eta;

    /**
     * 种子的状态，例如"downloading"、"paused"等，表示种子当前的运行状态。
     */
    @JsonProperty("state")
    private String state;

    /**
     * 种子的分类标签，用于对种子进行归类管理。
     */
    @JsonProperty("category")
    private String category;

    /**
     * 种子的标签列表，用于进一步细化种子的分类。
     */
    @JsonProperty("tags")
    @JsonDeserialize(using = TagsDeserializer.class)
    private List<String> tags;

    /**
     * 种子的保存路径，表示种子文件在服务器上的存储位置。
     */
    @JsonProperty("save_path")
    private String savePath;

    /**
     * 种子的当前连接的Leechers数量，表示正在下载该种子的用户数。
     */
    @JsonProperty("num_leechs")
    private int leechers;

    /**
     * 种子的当前连接的Seeders数量，表示正在上传该种子的用户数。
     */
    @JsonProperty("num_seeds")
    private int seeders;

    /**
     * 种子的活跃时间（秒），表示种子从开始下载到现在的持续时间。
     */
    @JsonProperty("time_active")
    private long timeActive;

    /**
     * 种子的共享比率，表示上传量与下载量的比例。
     */
    @JsonProperty("ratio")
    private double ratio;

    /**
     * 种子的添加时间戳（秒），表示种子被添加到Qbittorrent的时间。
     */
    @JsonProperty("added_on")
    private long addedOn;

    /**
     * 种子的完成时间戳（秒），表示种子完成下载的时间。
     */
    @JsonProperty("completion_on")
    private long completionOn;

    /**
     * 种子的最后活动时间戳（秒），表示种子最后一次有数据传输的时间。
     */
    @JsonProperty("last_activity")
    private long lastActivity;
}
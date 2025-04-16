package tdck.qdz.torrentbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类，用于存储Qbittorrent相关配置信息。
 * 包括Qbittorrent服务器地址、用户名、密码、标签、分类和下载路径等。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "qb.options")
public class QbConfig {
    /**
     * Qbittorrent服务器的主机地址。
     */
    private String host;

    /**
     * Qbittorrent服务器的用户名。
     */
    private String username;

    /**
     * Qbittorrent服务器的密码。
     */
    private String password;

    /**
     * 下载任务的标签。
     */
    private String tag;

    /**
     * 下载任务的分类。
     */
    private String category;

    /**
     * 下载任务的目标路径。
     */
    private String downloadPath;
}
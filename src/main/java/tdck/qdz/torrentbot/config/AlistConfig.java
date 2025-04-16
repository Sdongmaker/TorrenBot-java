package tdck.qdz.torrentbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类，用于存储Alist相关配置信息。
 * 包括Alist服务器地址、用户名、密码以及文件路径等。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alist.options")
public class AlistConfig {
    /**
     * Alist服务器的主机地址。
     */
    private String host;

    /**
     * Alist服务器的用户名。
     */
    private String username;

    /**
     * Alist服务器的密码。
     */
    private String password;

    /**
     * 源文件路径，表示需要操作的文件所在路径。
     */
    private String srcPath;

    /**
     * 目标文件路径，表示文件操作完成后存放的路径。
     */
    private String targetPath;
}
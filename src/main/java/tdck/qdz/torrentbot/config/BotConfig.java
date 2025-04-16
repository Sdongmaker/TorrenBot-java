package tdck.qdz.torrentbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "bot.options")
public class BotConfig {
    private String admins;
    private String token;
    private String username;
} 
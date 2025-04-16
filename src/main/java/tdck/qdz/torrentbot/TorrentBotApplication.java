package tdck.qdz.torrentbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TorrentBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TorrentBotApplication.class, args);
    }

}

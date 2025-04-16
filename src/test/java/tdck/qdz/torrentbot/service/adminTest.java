package tdck.qdz.torrentbot.service;

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class adminTest {

    @Autowired
    private QbService qbService;
    @Test
    public void test() {
        System.out.println("test");
        // 进行qb连接测试
        try {
            qbService.ensureLoggedIn();
            System.out.println("qb连接成功");
        } catch (Exception e) {
            System.out.println("qb连接失败");
            e.printStackTrace();
        }
        // 添加磁力链接
        try {
            qbService.addTorrent("magnet:?xt=urn:btih:D24AD559494EDB2F73A143739242120EFD84155F");
            System.out.println("添加磁力链接成功");
        }catch (Exception e)
            {
            System.out.println("添加磁力链接失败");
            e.printStackTrace();
        }
    }
}

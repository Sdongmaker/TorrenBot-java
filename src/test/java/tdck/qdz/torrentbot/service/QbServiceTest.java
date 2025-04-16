package tdck.qdz.torrentbot.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tdck.qdz.torrentbot.config.QbConfig;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class QbServiceTest {

    @Autowired
    private QbService qbService;

    @Autowired
    private QbConfig qbConfig;

    @Test
    void testLogin() {
        try {
            // 测试登录
            qbService.login();
            
            // 验证登录状态
            assertNotNull(qbService.getTorrents(), "登录后应该能获取种子列表");
            
            // 测试登出
            qbService.logout();
            
            // 验证登出后无法获取种子列表
            assertThrows(IOException.class, () -> qbService.getTorrents(), "登出后应该无法获取种子列表");
            
        } catch (IOException e) {
            fail("测试失败: " + e.getMessage());
        }
    }

    @Test
    void testConfig() {
        // 验证配置是否正确加载
        assertNotNull(qbConfig.getHost(), "主机地址不能为空");
        assertNotNull(qbConfig.getUsername(), "用户名不能为空");
        assertNotNull(qbConfig.getPassword(), "密码不能为空");
        assertNotNull(qbConfig.getDownloadPath(), "下载路径不能为空");
        
        // 打印配置信息（不包含密码）
        System.out.println("qBittorrent 配置信息:");
        System.out.println("主机: " + qbConfig.getHost());
        System.out.println("用户名: " + qbConfig.getUsername());
        System.out.println("下载路径: " + qbConfig.getDownloadPath());
        System.out.println("分类: " + qbConfig.getCategory());
        System.out.println("标签: " + qbConfig.getTag());
    }
} 
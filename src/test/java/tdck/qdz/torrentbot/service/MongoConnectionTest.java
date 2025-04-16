package tdck.qdz.torrentbot.service;

import com.mongodb.MongoCommandException;
import com.mongodb.MongoSecurityException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class MongoConnectionTest {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void testMongoConnection() {
        System.out.println("测试 MongoDB 连接...");
        System.out.println("连接 URI: " + mongoUri.replace(":WtQxKK", ":******"));

        try (MongoClient mongoClient = MongoClients.create(mongoUri)) {
            // 测试连接
            MongoDatabase database = mongoClient.getDatabase("BotDB");
            System.out.println("成功连接到数据库: " + database.getName());

            // 测试集合
            System.out.println("数据库中的集合:");
            database.listCollectionNames().forEach(System.out::println);

            // 测试写入
            System.out.println("测试写入操作...");
            mongoTemplate.save(new TestDocument("test", "测试文档"));
            System.out.println("写入成功");

            // 测试读取
            System.out.println("测试读取操作...");
            TestDocument doc = mongoTemplate.findById("test", TestDocument.class);
            assertNotNull(doc, "应该能读取到测试文档");
            System.out.println("读取成功: " + doc);

            // 测试删除
            System.out.println("测试删除操作...");
            mongoTemplate.remove(doc);
            System.out.println("删除成功");

        } catch (MongoSecurityException e) {
            System.err.println("MongoDB 认证失败: " + e.getMessage());
            System.err.println("请检查用户名和密码是否正确");
            fail("MongoDB 认证失败: " + e.getMessage());
        } catch (MongoCommandException e) {
            System.err.println("MongoDB 命令执行失败: " + e.getMessage());
            System.err.println("错误代码: " + e.getErrorCode());
            System.err.println("错误名称: " + e.getErrorCodeName());
            fail("MongoDB 命令执行失败: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("MongoDB 连接测试失败: " + e.getMessage());
            e.printStackTrace();
            fail("MongoDB 连接测试失败: " + e.getMessage());
        }
    }

    // 测试文档类
    static class TestDocument {
        private String id;
        private String content;

        public TestDocument(String id, String content) {
            this.id = id;
            this.content = content;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String toString() {
            return "TestDocument{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                '}';
        }
    }
} 
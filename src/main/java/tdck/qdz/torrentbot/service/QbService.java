package tdck.qdz.torrentbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;
import tdck.qdz.torrentbot.config.QbConfig;
import tdck.qdz.torrentbot.model.QbTorrent;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 服务类，用于与qBittorrent进行交互。
 * 提供登录、登出、添加种子、获取种子信息等功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QbService {
    /**
     * 注入的qBittorrent配置对象，包含连接信息和用户凭据。
     */
    private final QbConfig qbConfig;

    /**
     * Jackson对象映射器，用于JSON序列化和反序列化。
     */
    private final ObjectMapper objectMapper;

    /**
     * Cookie存储，用于保存登录后的会话信息。
     */
    private final BasicCookieStore cookieStore = new BasicCookieStore();

    /**
     * HTTP客户端，用于发送HTTP请求。
     */
    private CloseableHttpClient client;

    /**
     * 会话ID，用于标识当前登录的会话。
     */
    private String sid;

    void ensureLoggedIn() throws IOException {
        if (client == null) {
            client = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
            login();
        }
    }

    /**
     * 登录到qBittorrent。
     * 使用配置中的用户名和密码进行身份验证，并保存会话信息。
     *
     * @throws IOException 如果登录过程中发生IO异常，则抛出此异常
     */
    public void login() throws IOException {
        try {
            log.info("开始登录 qBittorrent，主机: {}", qbConfig.getHost());
            
            // 清除之前的 cookie
            cookieStore.clear();
            
            HttpPost request = new HttpPost(qbConfig.getHost() + "/api/v2/auth/login");
            
            // 构建表单数据
            String formData = String.format("username=%s&password=%s", 
                qbConfig.getUsername(), 
                qbConfig.getPassword());
            
            log.debug("登录请求表单数据: {}", formData);
            
            request.setEntity(new StringEntity(formData, ContentType.APPLICATION_FORM_URLENCODED));
            
            // 设置请求头，确保 Referer 和 Origin 与 Host 完全匹配
            String host = qbConfig.getHost();
            request.setHeader("Host", host.replace("http://", ""));
            request.setHeader("Referer", host);
            request.setHeader("Origin", host);
            request.setHeader("User-Agent", "Mozilla/5.0");
            request.setHeader("Accept", "application/json");
            request.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            request.setHeader("Connection", "keep-alive");

            log.debug("发送登录请求...");
            try (CloseableHttpResponse response = client.execute(request)) {
                log.debug("收到登录响应，状态码: {}", response.getCode());
                
                // 读取响应内容
                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                log.debug("登录响应内容: {}", responseBody);
                
                if (response.getCode() == 200) {
                    // 检查响应内容是否为 "Fails."
                    if ("Fails.".equals(responseBody.trim())) {
                        log.error("qBittorrent 登录失败：用户名或密码错误");
                        throw new IOException("qBittorrent 登录失败：用户名或密码错误");
                    }
                    
                    // 获取并保存 SID cookie
                    List<Cookie> cookies = cookieStore.getCookies();
                    log.debug("获取到 cookies: {}", cookies);
                    
                    for (Cookie cookie : cookies) {
                        if ("SID".equals(cookie.getName())) {
                            sid = cookie.getValue();
                            log.info("qBittorrent 登录成功，SID: {}", sid);
                            break;
                        }
                    }
                    
                    if (sid == null) {
                        log.warn("未找到 SID cookie，登录可能不完整");
                        throw new IOException("登录成功但未获取到 SID cookie，请检查 qBittorrent 配置");
                    }
                } else {
                    log.error("qBittorrent 登录失败: {} - {}", response.getCode(), responseBody);
                    
                    // 检查是否是 IP 被封禁
                    if (responseBody.contains("身份认证失败次数过多") || responseBody.contains("IP 地址已被封禁")) {
                        log.error("qBittorrent IP 被封禁，请等待一段时间后再试");
                        throw new IOException("qBittorrent IP 被封禁，请等待一段时间后再试");
                    }
                    
                    // 检查是否是用户名或密码错误
                    if (responseBody.contains("用户名或密码错误")) {
                        log.error("qBittorrent 用户名或密码错误");
                        throw new IOException("qBittorrent 用户名或密码错误");
                    }
                    
                    throw new IOException("qBittorrent 登录失败: " + response.getCode() + " - " + responseBody);
                }
            }
        } catch (Exception e) {
            log.error("qBittorrent 登录失败", e);
            throw e;
        }
    }

    /**
     * 从qBittorrent登出。
     * 清除会话信息并关闭HTTP客户端。
     *
     * @throws IOException 如果登出过程中发生IO异常，则抛出此异常
     */
    public void logout() throws IOException {
        try {
            ensureLoggedIn();
            HttpPost request = new HttpPost(qbConfig.getHost() + "/api/v2/auth/logout");
            request.setHeader("Referer", qbConfig.getHost());
            request.setHeader("Origin", qbConfig.getHost());

            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getCode() == 200) {
                    log.info("qBittorrent 登出成功");
                    sid = null;
                    cookieStore.clear();
                } else {
                    throw new IOException("qBittorrent 登出失败: " + response.getCode());
                }
            }
        } catch (Exception e) {
            log.error("qBittorrent 登出失败", e);
            throw e;
        }
    }

    /**
     * 向qBittorrent添加磁力链接。
     *
     * @param magnetUrl 磁力链接
     * @return 如果添加成功则返回true，否则返回false
     */
    public boolean addTorrent(String magnetUrl) {
        try {
            ensureLoggedIn();
            HttpPost request = new HttpPost(qbConfig.getHost() + "/api/v2/torrents/add");
            
            // 构建表单数据
            String formData = String.format("urls=%s&category=%s&tags=%s&savepath=%s", 
                magnetUrl,
                qbConfig.getCategory(),
                qbConfig.getTag(),
                qbConfig.getDownloadPath());
            
            request.setEntity(new StringEntity(formData, ContentType.APPLICATION_FORM_URLENCODED));
            request.setHeader("Referer", qbConfig.getHost());
            request.setHeader("Origin", qbConfig.getHost());

            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getCode() == 200) {
                    log.info("添加种子成功: {}", magnetUrl);
                    return true;
                } else {
                    log.error("添加种子失败: {}", response.getCode());
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("添加种子失败", e);
            return false;
        }
    }

    /**
     * 向qBittorrent添加种子文件。
     *
     * @param torrentFile 种子文件
     * @return 如果添加成功则返回true，否则返回false
     */
    public boolean addTorrent(File torrentFile) {
        try {
            ensureLoggedIn();
            HttpPost request = new HttpPost(qbConfig.getHost() + "/api/v2/torrents/add");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("torrents", torrentFile, ContentType.APPLICATION_OCTET_STREAM, torrentFile.getName());
            builder.addTextBody("category", qbConfig.getCategory());
            builder.addTextBody("tags", qbConfig.getTag());
            builder.addTextBody("savepath", qbConfig.getDownloadPath());
            request.setEntity(builder.build());
            request.setHeader("Referer", qbConfig.getHost());
            request.setHeader("Origin", qbConfig.getHost());

            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getCode() == 200) {
                    log.info("添加种子文件成功: {}", torrentFile.getName());
                    return true;
                } else {
                    log.error("添加种子文件失败: {}", response.getCode());
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("添加种子文件失败", e);
            return false;
        }
    }

    /**
     * 从qBittorrent获取种子列表。
     *
     * @param filter 过滤器 (all, downloading, seeding, completed, paused, active, inactive, resumed, stalled)
     * @param category 分类
     * @param tag 标签
     * @param sort 排序字段
     * @param reverse 是否反向排序
     * @param limit 限制数量
     * @param offset 偏移量
     * @param hashes 哈希值列表，用 | 分隔
     * @return 种子列表
     * @throws IOException 如果获取种子列表过程中发生IO异常，则抛出此异常
     */
    public List<QbTorrent> getTorrents(String filter, String category, String tag, 
            String sort, Boolean reverse, Integer limit, Integer offset, String hashes) throws IOException {
        ensureLoggedIn();
        
        // 构建查询参数
        StringBuilder urlBuilder = new StringBuilder(qbConfig.getHost() + "/api/v2/torrents/info");
        boolean firstParam = true;
        
        if (filter != null) {
            urlBuilder.append(firstParam ? "?" : "&").append("filter=").append(filter);
            firstParam = false;
        }
        if (category != null) {
            urlBuilder.append(firstParam ? "?" : "&").append("category=").append(category);
            firstParam = false;
        }
        if (tag != null) {
            urlBuilder.append(firstParam ? "?" : "&").append("tag=").append(tag);
            firstParam = false;
        }
        if (sort != null) {
            urlBuilder.append(firstParam ? "?" : "&").append("sort=").append(sort);
            firstParam = false;
        }
        if (reverse != null) {
            urlBuilder.append(firstParam ? "?" : "&").append("reverse=").append(reverse);
            firstParam = false;
        }
        if (limit != null) {
            urlBuilder.append(firstParam ? "?" : "&").append("limit=").append(limit);
            firstParam = false;
        }
        if (offset != null) {
            urlBuilder.append(firstParam ? "?" : "&").append("offset=").append(offset);
            firstParam = false;
        }
        if (hashes != null) {
            urlBuilder.append(firstParam ? "?" : "&").append("hashes=").append(hashes);
        }
        
        HttpGet request = new HttpGet(urlBuilder.toString());
        request.setHeader("Referer", qbConfig.getHost());
        request.setHeader("Origin", qbConfig.getHost());
        request.setHeader("User-Agent", "Mozilla/5.0");
        request.setHeader("Accept", "application/json");
        request.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        request.setHeader("Connection", "keep-alive");
        
        try (CloseableHttpResponse response = client.execute(request)) {
            if (response.getCode() == 200) {
                QbTorrent[] torrents = objectMapper.readValue(response.getEntity().getContent(), QbTorrent[].class);
                return List.of(torrents);
            } else {
                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                log.error("获取种子列表失败: {} - {}", response.getCode(), responseBody);
                throw new IOException("获取种子列表失败: " + response.getCode() + " - " + responseBody);
            }
        }
    }

    /**
     * 获取qBittorrent中的所有种子列表。
     *
     * @return 种子列表
     * @throws IOException 如果获取种子列表过程中发生IO异常，则抛出此异常
     */
    public List<QbTorrent> getTorrents() throws IOException {
        return getTorrents(null, null, null, null, null, null, null, null);
    }

    /**
     * 获取qBittorrent中指定哈希值的种子信息。
     *
     * @param hash 种子的哈希值
     * @return 种子信息对象
     * @throws IOException 如果获取种子信息过程中发生IO异常，则抛出此异常
     */
    public QbTorrent getTorrent(String hash) throws IOException {
        ensureLoggedIn();
        HttpGet request = new HttpGet(qbConfig.getHost() + "/api/v2/torrents/properties?hash=" + hash);
        request.setHeader("Referer", qbConfig.getHost());
        request.setHeader("Origin", qbConfig.getHost());
        request.setHeader("User-Agent", "Mozilla/5.0");
        request.setHeader("Accept", "application/json");
        request.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        request.setHeader("Connection", "keep-alive");
        
        try (CloseableHttpResponse response = client.execute(request)) {
            if (response.getCode() == 200) {
                return objectMapper.readValue(response.getEntity().getContent(), QbTorrent.class);
            } else if (response.getCode() == 404) {
                throw new IOException("种子未找到: " + hash);
            } else {
                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                log.error("获取种子信息失败: {} - {}", response.getCode(), responseBody);
                throw new IOException("获取种子信息失败: " + response.getCode() + " - " + responseBody);
            }
        }
    }

    /**
     * 从qBittorrent中删除指定哈希值的种子。
     *
     * @param hash 种子的哈希值
     * @param deleteFiles 是否同时删除种子文件
     * @return 如果删除成功则返回true，否则返回false
     */
    public boolean deleteTorrent(String hash, boolean deleteFiles) {
        try {
            ensureLoggedIn();
            HttpPost request = new HttpPost(qbConfig.getHost() + "/api/v2/torrents/delete");
            
            // 构建表单数据
            String formData = String.format("hashes=%s&deleteFiles=%s", 
                hash,
                deleteFiles);
            
            request.setEntity(new StringEntity(formData, ContentType.APPLICATION_FORM_URLENCODED));
            request.setHeader("Referer", qbConfig.getHost());
            request.setHeader("Origin", qbConfig.getHost());

            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getCode() == 200) {
                    log.info("删除种子成功: {}", hash);
                    return true;
                } else {
                    log.error("删除种子失败: {}", response.getCode());
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("删除种子失败", e);
            return false;
        }
    }

    /**
     * 暂停qBittorrent中指定哈希值的种子。
     *
     * @param hash 种子的哈希值
     * @return 如果暂停成功则返回true，否则返回false
     */
    public boolean pauseTorrent(String hash) {
        try {
            ensureLoggedIn();
            HttpPost request = new HttpPost(qbConfig.getHost() + "/api/v2/torrents/pause");
            
            // 构建表单数据
            String formData = String.format("hashes=%s", hash);
            
            request.setEntity(new StringEntity(formData, ContentType.APPLICATION_FORM_URLENCODED));
            request.setHeader("Referer", qbConfig.getHost());
            request.setHeader("Origin", qbConfig.getHost());

            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getCode() == 200) {
                    log.info("暂停种子成功: {}", hash);
                    return true;
                } else {
                    log.error("暂停种子失败: {}", response.getCode());
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("暂停种子失败", e);
            return false;
        }
    }

    /**
     * 恢复qBittorrent中指定哈希值的种子。
     *
     * @param hash 种子的哈希值
     * @return 如果恢复成功则返回true，否则返回false
     */
    public boolean resumeTorrent(String hash) {
        try {
            ensureLoggedIn();
            HttpPost request = new HttpPost(qbConfig.getHost() + "/api/v2/torrents/resume");
            
            // 构建表单数据
            String formData = String.format("hashes=%s", hash);
            
            request.setEntity(new StringEntity(formData, ContentType.APPLICATION_FORM_URLENCODED));
            request.setHeader("Referer", qbConfig.getHost());
            request.setHeader("Origin", qbConfig.getHost());

            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getCode() == 200) {
                    log.info("恢复种子成功: {}", hash);
                    return true;
                } else {
                    log.error("恢复种子失败: {}", response.getCode());
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("恢复种子失败", e);
            return false;
        }
    }

    /**
     * 为qBittorrent中指定哈希值的种子添加标签。
     *
     * @param hash 种子的哈希值
     * @param tags 要添加的标签
     * @return 如果添加标签成功则返回true，否则返回false
     */
    public boolean addTags(String hash, String tags) {
        try {
            ensureLoggedIn();
            HttpPost request = new HttpPost(qbConfig.getHost() + "/api/v2/torrents/addTags");
            
            // 构建表单数据
            String formData = String.format("hashes=%s&tags=%s", hash, tags);
            
            request.setEntity(new StringEntity(formData, ContentType.APPLICATION_FORM_URLENCODED));
            request.setHeader("Referer", qbConfig.getHost());
            request.setHeader("Origin", qbConfig.getHost());

            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getCode() == 200) {
                    log.info("添加标签成功: {} -> {}", hash, tags);
                    return true;
                } else {
                    log.error("添加标签失败: {}", response.getCode());
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("添加标签失败", e);
            return false;
        }
    }

    /**
     * 从qBittorrent中指定哈希值的种子移除标签。
     *
     * @param hash 种子的哈希值
     * @param tags 要移除的标签
     * @return 如果移除标签成功则返回true，否则返回false
     */
    public boolean removeTags(String hash, String tags) {
        try {
            ensureLoggedIn();
            HttpPost request = new HttpPost(qbConfig.getHost() + "/api/v2/torrents/removeTags");
            
            // 构建表单数据
            String formData = String.format("hashes=%s&tags=%s", hash, tags);
            
            request.setEntity(new StringEntity(formData, ContentType.APPLICATION_FORM_URLENCODED));
            request.setHeader("Referer", qbConfig.getHost());
            request.setHeader("Origin", qbConfig.getHost());

            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getCode() == 200) {
                    log.info("移除标签成功: {} -> {}", hash, tags);
                    return true;
                } else {
                    log.error("移除标签失败: {}", response.getCode());
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("移除标签失败", e);
            return false;
        }
    }
} 
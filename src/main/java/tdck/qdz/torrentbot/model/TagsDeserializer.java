package tdck.qdz.torrentbot.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义反序列化器，用于将JSON格式的标签数据解析为List<String>。
 * 支持两种格式的输入：
 * 1. 单个标签字符串（如："tag1"）。
 * 2. 标签数组（如：["tag1", "tag2"]）。
 */
public class TagsDeserializer extends JsonDeserializer<List<String>> {

    /**
     * 反序列化方法，将JSON格式的标签数据解析为List<String>。
     *
     * @param p    JsonParser对象，用于读取JSON数据流。
     * @param ctxt DeserializationContext对象，提供反序列化上下文信息。
     * @return 解析后的标签列表。
     * @throws IOException 如果解析过程中发生IO异常，则抛出此异常。
     */
    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        List<String> tags = new ArrayList<>();
        
        // 检查当前JSON令牌类型
        if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
            // 处理单个标签字符串
            String tag = p.getValueAsString();
            if (tag != null && !tag.isEmpty()) {
                tags.add(tag);
            }
        } else if (p.getCurrentToken() == JsonToken.START_ARRAY) {
            // 处理标签数组
            while (p.nextToken() != JsonToken.END_ARRAY) {
                if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
                    String tag = p.getValueAsString();
                    if (tag != null && !tag.isEmpty()) {
                        tags.add(tag);
                    }
                }
            }
        }
        
        return tags;
    }
}
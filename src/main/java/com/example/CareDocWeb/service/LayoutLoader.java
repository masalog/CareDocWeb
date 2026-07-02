package com.example.CareDocWeb.service;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * 座標YAMLを読み込み、各フィールドの描画位置を保持する。
 *
 * <p>InputStreamは try-with-resources で確実にクローズし、
 * リソースリークを防止する。</p>
 */
public class LayoutLoader {

    private LayoutLoader() {
    }

    public static Map<String, FieldPosition> loadLayout() {
        Yaml yaml = new Yaml();
        try (InputStream is = LayoutLoader.class.getResourceAsStream("/positions/converted_positions.yaml")) {
            if (is == null) {
                throw new IllegalStateException("座標YAMLファイルが見つかりません: /positions/converted_positions.yaml");
            }

            Map<String, Object> root = yaml.load(is);
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> fields = (Map<String, Map<String, Object>>) root.get("fields");

            return fields.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            e -> new FieldPosition(
                                    ((Number) e.getValue().get("x")).floatValue(),
                                    ((Number) e.getValue().get("y")).floatValue(),
                                    ((Number) e.getValue().get("fontSize")).floatValue()
                            )
                    ));
        } catch (IOException ex) {
            throw new IllegalStateException("座標YAMLファイルの読み込みに失敗しました", ex);
        }
    }

    public record FieldPosition(float x, float y, float fontSize) {
    }
}

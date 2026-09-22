package top.hotwaterflask.blockbolt.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Tag;

/**
 * 一个“材料集合”：既可以记录单个方块，也可以记录方块标签（例如
 * {@code #minecraft:wooden_doors}）。标签里的方块会被展开到内部集合中，
 * 因此判断某个方块是否受保护时速度不受影响。
 */
final class MaterialSet {
    // 用于把集合重新写回配置文件（保留标签写法，不展开）
    private final Set<Material> materials = new HashSet<>();
    private final Set<Tag<Material>> tags = new HashSet<>();

    // 用于快速查找（标签已展开为具体方块）
    private final Set<Material> flattened = new HashSet<>();

    /**
     * 判断集合中是否存在该方块，标签里的方块同样算数。
     *
     * @param material 要检查的方块。
     * @return 存在返回 true，否则返回 false。
     */
    boolean contains(Material material) {
        return flattened.contains(material);
    }

    /**
     * 添加单个方块。
     *
     * @param material 要添加的方块。
     */
    void addMaterial(Material material) {
        materials.add(material);
        flattened.add(material);
    }

    /**
     * 添加一个方块标签，标签中的所有方块都会被展开并加入集合。
     *
     * @param tag 要添加的标签。
     */
    void addTag(Tag<Material> tag) {
        tags.add(tag);
        flattened.addAll(tag.getValues());
    }

    /**
     * 获取集合中的全部方块（标签已展开）。返回的集合不可修改。
     *
     * @return 展开后的方块集合。
     * @see #toConfigStringList() 写回配置文件时使用，会保留标签写法。
     */
    Collection<Material> getAllFlattened() {
        return Collections.unmodifiableSet(flattened);
    }

    /**
     * 转换成可写回配置文件的字符串列表。标签会保留为
     * {@code "#minecraft:标签名"} 的写法，不会展开成一堆方块名。
     *
     * @return 字符串列表（已排序）。
     */
    List<String> toConfigStringList() {
        List<String> result = new ArrayList<>();
        for (Material material : materials) {
            result.add(material.getKey().toString());
        }
        for (Tag<Material> tag : tags) {
            result.add("#" + tag.getKey());
        }
        result.sort(null);
        return result;
    }
}
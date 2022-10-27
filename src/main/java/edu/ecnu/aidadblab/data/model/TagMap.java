package edu.ecnu.aidadblab.data.model;

import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@ToString
public class TagMap {

    private Map<Vertex, List<Integer>> vertexTag;

    public Map<Integer, Vertex> tagMap;

    public TagMap(int queryCount) {
        vertexTag = new HashMap<>(queryCount);
        tagMap = new HashMap<>(queryCount);
    }

    public void addTag(Vertex vertex, int tag) {
        tagMap.put(tag, vertex);
        if (!vertexTag.containsKey(vertex)) {
            vertexTag.put(vertex, new ArrayList<>());
        }
        vertexTag.get(vertex).add(tag);
    }

    public void removeTag(Vertex vertex, int tag) {
        tagMap.remove(tag);
        vertexTag.get(vertex).remove(Integer.valueOf(tag));
        if (vertexTag.get(vertex).isEmpty()) {
            vertexTag.remove(vertex);
        }
    }

    public List<Integer> getVertexTags(Vertex vertex) {
        return vertexTag.get(vertex);
    }

    public boolean isLackTag(int tag) {
        return !tagMap.containsKey(tag);
    }

    public TagMap copy() {
        TagMap newTagMap = new TagMap();
        newTagMap.vertexTag = new HashMap<>();
        for (Vertex vertex : vertexTag.keySet()) {
            newTagMap.vertexTag.put(vertex, new ArrayList<>(this.vertexTag.get(vertex)));
        }
        newTagMap.tagMap = new HashMap<>(this.tagMap);
        return newTagMap;
    }
}

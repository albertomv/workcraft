package org.workcraft.dom.references;

import org.workcraft.annotations.Annotations;
import org.workcraft.dom.Connection;
import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.exceptions.ArgumentException;
import org.workcraft.util.TwoWayMap;

import java.util.HashMap;
import java.util.Map;

public class DefaultNameManager implements NameManager {

    private final Map<String, Integer> prefixCount = new HashMap<>();
    private final TwoWayMap<String, Node> nodes = new TwoWayMap<>();

    @Override
    public String getPrefix(Node node) {
        String result = Annotations.getIdentifierPrefix(node.getClass());
        if (result != null) {
            return result;
        }
        if (node instanceof Connection) return Identifier.createInternal("c");
        if (node instanceof Container) return Identifier.createInternal("group");
        return "node";
    }

    @Override
    public void setPrefixCount(String prefix, Integer count) {
        prefixCount.put(prefix, count);
    }

    @Override
    public Integer getPrefixCount(String prefix) {
        if (prefixCount.containsKey(prefix)) {
            return prefixCount.get(prefix);
        } else {
            return 0;
        }
    }

    @Override
    public void setName(Node node, String name) {
        Node occupant = getNode(name);
        if (node != occupant) {
            if (isUnusedName(name)) {
                if (Identifier.isName(name) || Identifier.isInternal(name)) {
                    nodes.removeValue(node);
                    nodes.put(name, node);
                } else {
                    throw new ArgumentException("The name '" + name + "' is invalid identifier.");
                }
            } else {
                throw new ArgumentException("The name '" + name + "' is already taken.");
            }
        }
    }

    @Override
    public String getName(Node node) {
        return nodes.getKey(node);
    }

    @Override
    public boolean isNamed(Node node) {
        return nodes.getKey(node) != null;
    }

    @Override
    public boolean isUnusedName(String name) {
        return getNode(name) == null;
    }

    @Override
    public Node getNode(String name) {
        return nodes.getValue(name);
    }

    @Override
    public void remove(Node node) {
        if (nodes.getKey(node) != null) {
            nodes.removeValue(node);
        }
    }

    @Override
    public void setDefaultNameIfUnnamed(Node node) {
        if (!nodes.containsValue(node)) {
            String prefix = getPrefix(node);
            Integer count = getPrefixCount(prefix);
            String name;
            do {
                name = prefix + count++;
            } while (!isUnusedName(name));
            setPrefixCount(prefix, count);
            nodes.put(name, node);
        }
    }

    private static String codeToString(int code) {
        String result = "";
        do {
            result += (char) ('a' + code % 26);
            code /= 26;
        } while (code > 0);
        return result;
    }

    @Override
    public String getDerivedName(Node node, String candidate) {
        String result = candidate;
        int code = 0;
        while (!isUnusedName(result)) {
            result = candidate + codeToString(code);
            code++;
        }
        return result;
    }

}

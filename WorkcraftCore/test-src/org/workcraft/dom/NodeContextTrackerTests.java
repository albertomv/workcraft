package org.workcraft.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;
import org.workcraft.dom.math.MathConnection;
import org.workcraft.dom.math.MathGroup;
import org.workcraft.dom.math.MathNode;

public class NodeContextTrackerTests {

    class MockNode extends MathNode {

    }

    @Test
    public void testInit() {
        MathGroup group = new MathGroup();

        MockNode n1 = new MockNode();
        MockNode n2 = new MockNode();
        MockNode n3 = new MockNode();
        MockNode n4 = new MockNode();

        MathConnection con1 = new MathConnection(n1, n3);
        MathConnection con2 = new MathConnection(n2, n3);
        MathConnection con3 = new MathConnection(n3, n4);

        group.add(con1);
        group.add(con2);
        group.add(con3);

        group.add(n1);
        group.add(n2);
        group.add(n3);
        group.add(n4);

        NodeContextTracker nct = new NodeContextTracker();
        nct.attach(group);

        Set<Node> n4pre = nct.getPreset(n4);
        assertEquals(n4pre.size(), 1);
        assertTrue(n4pre.contains(n3));

        Set<Node> n3pre = nct.getPreset(n3);
        assertEquals(n3pre.size(), 2);
        assertTrue(n3pre.contains(n1));
        assertTrue(n3pre.contains(n2));

        Set<Node> n3post = nct.getPostset(n3);
        assertEquals(n3post.size(), 1);
        assertTrue(n3post.contains(n4));

        Set<Node> n4post = nct.getPostset(n4);
        assertTrue(n4post.isEmpty());

        Set<Node> n1pre = nct.getPreset(n1);
        assertTrue(n1pre.isEmpty());
    }

    @Test
    public void testAddRemove1() {
        MathGroup group = new MathGroup();

        NodeContextTracker nct = new NodeContextTracker();
        nct.attach(group);

        MockNode n1 = new MockNode();
        MockNode n2 = new MockNode();
        MockNode n3 = new MockNode();
        MockNode n4 = new MockNode();

        MathConnection con1 = new MathConnection(n1, n3);
        MathConnection con2 = new MathConnection(n2, n3);
        MathConnection con3 = new MathConnection(n3, n4);

        group.add(con1);
        group.add(con2);
        group.add(con3);

        group.add(n1);
        group.add(n2);
        group.add(n3);
        group.add(n4);

        Set<Node> n4pre = nct.getPreset(n4);
        assertEquals(n4pre.size(), 1);
        assertTrue(n4pre.contains(n3));

        Set<Node> n3pre = nct.getPreset(n3);
        assertEquals(n3pre.size(), 2);
        assertTrue(n3pre.contains(n1));
        assertTrue(n3pre.contains(n2));

        Set<Node> n3post = nct.getPostset(n3);
        assertEquals(n3post.size(), 1);
        assertTrue(n3post.contains(n4));

        Set<Node> n4post = nct.getPostset(n4);
        assertTrue(n4post.isEmpty());

        Set<Node> n1pre = nct.getPreset(n1);
        assertTrue(n1pre.isEmpty());

        group.remove(n3);
        assertTrue(nct.getPreset(n1).isEmpty());
        assertTrue(nct.getPreset(n2).isEmpty());
        assertTrue(nct.getPreset(n4).isEmpty());

        assertTrue(nct.getPostset(n1).isEmpty());
        assertTrue(nct.getPostset(n2).isEmpty());
        assertTrue(nct.getPostset(n4).isEmpty());

        group.remove(con1);

        assertTrue(nct.getPreset(n1).isEmpty());
        assertTrue(nct.getPreset(n2).isEmpty());
        assertTrue(nct.getPreset(n4).isEmpty());

        assertTrue(nct.getPostset(n1).isEmpty());
        assertTrue(nct.getPostset(n2).isEmpty());
        assertTrue(nct.getPostset(n4).isEmpty());
    }

    @Test
    public void testAddRemove2() {
        MathGroup group = new MathGroup();

        NodeContextTracker nct = new NodeContextTracker();
        nct.attach(group);

        MockNode n1 = new MockNode();
        MockNode n2 = new MockNode();
        MockNode n3 = new MockNode();
        MockNode n4 = new MockNode();

        MathConnection con1 = new MathConnection(n1, n3);
        MathConnection con2 = new MathConnection(n2, n3);
        MathConnection con3 = new MathConnection(n3, n4);

        group.add(con1);
        group.add(con2);
        group.add(con3);

        group.add(n1);
        group.add(n2);
        group.add(n3);
        group.add(n4);

        Set<Node> n4pre = nct.getPreset(n4);
        assertEquals(n4pre.size(), 1);
        assertTrue(n4pre.contains(n3));

        Set<Node> n3pre = nct.getPreset(n3);
        assertEquals(n3pre.size(), 2);
        assertTrue(n3pre.contains(n1));
        assertTrue(n3pre.contains(n2));

        Set<Node> n3post = nct.getPostset(n3);
        assertEquals(n3post.size(), 1);
        assertTrue(n3post.contains(n4));

        Set<Node> n4post = nct.getPostset(n4);
        assertTrue(n4post.isEmpty());

        Set<Node> n1pre = nct.getPreset(n1);
        assertTrue(n1pre.isEmpty());

        group.remove(con1);

        assertTrue(nct.getPostset(n1).isEmpty());
        assertEquals(nct.getPreset(n3).size(), 1);

        group.remove(con2);
        assertTrue(nct.getPostset(n2).isEmpty());
        assertTrue(nct.getPreset(n3).isEmpty());

        assertTrue(nct.getPostset(n3).contains(n4));

        group.remove(n4);

        assertTrue(nct.getPostset(n3).isEmpty());
    }

}

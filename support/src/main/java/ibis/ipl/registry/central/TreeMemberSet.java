/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibis.ipl.registry.central;

import ibis.ipl.impl.IbisIdentifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TreeMemberSet implements MemberSet, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(TreeMemberSet.class);

    private class Node implements Serializable, Comparable<Node> {

        private static final long serialVersionUID = 1L;

        Node[] children;

        Member member;

        int index;

        Node() {
            index = getNextIndex();

            children = new Node[0];
            member = null;
        }

        public String toString() {
            return "node " + index + " member = " + member + " with "
                    + children.length + " children";
        }

        public int compareTo(Node o) {
            return index - o.index;
        }

    }

    // root node
    private ArrayList<Node> root;

    // all nodes, as an array
    private ArrayList<Node> list;

    // list of not currently used nodes
    private SortedSet<Node> spares;

    private int nextNodeIndex;

    private final Random random;

    private Node lastSearchResult;

    public TreeMemberSet() {
        nextNodeIndex = 0;

        root = new ArrayList<Node>();
        list = new ArrayList<Node>();
        spares = new TreeSet<Node>();

        random = new Random();
    }

    @SuppressWarnings("unchecked")
    public void init(DataInputStream in) throws IOException {
        long start = System.currentTimeMillis();

        logger.debug("initializing tree member set");

        int size = in.readInt();
        logger.debug("reading  " + size + " bytes");

        byte[] data = new byte[size];
        in.readFully(data);

        long read = System.currentTimeMillis();

        ObjectInputStream objectInput =
            new ObjectInputStream(new ByteArrayInputStream(data));

        long stream = System.currentTimeMillis();

        try {
            root = (ArrayList<Node>) objectInput.readObject();
            list = (ArrayList<Node>) objectInput.readObject();
            spares = (SortedSet<Node>) objectInput.readObject();
            nextNodeIndex = objectInput.readInt();
        } catch (ClassNotFoundException e) {
            throw new IOException("could not deserialize data for tree");
        }
        objectInput.close();

        long done = System.currentTimeMillis();

        logger.debug("TreeMemberSet.init(): read = " + (read - start)
                + ", stream = " + (stream - read) + ", done = "
                + (done - stream));

        if (logger.isDebugEnabled()) {
            logger.debug("initialized tree member set, content:" + toString());
        }

    }

    public void writeTo(DataOutputStream out) throws IOException {
        logger.debug("writing tree member set to stream");

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        ObjectOutputStream objectOutput = new ObjectOutputStream(byteStream);

        objectOutput.writeObject(root);
        objectOutput.writeObject(list);
        objectOutput.writeObject(spares);
        objectOutput.writeInt(nextNodeIndex);
        objectOutput.flush();
        objectOutput.close();

        logger.debug("writing " + byteStream.size() + " bytes");

        byte[] bytes = byteStream.toByteArray();

        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private Node createTree(int order, SortedSet<Node> spares) {
        logger.debug("creating tree of order " + order);

        Node result = new Node();
        spares.add(result);

        result.children = new Node[order];

        for (int i = 0; i < order; i++) {
            result.children[i] = createTree(i, spares);
        }

        return result;
    }

    private int getNextIndex() {
        return nextNodeIndex++;
    }

    private void expandTree() {
        int order = root.size();

        logger.debug("expanding tree with order " + order + " subtree");

        Node newTree = createTree(order, spares);

        root.add(newTree);
    }

    public void add(Member member) {
	if (logger.isDebugEnabled()) {
	    logger.debug("adding " + member + " to tree");
	}

        if (spares.isEmpty()) {
            expandTree();
        }

        Node node = spares.first();
        spares.remove(node);

        node.member = member;

        list.add(node);

        if (logger.isDebugEnabled()) {
            logger.debug("" + this);
        }
    }

    public Member remove(IbisIdentifier identifier) {
	if (logger.isDebugEnabled()) {
	    logger.debug("removing " + identifier + " from tree");
	}

        for (int i = 0; i < list.size(); i++) {
            Node node = list.get(i);
            if (node.member.getIbis().equals(identifier)) {
                Member result = node.member;
                node.member = null;
                list.remove(i);
                spares.add(node);

                if (logger.isDebugEnabled()) {
                    logger.debug("removed " + result + " from tree, result "
                            + this);
                }

                return result;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("could not remove " + identifier + ", not found in tree");
        }
        return null;
    }

    public Member[] asArray() {
        Member[] result = new Member[list.size()];

        for (int i = 0; i < result.length; i++) {
            result[i] = list.get(i).member;
        }

        return result;
    }

    public boolean contains(IbisIdentifier identifier) {
        for (Node node : list) {
            if (node.member.getIbis().equals(identifier)) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(Member member) {
        for (Node node : list) {
            if (node.member.getIbis().equals(member.getIbis())) {
                return true;
            }
        }
        return false;

    }

    public Member get(IbisIdentifier identifier) {
        for (Node node : list) {
            if (node.member.getIbis().equals(identifier)) {
                return node.member;
            }
        }
        return null;
    }
    
    public Member get(String name) {
        for (Node node : list) {
            if (node.member.getIbis().name().equals(name)) {
                return node.member;
            }
        }
        return null;
    }

    private Node getNode(IbisIdentifier identifier) {
        for (Node node : list) {
            if (node.member.getIbis().equals(identifier)) {
                return node;
            }
        }
        return null;
    }

    public Member get(int index) {
        return list.get(index).member;
    }

    public List<Event> getJoinEvents() {
        List<Event> result = new ArrayList<Event>();

        for (Node node : list) {
            result.add(node.member.getEvent());
        }

        return result;
    }

    public Member getLeastRecentlySeen() {
        if (list.isEmpty()) {
            return null;
        }

        Member oldest = list.get(0).member;

        for (int i = 1; i < list.size(); i++) {
            if (list.get(i).member.getTime() < oldest.getTime()) {
                oldest = list.get(i).member;
            }
        }

        return oldest;
    }

    public int getMinimumTime() {
        if (list.isEmpty()) {
            return -1;
        }

        int minimum = list.get(0).member.getCurrentTime();

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).member.getCurrentTime() < minimum) {
                minimum = list.get(i).member.getCurrentTime();
            }
        }
        return minimum;
    }

    public Member getRandom() {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(random.nextInt(size())).member;
    }

    public Member[] getRandom(int size) {
        ArrayList<Member> result = new ArrayList<Member>();
        BitSet added = new BitSet();

        if (size > list.size()) {
            for (Node node : list) {
                result.add(node.member);
            }
            return result.toArray(new Member[0]);
        }

        while (result.size() < size) {
            int next = random.nextInt(list.size());
            if (!added.get(next)) {
                // not added yet

                result.add(list.get(next).member);

                // remember we already added this member.
                added.set(next);
            }
        }

        return result.toArray(new Member[0]);
    }

    public int size() {
        return list.size();
    }

    // add the member of this node to the list. If this node has
    // no member, add it's children instead(recursive).
    private void addMembers(Node node, List<Member> result) {
        if (node == null) {
            return;
        }

        if (node.member != null) {
            result.add(node.member);
            return;
        }

        // this node does not have a member, add all children
        // in reverse order
        for (int i = node.children.length - 1; i >= 0; i--) {
            addMembers(node.children[i], result);
        }
    }

    public Member[] getChildren(IbisIdentifier ibis) {
	if (logger.isDebugEnabled()) {
	    logger.debug("getting children of " + ibis);
	}

        if (lastSearchResult == null || lastSearchResult.member == null
                || !lastSearchResult.member.getIbis().equals(ibis)) {

            lastSearchResult = getNode(ibis);

            if (lastSearchResult == null) {
                return new Member[0];
            }
        }

        ArrayList<Member> result = new ArrayList<Member>();

        // add all children (in reverse order)
        for (int i = lastSearchResult.children.length - 1; i >= 0; i--) {
            addMembers(lastSearchResult.children[i], result);
        }

        if (logger.isDebugEnabled()) {
            String message = "children of " + ibis + ":\n";
            for (Member member : result) {
                message += member + "\n";
            }

            logger.debug(message);
        }

        return result.toArray(new Member[0]);
    }

    public Member[] getRootChildren() {
        ArrayList<Member> result = new ArrayList<Member>();

        // add all children (in reverse order)
        for (int i = root.size() - 1; i >= 0; i--) {
            addMembers(root.get(i), result);
        }

        return result.toArray(new Member[0]);
    }

    private String printTree(Node node, int level) {
        if (node == null) {
            return "";
        }

        String result = "";

        for (int i = 0; i < level; i++) {
            result += "  ";
        }

        result += node + "\n";

        for (Node child : node.children) {
            result += printTree(child, level + 1);
        }

        return result;
    }

    public String toString() {
        String result = "\nROOT:\n";

        for (Node node : root) {
            result += printTree(node, 1);
        }

        result += "LIST:\n";
        for (Node node : list) {
            result += node + "\n";
        }

        result += "SPARES:\n";
        for (Node node : spares) {
            result += node + "\n";
        }

        result += "Children of root:\n";
        for (Member member : getRootChildren()) {
            result += member + "\n";
        }

        return result;
    }

}

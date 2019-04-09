package io.github.yangziwen.jacoco.util;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;

public class LineNumberNodeWrapper {

    private LineNumberNode node;

    private AbstractInsnNode next;

    private boolean ignored = true;

    public LineNumberNodeWrapper(LineNumberNode node) {
        this.node = node;
    }

    public int getLine() {
        return getNode().line;
    }

    public LineNumberNode getNode() {
        return node;
    }

    public AbstractInsnNode getNext() {
        return next;
    }

    public void setNext(AbstractInsnNode next) {
        this.next = next;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

}

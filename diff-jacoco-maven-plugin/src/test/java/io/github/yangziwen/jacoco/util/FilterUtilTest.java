package io.github.yangziwen.jacoco.util;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.jacoco.core.internal.analysis.filter.IFilterContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class FilterUtilTest {

    @Test
    public void getClassPathWithPackageName() {
        IFilterContext context = Mockito.mock(IFilterContext.class);
        Mockito.doReturn("com/example/Foo").when(context).getClassName();
        Mockito.doReturn("Foo.java").when(context).getSourceFileName();

        assertEquals("com/example/Foo.java", FilterUtil.getClassPath(context));
    }

    @Test
    public void getClassPathWithDefaultPackage() {
        IFilterContext context = Mockito.mock(IFilterContext.class);
        Mockito.doReturn("Foo").when(context).getClassName();
        Mockito.doReturn("Foo.java").when(context).getSourceFileName();

        assertEquals("Foo.java", FilterUtil.getClassPath(context));
    }

    @Test
    public void collectLineNumberNodeList() {
        InsnList instructions = new InsnList();
        LineNumberNode line10 = new LineNumberNode(10, new LabelNode());
        LineNumberNode line20 = new LineNumberNode(20, new LabelNode());
        VarInsnNode insn1 = new VarInsnNode(Opcodes.ASTORE, 1);
        VarInsnNode insn2 = new VarInsnNode(Opcodes.ASTORE, 2);
        instructions.add(line10);
        instructions.add(new LabelNode());
        instructions.add(insn1);
        instructions.add(line20);
        instructions.add(new LabelNode());
        instructions.add(insn2);

        List<LineNumberNodeWrapper> wrappers = FilterUtil.collectLineNumberNodeList(instructions);

        assertEquals(2, wrappers.size());
        assertEquals(10, wrappers.get(0).getLine());
        assertEquals(20, wrappers.get(1).getLine());
        assertSame(line20, wrappers.get(0).getNext());
        assertSame(instructions.getLast(), wrappers.get(1).getNext());
    }

}

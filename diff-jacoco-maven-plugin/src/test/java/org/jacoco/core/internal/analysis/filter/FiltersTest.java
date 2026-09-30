package org.jacoco.core.internal.analysis.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * shadow Filters 的注册表机制测试：addFilter 注册的 filter
 * 应在 all() 组装出的组合 filter 中生效（Kotlin / 非 Kotlin 类均生效）。
 */
public class FiltersTest {

    private IFilterContext mockContext(String className, String... annotations) {
        IFilterContext context = Mockito.mock(IFilterContext.class);
        Set<String> annotationSet = new HashSet<>();
        Collections.addAll(annotationSet, annotations);
        Mockito.doReturn(className).when(context).getClassName();
        Mockito.doReturn("java/lang/Object").when(context).getSuperClassName();
        Mockito.doReturn(annotationSet).when(context).getClassAnnotations();
        Mockito.doReturn(Collections.emptySet()).when(context).getClassAttributes();
        Mockito.doReturn(Opcodes.ACC_PUBLIC).when(context).getClassAccess();
        Mockito.doReturn("Foo.java").when(context).getSourceFileName();
        return context;
    }

    private MethodNode emptyMethod() {
        MethodNode methodNode = new MethodNode();
        methodNode.name = "filter";
        methodNode.desc = "()V";
        // ASM tree 类不会自动初始化集合字段，不初始化会被内置的
        // SynchronizedFilter（tryCatchBlocks）等以下标遍历时 NPE
        methodNode.instructions = new InsnList();
        methodNode.tryCatchBlocks = new ArrayList<>();
        return methodNode;
    }

    @Test
    public void registeredFilterIsInvokedForNonKotlinClass() {
        AtomicInteger invoked = new AtomicInteger();
        Filters.addFilter((methodNode, context, output) -> invoked.incrementAndGet());

        Filters.all().filter(emptyMethod(), mockContext("Foo"), Mockito.mock(IFilterOutput.class));

        assertEquals(1, invoked.get());
    }

    @Test
    public void registeredFilterIsInvokedForKotlinClass() {
        AtomicInteger invoked = new AtomicInteger();
        Filters.addFilter((methodNode, context, output) -> invoked.incrementAndGet());

        Filters.all().filter(
                emptyMethod(),
                mockContext("Foo", Filters.KOTLIN_METADATA_DESC),
                Mockito.mock(IFilterOutput.class));

        assertEquals(1, invoked.get());
    }

    @Test
    public void registeredFiltersAreInvokedOnEveryAllCall() {
        AtomicInteger invoked = new AtomicInteger();
        Filters.addFilter((methodNode, context, output) -> invoked.incrementAndGet());

        IFilter all = Filters.all();
        IFilterOutput output = Mockito.mock(IFilterOutput.class);
        all.filter(emptyMethod(), mockContext("Foo1"), output);
        all.filter(emptyMethod(), mockContext("Foo2"), output);
        Filters.all().filter(emptyMethod(), mockContext("Foo3"), output);

        assertEquals(3, invoked.get());
    }

    @Test
    public void addFilterRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> Filters.addFilter(null));
    }

}

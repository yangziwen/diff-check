package io.github.yangziwen.jacoco.maven;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jacoco.core.internal.analysis.filter.Filters;
import org.jacoco.maven.AgentMojo;

@Mojo(
        name = "diff-prepare-agent",
        defaultPhase = LifecyclePhase.INITIALIZE,
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        threadSafe = true
)
public class DiffAgentMojo extends AgentMojo {

    @Override
    public void executeMojo() {
        System.out.println("yes");
        try {
            hackFilters();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.executeMojo();
    }

    private static void hackFilters() throws Exception {
        Field field = Filters.class.getDeclaredField("ALL");
        field.setAccessible(true);
        Field modifiersField = field.getClass().getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        int modifiers = field.getModifiers();
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(Filters.class, Filters.NONE);
        modifiersField.setInt(field, modifiers);
        System.out.println(Filters.ALL);
    }

}

package io.github.yangziwen.checkstyle.filter;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.Filter;
import com.puppycrawl.tools.checkstyle.api.Violation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WholeDiffFilterTest {

    @Test
    public void acceptEmptyList() {
        Filter filter = new WholeDiffFilter(Collections.emptyList());
        AuditEvent event = buildAuditEvent();
        boolean result = filter.accept(event);
        assertFalse(result);
    }

    @Test
    public void acceptUnmatched() {
        List<String> checks = Collections.singletonList("something");
        Filter filter = new WholeDiffFilter(checks);
        AuditEvent event = buildAuditEvent();
        boolean result = filter.accept(event);
        assertFalse(result);
    }

    @Test
    public void acceptMatchOne() {
        List<String> checks = new ArrayList<>();
        checks.add("something");
        checks.add("java.lang.Object");
        Filter filter = new WholeDiffFilter(checks);
        AuditEvent event = buildAuditEvent();
        boolean result = filter.accept(event);
        assertTrue(result);
    }

    @Test
    public void acceptPartialMatch() {
        List<String> checks = Collections.singletonList("Object");
        Filter filter = new WholeDiffFilter(checks);
        AuditEvent event = buildAuditEvent();
        boolean result = filter.accept(event);
        assertTrue(result);
    }


    @Test
    public void acceptSingleMatch() {
        List<String> checks = Collections.singletonList("java.lang.Object");
        Filter filter = new WholeDiffFilter(checks);
        AuditEvent event = buildAuditEvent();
        boolean result = filter.accept(event);
        assertTrue(result);
    }

    private static AuditEvent buildAuditEvent() {
        Object source = new Object();
        Class<?> sourceClass = Object.class;
        Violation violation = new Violation(0, "", "", null, "", sourceClass, "");
        return new AuditEvent(source, "", violation);
    }
}
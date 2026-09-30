package io.github.yangziwen.checkstyle.filter;

import java.util.List;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.Filter;

public class WholeDiffFilter implements Filter {

    private final List<String> checkNames;

    public WholeDiffFilter(List<String> checks) {
        this.checkNames = checks;
    }

    /**
     * 精确匹配规则名：sourceName 与 checkName 全等（支持传全限定名），
     * 或 sourceName 的简单类名与 checkName 相等（即以 "." + checkName 结尾）。
     * 不用 contains 子串匹配，避免宽泛片段误伤同名族规则
     * （如 "Object" 命中所有含 Object 的规则、"Check" 命中所有规则）。
     */
    @Override
    public boolean accept(AuditEvent event) {
        String sourceName = event.getViolation().getSourceName();
        return checkNames.stream().anyMatch(checkName ->
                sourceName.equals(checkName) || sourceName.endsWith("." + checkName));
    }
}

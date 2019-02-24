package io.github.yangziwen.jacoco.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.jacoco.core.internal.analysis.filter.Filters;
import org.jacoco.core.internal.analysis.filter.IFilter;

public class FilterUtil {

    private FilterUtil() { }

    public static void appendFilter(IFilter filter) throws Exception {
        IFilter[] filters = getAllFilters();
        IFilter[] newFilters = new IFilter[filters.length + 1];
        System.arraycopy(filters, 0, newFilters, 0, filters.length);
        newFilters[newFilters.length - 1] = filter;
        setAllFilters(newFilters);
    }

    private static IFilter[] getAllFilters() throws Exception {
        Field filtersField = Filters.class.getDeclaredField("filters");
        filtersField.setAccessible(true);
        return (IFilter[]) filtersField.get(Filters.ALL);
    }

    private static void setAllFilters(IFilter[] filters) throws Exception {
        Field filtersField = Filters.class.getDeclaredField("filters");
        filtersField.setAccessible(true);
        Field modifiersField = filtersField.getClass().getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        int modifiers = filtersField.getModifiers();
        modifiersField.setInt(filtersField, modifiers & ~Modifier.FINAL);
        filtersField.set(Filters.ALL, filters);
        modifiersField.setInt(filtersField, modifiers);
    }

}

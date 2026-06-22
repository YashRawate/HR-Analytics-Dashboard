package com.nmdc.hranalytics.util;

import com.nmdc.hranalytics.model.Employee;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AnalyticsUtil {

    private AnalyticsUtil() { }

    public static final Set<String> EMPTY_LIKE = Set.of("", "undefined", "null", "None");

    /** A (label, count) pair, ordered by count descending — mirrors count_by()'s return value. */
    public record Count(String label, int count) { }

    /** Mirrors count_by(records, field): counts occurrences of a field, sorted desc by count. */
    public static List<Count> countBy(List<Employee> records, Function<Employee, String> field) {
        Map<String, Integer> counter = new LinkedHashMap<>();
        for (Employee r : records) {
            String v = field.apply(r);
            if (v != null && !EMPTY_LIKE.contains(v)) {
                counter.merge(v, 1, Integer::sum);
            }
        }
        List<Count> result = new ArrayList<>();
        for (var e : counter.entrySet()) result.add(new Count(e.getKey(), e.getValue()));
        result.sort((a, b) -> Integer.compare(b.count(), a.count()));
        return result;
    }

    /** Mirrors apply_dept_filter(): filters by department, expanding the 'C & IT' virtual bucket. */
    public static List<Employee> applyDeptFilter(List<Employee> records, String deptFilter) {
        if ("All".equals(deptFilter)) return records;
        if ("C & IT".equals(deptFilter)) {
            Set<String> citSet = Set.copyOf(Constants.CIT_DEPTS);
            return filter(records, r -> citSet.contains(r.getDepartment()));
        }
        return filter(records, r -> Objects.equals(r.getDepartment(), deptFilter));
    }

    public static List<Employee> filter(List<Employee> records, Predicate<Employee> pred) {
        List<Employee> out = new ArrayList<>();
        for (Employee r : records) if (pred.test(r)) out.add(r);
        return out;
    }
}

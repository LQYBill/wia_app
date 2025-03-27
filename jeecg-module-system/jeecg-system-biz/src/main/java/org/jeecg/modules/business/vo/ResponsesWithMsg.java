package org.jeecg.modules.business.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ResponsesWithMsg<S, E> {
    protected Map<S, List<String>> successes = new HashMap<>();
    protected Map<E, List<String>> failures = new HashMap<>();

    public void addSuccess(S data) {
        successes.putIfAbsent(data, new ArrayList<>());
    }
    public void addSuccess(S data, String message) {
        successes.computeIfAbsent(data, k -> new ArrayList<>()).add(message);
    }
    public void addSuccess(S data, List<String> messages) {
        successes.computeIfAbsent(data, k -> new ArrayList<>()).addAll(messages);
    }
    public void addFailure(E data) {
        failures.putIfAbsent(data, new ArrayList<>());
    }
    public void addFailure(E data, String message) {
        failures.computeIfAbsent(data, k -> new ArrayList<>()).add(message);
    }
    public void addFailure(E data, List<String> messages) {
        failures.computeIfAbsent(data, k -> new ArrayList<>()).addAll(messages);
    }
}

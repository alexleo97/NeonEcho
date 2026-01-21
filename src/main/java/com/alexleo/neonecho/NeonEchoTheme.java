package com.alexleo.neonecho;

import java.util.List;

public final class NeonEchoTheme {
    private final String name;
    private final String prefix;
    private final String joinMessage;
    private final List<String> netrunStartLines;
    private final List<String> netrunSuccessLines;
    private final List<String> netrunFailLines;
    private final List<String> netrunCooldownLines;
    private final List<String> netrunHintLines;

    public NeonEchoTheme(
            String name,
            String prefix,
            String joinMessage,
            List<String> netrunStartLines,
            List<String> netrunSuccessLines,
            List<String> netrunFailLines,
            List<String> netrunCooldownLines,
            List<String> netrunHintLines) {
        this.name = name;
        this.prefix = prefix;
        this.joinMessage = joinMessage;
        this.netrunStartLines = List.copyOf(netrunStartLines);
        this.netrunSuccessLines = List.copyOf(netrunSuccessLines);
        this.netrunFailLines = List.copyOf(netrunFailLines);
        this.netrunCooldownLines = List.copyOf(netrunCooldownLines);
        this.netrunHintLines = List.copyOf(netrunHintLines);
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getJoinMessage() {
        return joinMessage;
    }

    public List<String> getNetrunStartLines() {
        return netrunStartLines;
    }

    public List<String> getNetrunSuccessLines() {
        return netrunSuccessLines;
    }

    public List<String> getNetrunFailLines() {
        return netrunFailLines;
    }

    public List<String> getNetrunCooldownLines() {
        return netrunCooldownLines;
    }

    public List<String> getNetrunHintLines() {
        return netrunHintLines;
    }
}

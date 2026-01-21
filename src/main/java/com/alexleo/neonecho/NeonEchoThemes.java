package com.alexleo.neonecho;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NeonEchoThemes {
    private static final Map<String, NeonEchoTheme> THEMES;

    static {
        Map<String, NeonEchoTheme> themes = new HashMap<>();
        themes.put("neon", new NeonEchoTheme(
                "neon",
                "[NeonEcho]",
                "NeonEcho online. Welcome back, runner. Type /netrun to sync.",
                List.of(
                        "Netrun handshake ready. Tier {tier} | Risk {risk}.",
                        "Stage {stage}/{stages}. Code {code}. {seconds}s to breach."
                ),
                List.of(
                        "Breach confirmed. Cred injected: +{cred}.",
                        "Tier {tier} | Risk {risk}. Streak {streak}.",
                        "Trace cooled. Cooldown {cooldown}s."
                ),
                List.of(
                        "ICE spiked the line. Signal dropped.",
                        "Cooldown {cooldown}s before retry."
                ),
                List.of(
                        "Netrun rig cooling. {cooldown}s remaining."
                ),
                List.of(
                        "Active netrun detected. Stage {stage}/{stages}. Code {code}, {seconds}s left."
                )
        ));
        themes.put("chrome", new NeonEchoTheme(
                "chrome",
                "[NeonEcho//Chrome]",
                "Chrome deck synced. Type /netrun to jack in.",
                List.of(
                        "Chrome handshake ready. Tier {tier} | Risk {risk}.",
                        "Stage {stage}/{stages}. Run /netrun {code} within {seconds}s."
                ),
                List.of(
                        "Access granted. Cred payout: +{cred}.",
                        "Tier {tier} | Risk {risk}. Streak {streak}.",
                        "Cooldown engaged: {cooldown}s."
                ),
                List.of(
                        "Access denied. ICE triggered.",
                        "Cooldown engaged: {cooldown}s."
                ),
                List.of(
                        "Deck cooling. {cooldown}s remaining."
                ),
                List.of(
                        "Chrome netrun active. Stage {stage}/{stages}. Code {code}, {seconds}s left."
                )
        ));
        themes.put("ghost", new NeonEchoTheme(
                "ghost",
                "[NeonEcho.GHOST]",
                "Ghost channel active. Type /netrun to breach.",
                List.of(
                        "Ghost line open. Tier {tier} | Risk {risk}.",
                        "Stage {stage}/{stages}. Type /netrun {code} within {seconds}s."
                ),
                List.of(
                        "Ghost breach complete. Cred +{cred}.",
                        "Tier {tier} | Risk {risk}. Streak {streak}.",
                        "Shadows reset in {cooldown}s."
                ),
                List.of(
                        "Ghost line rejected.",
                        "Shadows reset in {cooldown}s."
                ),
                List.of(
                        "Ghost deck cooling. {cooldown}s left."
                ),
                List.of(
                        "Ghost netrun active. Stage {stage}/{stages}. Code {code}, {seconds}s left."
                )
        ));
        THEMES = Collections.unmodifiableMap(themes);
    }

    private NeonEchoThemes() {
    }

    public static NeonEchoTheme get(String name) {
        if (name == null) {
            return THEMES.get("neon");
        }
        NeonEchoTheme theme = THEMES.get(name.trim().toLowerCase());
        return theme != null ? theme : THEMES.get("neon");
    }

    public static Set<String> names() {
        return THEMES.keySet();
    }
}

package com.alexleo.neonecho;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NeonEchoData {
    public Map<String, Integer> cred = new ConcurrentHashMap<>();
    public Map<String, Integer> netrunWins = new ConcurrentHashMap<>();
    public Map<String, Integer> netrunFails = new ConcurrentHashMap<>();
    public Map<String, Integer> netrunStreak = new ConcurrentHashMap<>();
    public Map<String, Integer> netrunBestStreak = new ConcurrentHashMap<>();
    public Map<String, String> names = new ConcurrentHashMap<>();
    public Map<String, Integer> dailyChatCount = new ConcurrentHashMap<>();
    public Map<String, Integer> dailyNetrunWins = new ConcurrentHashMap<>();
    public Map<String, Integer> dailyOnlineSeconds = new ConcurrentHashMap<>();
    public Map<String, NeonDailyContract> dailyContracts = new ConcurrentHashMap<>();

    public void normalize() {
        if (cred == null) {
            cred = new ConcurrentHashMap<>();
        }
        if (netrunWins == null) {
            netrunWins = new ConcurrentHashMap<>();
        }
        if (netrunFails == null) {
            netrunFails = new ConcurrentHashMap<>();
        }
        if (netrunStreak == null) {
            netrunStreak = new ConcurrentHashMap<>();
        }
        if (netrunBestStreak == null) {
            netrunBestStreak = new ConcurrentHashMap<>();
        }
        if (names == null) {
            names = new ConcurrentHashMap<>();
        }
        if (dailyChatCount == null) {
            dailyChatCount = new ConcurrentHashMap<>();
        }
        if (dailyNetrunWins == null) {
            dailyNetrunWins = new ConcurrentHashMap<>();
        }
        if (dailyOnlineSeconds == null) {
            dailyOnlineSeconds = new ConcurrentHashMap<>();
        }
        if (dailyContracts == null) {
            dailyContracts = new ConcurrentHashMap<>();
        }
    }

    public NeonEchoData copy() {
        NeonEchoData copy = new NeonEchoData();
        copy.cred.putAll(cred);
        copy.netrunWins.putAll(netrunWins);
        copy.netrunFails.putAll(netrunFails);
        copy.netrunStreak.putAll(netrunStreak);
        copy.netrunBestStreak.putAll(netrunBestStreak);
        copy.names.putAll(names);
        copy.dailyChatCount.putAll(dailyChatCount);
        copy.dailyNetrunWins.putAll(dailyNetrunWins);
        copy.dailyOnlineSeconds.putAll(dailyOnlineSeconds);
        copy.dailyContracts.putAll(dailyContracts);
        return copy;
    }
}

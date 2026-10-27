package com.eisiadev.vendingmachine.storage;

import com.eisiadev.vendingmachine.currency.CurrencyAmount;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Wallet {

    private final Map<String, Long> balances = new TreeMap<>();

    public static Wallet copyOf(Wallet other) {
        Wallet w = new Wallet();
        w.balances.putAll(other.balances);
        return w;
    }

    public long get(String currency) {
        return balances.getOrDefault(currency, 0L);
    }

    public void set(String currency, long amount) {
        if (amount <= 0) balances.remove(currency);
        else balances.put(currency, amount);
    }

    public void add(String currency, long amount) {
        set(currency, saturatedAdd(get(currency), amount));
    }

    public boolean tryDeposit(String currency, long amount) {
        if (amount < 0) return false;
        long have = get(currency);
        if (have > Long.MAX_VALUE - amount) return false;
        set(currency, have + amount);
        return true;
    }

    public static long saturatedAdd(long a, long b) {
        long r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        return r;
    }

    public static long checkedMultiply(long a, long b) {
        try {
            return Math.multiplyExact(a, b);
        } catch (ArithmeticException e) {
            return -1;
        }
    }

    public void reset(String currency) {
        balances.remove(currency);
    }

    public void resetAll() {
        balances.clear();
    }

    public Map<String, Long> getBalances() {
        return Collections.unmodifiableMap(balances);
    }

    public boolean hasEnough(List<CurrencyAmount> price) {
        for (CurrencyAmount ca : price) {
            if (get(ca.type()) < ca.value()) return false;
        }
        return true;
    }

    public List<CurrencyAmount> pay(List<CurrencyAmount> price) {
        List<CurrencyAmount> remaining = new ArrayList<>();
        for (CurrencyAmount ca : price) {
            long have = get(ca.type());
            if (have >= ca.value()) {
                set(ca.type(), have - ca.value());
            } else {
                set(ca.type(), 0);
                remaining.add(new CurrencyAmount(ca.type(), ca.value() - have));
            }
        }
        return remaining;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        balances.forEach(tag::putLong);
        return tag;
    }

    public void load(CompoundTag tag) {
        balances.clear();
        for (String key : tag.getAllKeys()) {
            long v = tag.getLong(key);
            if (v > 0) balances.put(key, v);
        }
    }
}

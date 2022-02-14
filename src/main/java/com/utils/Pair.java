package com.utils;

import java.util.Objects;
import java.util.Random;

public class Pair implements Comparable<Pair> {
    private Integer value;
    private Integer index;

    public Pair(Integer value, Integer index) {
        this.value = value;
        this.index = index;
    }

    public Integer getValue() {
        return value;
    }

    public Pair setValue(Integer value) {
        this.value = value;
        return this;
    }

    public Integer getIndex() {
        return index;
    }

    public Pair setIndex(Integer index) {
        this.index = index;
        return this;
    }

    @Override
    public int compareTo(Pair o) {
        Random random = new Random();
        if (!Objects.equals(value, o.value))
            return value - o.value;
        else
            return random.nextInt(2) - random.nextInt(2) ;
    }

    @Override
    public String toString() {
        return "("+value.toString() + " " + index.toString()+")";
    }
}
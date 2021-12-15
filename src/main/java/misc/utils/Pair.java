package misc.utils;

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
        return value - o.value;
    }

    @Override
    public String toString() {
        return "("+value.toString() + " " + index.toString()+")";
    }
}
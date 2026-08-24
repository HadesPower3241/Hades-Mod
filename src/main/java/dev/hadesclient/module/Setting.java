package dev.hadesclient.module;

import com.google.gson.JsonObject;

/**
 * A single configurable value on a module. Kept as a small sealed-ish family
 * with nested implementations so adding a module never means touching more
 * than one file.
 */
public abstract class Setting {

    private final String id;
    private final String name;

    protected Setting(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() { return id; }

    public String name() { return name; }

    public abstract void save(JsonObject json);

    public abstract void load(JsonObject json);

    /** On/off. */
    public static final class Bool extends Setting {
        private boolean value;

        public Bool(String id, String name, boolean initial) {
            super(id, name);
            this.value = initial;
        }

        public boolean get() { return value; }

        public void set(boolean value) { this.value = value; }

        @Override
        public void save(JsonObject json) { json.addProperty(id(), value); }

        @Override
        public void load(JsonObject json) {
            if (json.has(id())) value = json.get(id()).getAsBoolean();
        }
    }

    /** A number in a range, optionally integer-only. */
    public static final class Number extends Setting {
        private final double min;
        private final double max;
        private final double step;
        private final boolean whole;
        private double value;

        public Number(String id, String name, double initial, double min, double max, double step, boolean whole) {
            super(id, name);
            this.min = min;
            this.max = max;
            this.step = step;
            this.whole = whole;
            this.value = initial;
        }

        public double get() { return value; }

        public int asInt() { return (int) Math.round(value); }

        public float asFloat() { return (float) value; }

        public void set(double value) { this.value = Math.max(min, Math.min(max, value)); }

        public double min() { return min; }

        public double max() { return max; }

        public double step() { return step; }

        public boolean whole() { return whole; }

        @Override
        public void save(JsonObject json) { json.addProperty(id(), value); }

        @Override
        public void load(JsonObject json) {
            if (json.has(id())) set(json.get(id()).getAsDouble());
        }
    }

    /** One choice out of a fixed list of labels. */
    public static final class Mode extends Setting {
        private final String[] options;
        private int index;

        public Mode(String id, String name, int initial, String... options) {
            super(id, name);
            this.options = options;
            this.index = initial;
        }

        public String[] options() { return options; }

        public int index() { return index; }

        public String get() { return options[index]; }

        public void set(int index) {
            if (options.length == 0) return;
            this.index = ((index % options.length) + options.length) % options.length;
        }

        public void next() { set(index + 1); }

        @Override
        public void save(JsonObject json) { json.addProperty(id(), index); }

        @Override
        public void load(JsonObject json) {
            if (json.has(id())) set(json.get(id()).getAsInt());
        }
    }

    /** An editable list of strings (e.g. ignored player names). */
    public static final class StringList extends Setting {
        private final java.util.List<String> values;

        public StringList(String id, String name) {
            super(id, name);
            this.values = new java.util.ArrayList<>();
        }

        public java.util.List<String> get() { return values; }

        public boolean contains(String value) {
            for (String v : values) if (v.equalsIgnoreCase(value)) return true;
            return false;
        }

        public void add(String value) {
            if (value == null) return;
            value = value.trim();
            if (value.isEmpty()) return;
            if (!contains(value)) values.add(value);
        }

        public void remove(String value) {
            values.removeIf(v -> v.equalsIgnoreCase(value));
        }

        public void clear() { values.clear(); }

        @Override
        public void save(JsonObject json) {
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (String v : values) arr.add(v);
            json.add(id(), arr);
        }

        @Override
        public void load(JsonObject json) {
            if (!json.has(id())) return;
            values.clear();
            com.google.gson.JsonElement el = json.get(id());
            if (el.isJsonArray()) {
                for (com.google.gson.JsonElement e : el.getAsJsonArray()) {
                    if (e.isJsonPrimitive()) add(e.getAsString());
                }
            }
        }
    }
}

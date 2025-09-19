package org.example;

import java.util.*;

// enkel JSON hjælper til undervisning (strings, tal, booleans, null) – én besked pr. linje
public class Json {
    public static Map<String, Object> parse(String s) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (s == null) return m;
        s = s.trim();
        if (s.isEmpty()) return m;
        if (s.charAt(0) == '{' && s.charAt(s.length()-1) == '}')
            s = s.substring(1, s.length()-1);

        List<String> parts = splitTopLevel(s);
        for (String p : parts) {
            int idx = p.indexOf(':');
            if (idx < 0) continue;
            String key = unquote(p.substring(0, idx).trim());
            String vraw = p.substring(idx+1).trim();
            Object v = parseValue(vraw);
            m.put(key, v);
        }
        return m;
    }

    public static String stringify(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escape(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v.toString());
            else sb.append("\"").append(escape(String.valueOf(v))).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    public static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i+1 < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i+1]);
        return m;
    }

    public static long asLong(Object o, long def) {
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return def; }
    }

    private static Object parseValue(String vraw) {
        if (vraw.equals("null")) return null;
        if (vraw.equals("true")) return Boolean.TRUE;
        if (vraw.equals("false")) return Boolean.FALSE;
        if (vraw.startsWith("\"")) return unquote(vraw);
        // tal
        try {
            if (vraw.contains(".") || vraw.contains("e") || vraw.contains("E"))
                return Double.valueOf(vraw);
            return Long.valueOf(vraw);
        } catch (Exception e) {
            // fallback til string
            return vraw;
        }
    }

    private static List<String> splitTopLevel(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i-1) != '\\')) inStr = !inStr;
            if (c == ',' && !inStr) { out.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private static String unquote(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length()-1);
        return s.replace("\\\"", "\"").replace("\\\\","\\");
    }

    private static String escape(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }
}

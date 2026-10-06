package amoussa.sixtakes.Multijoueur.Net;

import java.util.*;

/**
 * Mini parseur / sérialiseur JSON (sans dépendance externe).
 * Objets -> LinkedHashMap, tableaux -> ArrayList, nombres -> Long ou Double.
 */
public final class Json {

    private final String s;
    private int pos;

    private Json(String s) {
        this.s = s;
    }

    /**
     * Analyser une chaîne JSON.
     *
     * @param text
     * @return
     */
    public static Object parse(String text) {
        Json p = new Json(text);
        p.skipWs();
        Object v = p.readValue();
        p.skipWs();
        if (p.pos != p.s.length()) {
            throw new IllegalArgumentException("JSON invalide à la position " + p.pos);
        }
        return v;
    }

    /**
     * Analyser un objet JSON.
     *
     * @param text
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object o = parse(text);
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("Un objet JSON est attendu");
        }
        return (Map<String, Object>) o;
    }

    /**
     * Sérialiser une valeur Java en JSON.
     *
     * @param o
     * @return
     */
    public static String stringify(Object o) {
        StringBuilder sb = new StringBuilder();
        write(sb, o);
        return sb.toString();
    }

    /**
     * Construire un objet JSON à partir de paires clé / valeur.
     *
     * @param kv
     * @return
     */
    public static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    // ---- accès typés ----

    public static String str(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o == null ? null : o.toString();
    }

    public static int integer(Map<String, Object> m, String key, int def) {
        Object o = m.get(key);
        return o instanceof Number ? ((Number) o).intValue() : def;
    }

    public static boolean bool(Map<String, Object> m, String key) {
        return Boolean.TRUE.equals(m.get(key));
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof List ? (List<Object>) o : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> objects(Map<String, Object> m, String key) {
        List<Map<String, Object>> res = new ArrayList<>();
        for (Object o : list(m, key)) {
            if (o instanceof Map) {
                res.add((Map<String, Object>) o);
            }
        }
        return res;
    }

    public static List<Integer> ints(Object o) {
        List<Integer> res = new ArrayList<>();
        if (o instanceof List) {
            for (Object e : (List<?>) o) {
                if (e instanceof Number) {
                    res.add(((Number) e).intValue());
                }
            }
        }
        return res;
    }

    // ---- écriture ----

    private static void write(StringBuilder sb, Object o) {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof String) {
            writeString(sb, (String) o);
        } else if (o instanceof Number || o instanceof Boolean) {
            sb.append(o);
        } else if (o instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, String.valueOf(e.getKey()));
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
        } else if (o instanceof Collection) {
            sb.append('[');
            boolean first = true;
            for (Object e : (Collection<?>) o) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(sb, e);
            }
            sb.append(']');
        } else if (o instanceof int[]) {
            int[] arr = (int[]) o;
            List<Integer> l = new ArrayList<>();
            for (int v : arr) {
                l.add(v);
            }
            write(sb, l);
        } else {
            writeString(sb, o.toString());
        }
    }

    private static void writeString(StringBuilder sb, String str) {
        sb.append('"');
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    // ---- lecture ----

    private void skipWs() {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        if (pos >= s.length()) {
            throw new IllegalArgumentException("Fin de JSON inattendue");
        }
        return s.charAt(pos);
    }

    private void expect(char c) {
        if (peek() != c) {
            throw new IllegalArgumentException("'" + c + "' attendu à la position " + pos);
        }
        pos++;
    }

    private Object readValue() {
        char c = peek();
        switch (c) {
            case '{': return readObject();
            case '[': return readArray();
            case '"': return readString();
            case 't': readWord("true"); return Boolean.TRUE;
            case 'f': readWord("false"); return Boolean.FALSE;
            case 'n': readWord("null"); return null;
            default: return readNumber();
        }
    }

    private void readWord(String w) {
        if (!s.startsWith(w, pos)) {
            throw new IllegalArgumentException("Mot inattendu à la position " + pos);
        }
        pos += w.length();
    }

    private Map<String, Object> readObject() {
        Map<String, Object> m = new LinkedHashMap<>();
        expect('{');
        skipWs();
        if (peek() == '}') {
            pos++;
            return m;
        }
        while (true) {
            skipWs();
            String key = readString();
            skipWs();
            expect(':');
            skipWs();
            m.put(key, readValue());
            skipWs();
            if (peek() == ',') {
                pos++;
            } else {
                expect('}');
                return m;
            }
        }
    }

    private List<Object> readArray() {
        List<Object> l = new ArrayList<>();
        expect('[');
        skipWs();
        if (peek() == ']') {
            pos++;
            return l;
        }
        while (true) {
            skipWs();
            l.add(readValue());
            skipWs();
            if (peek() == ',') {
                pos++;
            } else {
                expect(']');
                return l;
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = peek();
            pos++;
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                char e = peek();
                pos++;
                switch (e) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default: sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
    }

    private Number readNumber() {
        int start = pos;
        while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) {
            pos++;
        }
        String n = s.substring(start, pos);
        if (n.isEmpty()) {
            throw new IllegalArgumentException("Valeur inattendue à la position " + start);
        }
        if (n.contains(".") || n.contains("e") || n.contains("E")) {
            return Double.parseDouble(n);
        }
        return Long.parseLong(n);
    }
}

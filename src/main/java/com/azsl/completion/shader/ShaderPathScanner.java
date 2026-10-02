package com.azsl.completion.shader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Determines where the caret sits inside a {@code .shader} (JSON) document by scanning the raw
 * text up to the caret: the chain of container keys ("path"), the member owning the current
 * value, whether the caret is inside a string and whether that string is a key or a value.
 *
 * <p>The scan is text based on purpose: CLion/Rider own the {@code .shader} file type, so the
 * plugin cannot rely on a particular PSI/language being present.</p>
 *
 * <p>No IntelliJ dependencies — usable from a plain unit harness.</p>
 */
public final class ShaderPathScanner {

    /** What kind of completion the caret position calls for. */
    public enum Position {
        KEY,
        VALUE
    }

    /**
     * @param path          ancestor container keys, raw spelling (root container contributes nothing)
     * @param valueKey      member owning the current value; {@code null} for {@link Position#KEY}
     *                      and for array elements (whose path already ends with the array key)
     * @param position      key vs. value position
     * @param inString      caret is inside a quoted string
     * @param containerKeys keys already present in the current container (raw spelling)
     */
    public record Context(List<String> path,
                          String valueKey,
                          Position position,
                          boolean inString,
                          Set<String> containerKeys) {
    }

    private enum State {
        /** Object wants a key. */
        EXPECT_KEY,
        /** Key string was read; the colon is next. */
        AFTER_KEY,
        /** The value of the current key (started or in progress). */
        IN_VALUE,
        /** Value finished; a comma or the container end is next. */
        AFTER_VALUE
    }

    private static final class Frame {
        final String key;       // member that owns this container, or null (root / array element)
        final boolean isArray;
        State state = State.EXPECT_KEY;
        String currentKey;      // last key read in this object
        final Set<String> keys = new HashSet<>();

        Frame(String key, boolean isArray) {
            this.key = key;
            this.isArray = isArray;
        }
    }

    private ShaderPathScanner() {
    }

    /** Scans {@code text[0, offset)} and reports the caret context. */
    public static Context scan(CharSequence text, int offset) {
        final int end = Math.max(0, Math.min(offset, text.length()));

        List<Frame> stack = new ArrayList<>();
        Frame root = new Frame(null, false);
        root.state = State.EXPECT_KEY;
        stack.add(root);

        boolean inString = false;
        boolean stringIsKey = false;
        StringBuilder content = new StringBuilder();

        int i = 0;
        while (i < end) {
            char c = text.charAt(i);

            if (inString) {
                if (c == '\\') {
                    // Skip the escaped character (bounded by the caret).
                    i = Math.min(i + 2, end);
                    continue;
                }
                if (c == '"') {
                    Frame frame = top(stack);
                    if (stringIsKey) {
                        frame.currentKey = content.toString();
                        frame.keys.add(frame.currentKey);
                        frame.state = State.AFTER_KEY;
                    }
                    inString = false;
                    content.setLength(0);
                    i++;
                    continue;
                }
                content.append(c);
                i++;
                continue;
            }

            // Comments are not valid in the engine's JSON (rapidjson defaults), but older or
            // hand-edited files may contain them — skip so braces/quotes inside don't confuse us.
            if (c == '/' && i + 1 < end) {
                char next = text.charAt(i + 1);
                if (next == '/') {
                    while (i < end && text.charAt(i) != '\n') {
                        i++;
                    }
                    continue;
                }
                if (next == '*') {
                    i += 2;
                    while (i + 1 < end && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) {
                        i++;
                    }
                    i = Math.min(i + 2, end);
                    continue;
                }
            }

            Frame frame = top(stack);
            switch (c) {
                case '"':
                    inString = true;
                    stringIsKey = frame.state == State.EXPECT_KEY;
                    content.setLength(0);
                    i++;
                    break;
                case '{': {
                    String owner = frame.state == State.IN_VALUE ? frame.currentKey : null;
                    stack.add(new Frame(owner, false));
                    i++;
                    break;
                }
                case '[': {
                    String owner = frame.state == State.IN_VALUE ? frame.currentKey : null;
                    Frame child = new Frame(owner, true);
                    child.state = State.IN_VALUE;
                    stack.add(child);
                    i++;
                    break;
                }
                case '}':
                case ']':
                    if (stack.size() > 1) {
                        stack.remove(stack.size() - 1);
                        Frame parent = top(stack);
                        if (parent.state == State.IN_VALUE) {
                            parent.state = State.AFTER_VALUE;
                        }
                    }
                    i++;
                    break;
                case ':':
                    if (frame.state == State.AFTER_KEY) {
                        frame.state = State.IN_VALUE;
                    }
                    i++;
                    break;
                case ',':
                    frame.state = frame.isArray ? State.IN_VALUE : State.EXPECT_KEY;
                    i++;
                    break;
                default:
                    i++;
                    break;
            }
        }

        Frame top = top(stack);
        List<String> path = containerPath(stack);
        Set<String> containerKeys = Set.copyOf(top.keys);

        if (inString) {
            if (stringIsKey) {
                return new Context(path, null, Position.KEY, true, containerKeys);
            }
            return new Context(path, valueKey(top), Position.VALUE, true, containerKeys);
        }
        if (top.state == State.EXPECT_KEY || top.state == State.AFTER_KEY) {
            return new Context(path, null, Position.KEY, false, containerKeys);
        }
        return new Context(path, valueKey(top), Position.VALUE, false, containerKeys);
    }

    /** Member owning the current value of {@code frame}, or {@code null}. */
    private static String valueKey(Frame frame) {
        if (frame.isArray) {
            // Elements of an array are values of the array member itself, which is already the
            // last segment of the container path.
            return null;
        }
        return frame.currentKey;
    }

    /** Outer-to-inner chain of member keys that own the open containers (root excluded). */
    private static List<String> containerPath(List<Frame> stack) {
        List<String> path = new ArrayList<>();
        for (Frame frame : stack) {
            if (frame.key != null && !frame.key.isEmpty()) {
                path.add(frame.key);
            }
        }
        return path;
    }

    private static Frame top(List<Frame> stack) {
        return stack.get(stack.size() - 1);
    }
}

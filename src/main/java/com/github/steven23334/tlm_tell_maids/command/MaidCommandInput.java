package com.github.steven23334.tlm_tell_maids.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 解析 {@code /tellmaid} 的输入，并生成 Tab 补全候选。
 *
 * <p>支持三种目标写法：
 * <ul>
 *     <li>{@code <name>}：与单个女仆对话，名字含空格等字符时可以写成 {@code "..."} 或 {@code '...'}；</li>
 *     <li>{@code @m}：与附近所有属于当前玩家的女仆对话；</li>
 *     <li>{@code @m[<name_a>,<name_b>,...]}：同时与列表中的女仆对话。</li>
 * </ul>
 *
 * <p>本类刻意不依赖 Minecraft 与 Brigadier，方便单独测试解析逻辑。
 */
public final class MaidCommandInput {
    /**
     * 多女仆选择器前缀。
     */
    public static final String ALL_MAIDS_SELECTOR = "@m";

    private static final String LIST_START = "[";

    private MaidCommandInput() {
    }

    public enum Error {
        /**
         * 引号或方括号没有闭合。
         */
        BAD_QUOTE,
        /**
         * 女仆名与消息之间缺少空格。
         */
        BAD_SEPARATOR,
        /**
         * 不认识的 {@code @...} 选择器。
         */
        UNKNOWN_SELECTOR,
        /**
         * 女仆名为空（包括空的 {@code @m[]} 列表）。
         */
        EMPTY_NAME
    }

    public static final class ParseException extends Exception {
        private final Error error;
        private final String detail;

        ParseException(Error error, String detail) {
            super(error.name() + (detail == null || detail.isEmpty() ? "" : " (" + detail + ")"));
            this.error = error;
            this.detail = detail == null ? "" : detail;
        }

        public Error error() {
            return this.error;
        }

        public String detail() {
            return this.detail;
        }
    }

    /**
     * 解析出的女仆目标。
     *
     * @param all   为 true 时表示 {@code @m}，即附近所有属于自己的女仆
     * @param names {@code all} 为 false 时要对话的女仆名，顺序与玩家输入一致，已去重
     */
    public record Target(boolean all, List<String> names) {
        public static Target allMaids() {
            return new Target(true, List.of());
        }

        public static Target of(String name) {
            return new Target(false, List.of(name));
        }

        public static Target of(List<String> names) {
            return new Target(false, List.copyOf(names));
        }
    }

    /**
     * 名字参数的解析结果。
     *
     * @param target 解析出的目标
     * @param end    参数结束位置，供 Brigadier 的 StringReader 移动光标
     */
    public record TargetParse(Target target, int end) {
    }

    /**
     * 一个补全候选。
     *
     * @param start 替换起点在已输入文本中的下标（相对名字参数开头）
     * @param text  要插入的文本
     */
    public record Suggestion(int start, String text) {
    }

    /**
     * 只解析名字参数（不包含后面的消息）。
     *
     * @param input 完整命令字符串
     * @param start 名字参数的起始下标（即 Brigadier StringReader 的光标）
     * @return 解析结果
     * @throws ParseException 语法错误
     */
    public static TargetParse parseTarget(String input, int start) throws ParseException {
        int length = input.length();
        if (start >= length) {
            throw new ParseException(Error.EMPTY_NAME, "");
        }
        if (input.charAt(start) == '@') {
            return parseSelectorTarget(input, start);
        }
        Token token = readToken(input, start);
        return new TargetParse(Target.of(token.value()), token.end());
    }

    /**
     * 生成 Tab 补全候选。
     *
     * <p>返回值中的 {@link Suggestion#start()} 是“替换起点”，因此 {@code @m[...]} 里的候选
     * 只替换当前正在输入的那个名字，效果和原版 {@code @e[...]} 的候选栏一致。
     *
     * @param remaining 玩家当前已输入的内容（已经去掉 {@code /tellmaid } 前缀）
     * @param maidNames 附近属于该玩家的女仆名
     * @return 补全候选
     */
    public static List<Suggestion> suggestions(String remaining, List<String> maidNames) {
        List<Suggestion> result = new ArrayList<>();
        if (remaining == null) {
            remaining = "";
        }
        int start = 0;
        while (start < remaining.length() && Character.isWhitespace(remaining.charAt(start))) {
            start++;
        }
        remaining = remaining.substring(start);

        if (remaining.isEmpty()) {
            result.add(new Suggestion(0, ALL_MAIDS_SELECTOR));
            for (String name : maidNames) {
                result.add(new Suggestion(0, formatSingleName(name)));
            }
            return distinct(result);
        }

        if (remaining.charAt(0) == '@') {
            return distinct(suggestSelector(remaining, maidNames));
        }

        String prefix = readSingleNamePrefix(remaining);
        if (prefix == null) {
            // 女仆名之后已经有空格，说明玩家在输入消息，不需要补全
            return result;
        }
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        for (String name : maidNames) {
            if (name.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                result.add(new Suggestion(0, formatSingleName(name)));
            }
        }
        return distinct(result);
    }

    // ------------------------------------------------------------------
    // 解析
    // ------------------------------------------------------------------

    private static TargetParse parseSelectorTarget(String input, int start) throws ParseException {
        int length = input.length();
        if (start + 1 >= length || input.charAt(start + 1) != 'm') {
            int end = start + 1;
            while (end < length && !Character.isWhitespace(input.charAt(end))
                    && input.charAt(end) != ',' && input.charAt(end) != ']') {
                end++;
            }
            throw new ParseException(Error.UNKNOWN_SELECTOR, input.substring(start, end));
        }

        int cursor = start + ALL_MAIDS_SELECTOR.length();
        if (cursor < length && input.charAt(cursor) == '[') {
            NameList nameList = readNameList(input, cursor + 1);
            return new TargetParse(Target.of(nameList.names()), nameList.end());
        }
        if (cursor < length && !Character.isWhitespace(input.charAt(cursor))) {
            // 例如 @maid，既不是 @m 也不是 @m[...]，当作不认识的选择器处理
            int end = cursor;
            while (end < length && !Character.isWhitespace(input.charAt(end))) {
                end++;
            }
            throw new ParseException(Error.UNKNOWN_SELECTOR, input.substring(start, end));
        }
        return new TargetParse(Target.allMaids(), cursor);
    }

    private static NameList readNameList(String input, int cursor) throws ParseException {
        int length = input.length();
        List<String> names = new ArrayList<>();
        boolean closed = false;

        while (cursor < length) {
            while (cursor < length && Character.isWhitespace(input.charAt(cursor))) {
                cursor++;
            }
            if (cursor >= length) {
                break;
            }
            char c = input.charAt(cursor);
            if (c == ']') {
                cursor++;
                closed = true;
                break;
            }
            if (c == ',') {
                // 允许多余的空项，例如 @m[女仆A,]
                cursor++;
                continue;
            }
            Entry entry = readEntry(input, cursor);
            names.add(entry.value());
            cursor = entry.end();
        }

        if (!closed) {
            throw new ParseException(Error.BAD_QUOTE, "");
        }
        if (names.isEmpty()) {
            throw new ParseException(Error.EMPTY_NAME, "");
        }
        return new NameList(dedupe(names), cursor);
    }

    private static Entry readEntry(String input, int start) throws ParseException {
        int length = input.length();
        if (isQuote(input.charAt(start))) {
            Token token = readToken(input, start);
            int cursor = token.end();
            while (cursor < length && Character.isWhitespace(input.charAt(cursor))) {
                cursor++;
            }
            if (cursor < length && input.charAt(cursor) != ',' && input.charAt(cursor) != ']') {
                throw new ParseException(Error.BAD_SEPARATOR, "");
            }
            return new Entry(token.value(), cursor);
        }

        int cursor = start;
        while (cursor < length && input.charAt(cursor) != ',' && input.charAt(cursor) != ']') {
            cursor++;
        }
        String value = input.substring(start, cursor).trim();
        if (value.isEmpty()) {
            throw new ParseException(Error.EMPTY_NAME, "");
        }
        return new Entry(value, cursor);
    }

    /**
     * 读取一个名字：双引号或单引号包裹的字符串，或者一段不含空白的文本。
     */
    private static Token readToken(String input, int start) throws ParseException {
        int length = input.length();
        char quote = input.charAt(start);
        if (!isQuote(quote)) {
            int cursor = start;
            while (cursor < length && !Character.isWhitespace(input.charAt(cursor))) {
                cursor++;
            }
            String value = input.substring(start, cursor);
            if (value.isEmpty()) {
                throw new ParseException(Error.EMPTY_NAME, "");
            }
            return new Token(value, cursor);
        }

        StringBuilder value = new StringBuilder();
        int cursor = start + 1;
        boolean closed = false;
        while (cursor < length) {
            char c = input.charAt(cursor++);
            if (c == '\\') {
                if (cursor >= length) {
                    throw new ParseException(Error.BAD_QUOTE, "");
                }
                char escaped = input.charAt(cursor++);
                if (escaped == quote || escaped == '\\') {
                    value.append(escaped);
                } else {
                    value.append('\\').append(escaped);
                }
            } else if (c == quote) {
                closed = true;
                break;
            } else {
                value.append(c);
            }
        }
        if (!closed) {
            throw new ParseException(Error.BAD_QUOTE, "");
        }
        if (value.isEmpty()) {
            throw new ParseException(Error.EMPTY_NAME, "");
        }
        return new Token(value.toString(), cursor);
    }

    private record Token(String value, int end) {
    }

    private record Entry(String value, int end) {
    }

    private record NameList(List<String> names, int end) {
    }

    // ------------------------------------------------------------------
    // 补全
    // ------------------------------------------------------------------

    private static List<Suggestion> suggestSelector(String remaining, List<String> maidNames) {
        List<Suggestion> result = new ArrayList<>();
        if (remaining.equals("@")) {
            result.add(new Suggestion(0, ALL_MAIDS_SELECTOR));
            return result;
        }
        if (!remaining.startsWith(ALL_MAIDS_SELECTOR)) {
            return result;
        }
        if (remaining.length() == ALL_MAIDS_SELECTOR.length()) {
            // 补出左方括号，随后就会列出女仆名
            result.add(new Suggestion(0, ALL_MAIDS_SELECTOR + LIST_START));
            return result;
        }
        char next = remaining.charAt(ALL_MAIDS_SELECTOR.length());
        if (Character.isWhitespace(next)) {
            // 已经进入消息部分
            return result;
        }
        if (next != '[') {
            return result;
        }
        return suggestNameList(remaining, maidNames);
    }

    private static List<Suggestion> suggestNameList(String remaining, List<String> maidNames) {
        List<Suggestion> result = new ArrayList<>();
        String body = remaining.substring(ALL_MAIDS_SELECTOR.length() + 1);
        int closing = findClosingBracket(body);
        if (closing >= 0) {
            // 方括号已闭合，剩下的部分属于消息
            return result;
        }

        int lastComma = lastTopLevelComma(body);
        String completedRaw = lastComma >= 0 ? body.substring(0, lastComma) : "";

        Set<String> completedLower = new LinkedHashSet<>();
        for (String raw : splitTopLevel(completedRaw)) {
            String value = unquote(raw.trim());
            if (!value.isEmpty()) {
                completedLower.add(value.toLowerCase(Locale.ROOT));
            }
        }

        // 当前正在输入的名字的起始位置（跳过逗号后的空格），只替换这一段
        int partialStart = ALL_MAIDS_SELECTOR.length() + 1 + lastComma + 1;
        while (partialStart < remaining.length() && Character.isWhitespace(remaining.charAt(partialStart))) {
            partialStart++;
        }
        String partial = remaining.substring(partialStart);
        if (!partial.isEmpty() && isQuote(partial.charAt(0))) {
            partial = partial.substring(1);
        }
        String lowerPartial = partial.toLowerCase(Locale.ROOT);

        for (String name : maidNames) {
            if (completedLower.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (!name.toLowerCase(Locale.ROOT).startsWith(lowerPartial)) {
                continue;
            }
            result.add(new Suggestion(partialStart, formatListEntry(name)));
        }
        return result;
    }

    /**
     * 当玩家正在输入单个女仆名时，返回名字前缀；如果已经输入完名字（后面有空格），返回 null。
     */
    private static String readSingleNamePrefix(String remaining) {
        if (Character.isWhitespace(remaining.charAt(0))) {
            return null;
        }
        char first = remaining.charAt(0);
        if (isQuote(first)) {
            StringBuilder text = new StringBuilder();
            int i = 1;
            boolean escaped = false;
            while (i < remaining.length()) {
                char c = remaining.charAt(i++);
                if (escaped) {
                    text.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == first) {
                    // 引号已经闭合，名字输入完了
                    return null;
                } else {
                    text.append(c);
                }
            }
            return text.toString();
        }
        for (int i = 0; i < remaining.length(); i++) {
            if (Character.isWhitespace(remaining.charAt(i))) {
                return null;
            }
        }
        return remaining;
    }

    private static int findClosingBracket(String body) {
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (isQuote(c)) {
                quote = c;
            } else if (c == ']') {
                return i;
            }
        }
        return -1;
    }

    private static int lastTopLevelComma(String body) {
        char quote = 0;
        boolean escaped = false;
        int last = -1;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (isQuote(c)) {
                quote = c;
            } else if (c == ',') {
                last = i;
            }
        }
        return last;
    }

    private static List<String> splitTopLevel(String raw) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (quote != 0) {
                current.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (isQuote(c)) {
                quote = c;
                current.append(c);
            } else if (c == ',') {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    private static String unquote(String raw) {
        if (raw.length() < 2 || !isQuote(raw.charAt(0)) || raw.charAt(raw.length() - 1) != raw.charAt(0)) {
            return raw;
        }
        String inner = raw.substring(1, raw.length() - 1);
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (escaped) {
                value.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                value.append(c);
            }
        }
        if (escaped) {
            value.append('\\');
        }
        return value.toString();
    }

    private static String formatSingleName(String name) {
        if (!needsQuoting(name, false)) {
            return name;
        }
        return "\"" + escape(name) + "\"";
    }

    private static String formatListEntry(String name) {
        if (!needsQuoting(name, true)) {
            return name;
        }
        return "\"" + escape(name) + "\"";
    }

    private static boolean needsQuoting(String name, boolean inList) {
        if (name.isEmpty()) {
            return true;
        }
        if (name.charAt(0) == '@' || isQuote(name.charAt(0))) {
            // 避免被误认为 @m 选择器，或被误认为引号字符串的开头
            return true;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isWhitespace(c) || c == '"' || c == '\\') {
                return true;
            }
            if (inList && (c == ',' || c == '[' || c == ']')) {
                return true;
            }
        }
        return false;
    }

    private static boolean isQuote(char c) {
        return c == '"' || c == '\'';
    }

    private static String escape(String name) {
        StringBuilder value = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '"' || c == '\\') {
                value.append('\\');
            }
            value.append(c);
        }
        return value.toString();
    }

    private static List<String> dedupe(List<String> values) {
        Map<String, String> unique = new LinkedHashMap<>();
        for (String value : values) {
            unique.putIfAbsent(value.toLowerCase(Locale.ROOT), value);
        }
        return List.copyOf(unique.values());
    }

    private static <T> List<T> distinct(List<T> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }
}

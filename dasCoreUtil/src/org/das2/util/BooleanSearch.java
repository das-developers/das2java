
package org.das2.util;
        
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Small Boolean full-text search parser. Requires Java 8 or later.
 *
 * <p>Operators (case-insensitive): NOT, AND, OR, in descending precedence.
 * Adjacent terms imply AND. Parentheses group expressions; double quotes
 * match literal phrases. A minus sign at the start of a term means NOT.
 * Searches are case-insensitive substring matches, not whole-word matches.
 *
 * <p>Example: {@code BooleanSearch.compile("(rbsp OR themis) AND density")}
 */
public final class BooleanSearch {
    private BooleanSearch() { }

    /** Compiles a query once for use against many strings. */
    public static Predicate<String> compile(String query) {
        if (query == null) {
            throw new IllegalArgumentException("Query must not be null");
        }
        return compileTokens(tokenize(query));
    }

    /**
     * Compiles a partially typed query for interactive search fields.
     * Incomplete trailing operators and empty opening groups are discarded;
     * unmatched opening parentheses are closed automatically. An unfinished
     * quoted phrase is treated as a phrase extending to the end of the query.
     * Other syntax errors are still reported.
     */
    public static Predicate<String> compileLenient(String query) {
        if (query == null) {
            throw new IllegalArgumentException("Query must not be null");
        }
        List<Token> tokens = tokenize(query, true);

        // Strip incomplete suffixes, including "OR (" and "AND NOT".
        boolean changed;
        do {
            changed = false;
            while (!tokens.isEmpty()) {
                Type type = tokens.get(tokens.size() - 1).type;
                if (type == Type.AND || type == Type.OR || type == Type.NOT) {
                    tokens.remove(tokens.size() - 1);
                    changed = true;
                } else {
                    break;
                }
            }
            if (!tokens.isEmpty()
                    && tokens.get(tokens.size() - 1).type == Type.LPAREN) {
                tokens.remove(tokens.size() - 1);
                changed = true;
            }
        } while (changed);

        int depth = 0;
        for (Token token : tokens) {
            if (token.type == Type.LPAREN) {
                depth++;
            } else if (token.type == Type.RPAREN) {
                depth--;
                if (depth < 0) {
                    throw new IllegalArgumentException("Unexpected closing ')'");
                }
            }
        }
        while (depth-- > 0) {
            tokens.add(new Token(Type.RPAREN, ")"));
        }
        return compileTokens(tokens);
    }

    private static Predicate<String> compileTokens(List<Token> tokens) {
        Parser parser = new Parser(tokens);
        if (tokens.isEmpty()) {
            return text -> true;
        }
        Predicate<String> result = parser.parseOr();
        if (parser.peek() != null) {
            throw new IllegalArgumentException("Unexpected token: " + parser.peek().value);
        }
        return text -> result.test(text == null ? "" : text.toLowerCase(Locale.ROOT));
    }

    /** Convenience method for one-off matches. */
    public static boolean matches(String query, String text) {
        return compile(query).test(text);
    }

    private enum Type { WORD, AND, OR, NOT, LPAREN, RPAREN }

    private static final class Token {
        final Type type;
        final String value;
        Token(Type type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    private static List<Token> tokenize(String query) {
        return tokenize(query, false);
    }

    private static List<Token> tokenize(String query, boolean lenient) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < query.length()) {
            char ch = query.charAt(i);
            if (Character.isWhitespace(ch)) {
                i++;
                continue;
            }
            if (ch == '(' || ch == ')') {
                tokens.add(new Token(ch == '(' ? Type.LPAREN : Type.RPAREN,
                                     Character.toString(ch)));
                i++;
                continue;
            }
            if (ch == '"') {
                int start = ++i;
                while (i < query.length() && query.charAt(i) != '"') {
                    i++;
                }
                if (i == query.length() && !lenient) {
                    throw new IllegalArgumentException("Unterminated quoted phrase");
                }
                tokens.add(new Token(Type.WORD, query.substring(start, i)));
                if (i < query.length()) {
                    i++;
                }
                continue;
            }
            if (ch == '-') {
                tokens.add(new Token(Type.NOT, "-"));
                i++;
                continue;
            }
            int start = i;
            while (i < query.length() && !Character.isWhitespace(query.charAt(i))
                    && query.charAt(i) != '(' && query.charAt(i) != ')') {
                i++;
            }
            String word = query.substring(start, i);
            Type type;
            switch (word.toUpperCase(Locale.ROOT)) {
                case "AND": type = Type.AND; break;
                case "OR":  type = Type.OR;  break;
                case "NOT": type = Type.NOT; break;
                default:    type = Type.WORD; break;
            }
            tokens.add(new Token(type, word));
        }
        return tokens;
    }

    private static final class Parser {
        final List<Token> tokens;
        int pos;

        Parser(List<Token> tokens) { this.tokens = tokens; }

        Token peek() {
            return pos < tokens.size() ? tokens.get(pos) : null;
        }

        boolean accept(Type type) {
            if (peek() != null && peek().type == type) {
                pos++;
                return true;
            }
            return false;
        }

        Predicate<String> primary() {
            if (accept(Type.LPAREN)) {
                Predicate<String> inner = parseOr();
                if (!accept(Type.RPAREN)) {
                    throw new IllegalArgumentException("Missing closing ')'");
                }
                return inner;
            }
            Token token = peek();
            if (token != null && token.type == Type.WORD) {
                pos++;
                String term = token.value.toLowerCase(Locale.ROOT);
                return text -> text.contains(term);
            }
            throw new IllegalArgumentException("Expected a search term or '('");
        }

        Predicate<String> parseNot() {
            if (accept(Type.NOT)) {
                return primaryOrNot().negate();
            }
            return primary();
        }

        private Predicate<String> primaryOrNot() {
            return parseNot();
        }

        Predicate<String> parseAnd() {
            Predicate<String> left = parseNot();
            while (true) {
                if (accept(Type.AND)) {
                    left = left.and(parseNot());
                } else if (peek() != null &&
                           (peek().type == Type.WORD || peek().type == Type.LPAREN
                            || peek().type == Type.NOT)) {
                    left = left.and(parseNot());
                } else {
                    return left;
                }
            }
        }

        Predicate<String> parseOr() {
            Predicate<String> left = parseAnd();
            while (accept(Type.OR)) {
                left = left.or(parseAnd());
            }
            return left;
        }
    }
}

import java.util.HashMap;
import java.util.Map;

public enum TokenType implements LexicalElement {
    KEYWORD("keyword"), SYMBOL("symbol"), IDENTIFIER("identifier"), INT_CONST("integerConstant"), STRING_CONST("stringConstant");

    private static final Map<String, TokenType> TO_KEYWORD = new HashMap<>();
    private final String identifier;

    TokenType(String str) {
        this.identifier = str;
    }

    static {
        for (TokenType kw : TokenType.values())
            TO_KEYWORD.put(kw.identifier, kw);
    }

    public static TokenType lookup(String s) {
        return TO_KEYWORD.get(s);
    }

    @Override
    public String toString() {
        return identifier;
    }
}

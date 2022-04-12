import java.util.HashMap;
import java.util.Map;

public enum Keyword implements LexicalElement {
    CLASS("class"), METHOD("method"), FUNCTION("function"), CONSTRUCTOR("constructor"), INT("int"),
    BOOLEAN("boolean"), CHAR("char"), VOID("void"), VAR("var"), STATIC("static"), FIELD("field"),
    LET("let"), DO("do"), IF("if"), ELSE("else"), WHILE("while"), RETURN("return"), TRUE("true"), FALSE("false"),
    NULL("null"), THIS("this");

    private static final Map<String, Keyword> TO_KEYWORD = new HashMap<>();
    private final String identifier;

    Keyword(String str) {
        this.identifier = str;
    }

    static {
        for (Keyword kw : Keyword.values())
            TO_KEYWORD.put(kw.identifier, kw);
    }

    public static Keyword lookup(String s) {
        return TO_KEYWORD.get(s);
    }

    @Override
    public String toString() {
        return identifier;
    }
}

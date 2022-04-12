import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompilationEngine {
    private final static String newline = System.getProperty("line.separator");
    private final static Set<LexicalElement> TYPE = Set.of(Keyword.INT, Keyword.CHAR, Keyword.BOOLEAN, TokenType.IDENTIFIER);
    private final static Set<Character> OP = Set.of('+', '-', '*', '/', '&', '|', '>', '<', '=');
    private final static Set<Keyword> KEYWORD_CONST = Set.of(Keyword.FALSE, Keyword.TRUE, Keyword.NULL, Keyword.THIS);
    private final PrintWriter out;
    private final JackTokenizer jt;
    private int currLevel = 0;

    public CompilationEngine(PrintWriter out, JackTokenizer jt) {
        this.out = out;
        this.jt = jt;
    }

    private void addTabs() {
        for (int i = 0; i < currLevel; i++)
            out.print('\t');
    }

    private void appendWithTabs(String s) {
        addTabs();
        out.println(s);
    }

    private void appendXML(String tag, String val) {
        addTabs();
        out.format("<%1$s> %2$s </%1$s>\n", tag, val);
        jt.advance();
    }

    private void eat(Set<LexicalElement> expected) {
        if (!jt.hasMoreTokens() || (!expected.contains(jt.tokenType())
                && (jt.tokenType() == TokenType.KEYWORD && !expected.contains(jt.keyword()))))
            throw new IllegalArgumentException("this is not a valid jack code. Type expected");

        switch (jt.tokenType()) {
            case KEYWORD:
                eat(jt.keyword());
                break;
            case IDENTIFIER:
                eat();
                break;
            case SYMBOL:
                eat(jt.symbol());
                break;
            case INT_CONST:
                eat(jt.intVal());
                break;
            case STRING_CONST:
                eat(jt.stringVal());
                break;
            default:
                break;
        }
    }

    private void eat(String expected) {
        if (!jt.hasMoreTokens() || jt.tokenType() != TokenType.STRING_CONST)
            throw new IllegalArgumentException("this is not a valid jack code. String expected");
        appendXML("stringConstant", expected);
    }

    private void eat(int expected) {
        if (!jt.hasMoreTokens() || jt.tokenType() != TokenType.INT_CONST)
            throw new IllegalArgumentException("this is not a valid jack code. Integer expected");
        appendXML("integerConstant", String.valueOf(expected));
    }

    private void eat(char expected) {
        if (!jt.hasMoreTokens() || jt.tokenType() != TokenType.SYMBOL || expected != jt.symbol())
            throw new IllegalArgumentException("this is not a valid jack code. Symbol expected");
        String exp = String.valueOf(expected);
        if (expected == '<')
            exp = "&lt;";
        if (expected == '>')
            exp = "&gt;";
        if (expected == '&')
            exp = "&amp;";
        appendXML("symbol", exp);
    }

    private void eat() {
        if (!jt.hasMoreTokens() || jt.tokenType() != TokenType.IDENTIFIER)
            throw new IllegalArgumentException("this is not a valid jack code. Identifier expected");
        appendXML("identifier", jt.identifier());
    }

    private void eat(Keyword expected) {
        if (!jt.hasMoreTokens() || jt.tokenType() != TokenType.KEYWORD || expected != jt.keyword())
            throw new IllegalArgumentException("this is not a valid jack code. Keyword expected");
        appendXML("keyword", expected.toString());
    }

    private void compile(Runnable compileFunc) {
        if (!jt.hasMoreTokens())
            return;
        currLevel++;
        compileFunc.run();
        currLevel--;
    }

    public void compileClass() {
        if (!jt.hasMoreTokens())
            return;

        out.println("<class>");

        currLevel++;

        jt.advance();
        eat(Keyword.CLASS);
        eat();
        eat('{');
        compileClassVarDec();
        compileSubroutineDec();
        eat('}');

        currLevel--;
        out.println("</class>");
    }

    public void compileClassVarDec() {
        while (jt.hasMoreTokens() && jt.tokenType() == TokenType.KEYWORD && (jt.keyword() == Keyword.FIELD || jt.keyword() == Keyword.STATIC)) {
            appendWithTabs("<classVarDec>");
            currLevel++;
            eat(jt.keyword());
            eat(TYPE);
            eat();
            while (jt.tokenType() == TokenType.SYMBOL && jt.symbol() == ',') {
                eat(',');
                eat();
            }
            eat(';');
            currLevel--;
            appendWithTabs("</classVarDec>");
        }
    }

    public void compileSubroutineDec() {
        Set<LexicalElement> typeOrVoid = Stream.concat(TYPE.stream(), Stream.of(Keyword.VOID)).collect(Collectors.toUnmodifiableSet());
        while (jt.hasMoreTokens() && jt.tokenType() == TokenType.KEYWORD
                && (jt.keyword() == Keyword.CONSTRUCTOR
                || jt.keyword() == Keyword.FUNCTION
                || jt.keyword() == Keyword.METHOD)) {
            appendWithTabs("<subroutineDec>");
            currLevel++;
            eat(jt.keyword());
            eat(typeOrVoid);
            eat();
            eat('(');
            compileParameterList();
            eat(')');
            compileSubroutineBody();
            currLevel--;
            appendWithTabs("</subroutineDec>");
        }
    }

    public void compileParameterList() {
        appendWithTabs("<parameterList>");
        currLevel++;

        if (jt.hasMoreTokens() && jt.tokenType() == TokenType.KEYWORD && TYPE.contains(jt.keyword())) {
            eat(TYPE);
            eat();
            while (jt.hasMoreTokens() && jt.tokenType() == TokenType.SYMBOL && jt.symbol() == ',') {
                eat(',');
                eat(TYPE);
                eat();
            }
        }

        currLevel--;
        appendWithTabs("</parameterList>");
    }

    public void compileSubroutineBody() {
        appendWithTabs("<subroutineBody>");
        currLevel++;

        eat('{');
        compileVarDec();
        compileStatements();
        eat('}');

        currLevel--;
        appendWithTabs("</subroutineBody>");
    }

    public void compileVarDec() {
        while (jt.hasMoreTokens() && jt.tokenType() == TokenType.KEYWORD && jt.keyword() == Keyword.VAR) {
            appendWithTabs("<varDec>");
            currLevel++;
            eat(Keyword.VAR);
            eat(TYPE);
            eat();
            while (jt.hasMoreTokens() && jt.tokenType() == TokenType.SYMBOL && jt.symbol() == ',') {
                eat(',');
                eat();
            }
            eat(';');
            currLevel--;
            appendWithTabs("</varDec>");
        }
    }

    public void compileStatements() {
        if (!jt.hasMoreTokens() || jt.tokenType() != TokenType.KEYWORD)
            return;

        appendWithTabs("<statements>");
        currLevel++;

        Set<Keyword> statementKeyword = Set.of(Keyword.IF, Keyword.WHILE, Keyword.DO, Keyword.LET, Keyword.RETURN);
        while (jt.hasMoreTokens() && jt.tokenType() == TokenType.KEYWORD && statementKeyword.contains(jt.keyword())) {
            switch (jt.keyword()) {
                case LET:
                    appendWithTabs("<letStatement>");
                    compile(this::compileLet);
                    appendWithTabs("</letStatement>");
                    break;
                case IF:
                    appendWithTabs("<ifStatement>");
                    compile(this::compileIf);
                    appendWithTabs("</ifStatement>");
                    break;
                case WHILE:
                    appendWithTabs("<whileStatement>");
                    compile(this::compileWhile);
                    appendWithTabs("</whileStatement>");
                    break;
                case RETURN:
                    appendWithTabs("<returnStatement>");
                    compile(this::compileReturn);
                    appendWithTabs("</returnStatement>");
                    break;
                case DO:
                    appendWithTabs("<doStatement>");
                    compile(this::compileDo);
                    appendWithTabs("</doStatement>");
                    break;
                default:
                    break;
            }
        }

        currLevel--;
        appendWithTabs("</statements>");
    }

    public void compileIf() {
        eat(Keyword.IF);
        eat('(');
        compileExpression();
        eat(')');
        eat('{');
        compileStatements();
        eat('}');
        if (jt.hasMoreTokens() && jt.tokenType() == TokenType.KEYWORD && jt.keyword() == Keyword.ELSE) {
            eat(Keyword.ELSE);
            eat('{');
            compileStatements();
            eat('}');
        }
    }

    public void compileLet() {
        eat(Keyword.LET);
        eat();
        if (jt.hasMoreTokens() && jt.tokenType() == TokenType.SYMBOL && jt.symbol() == '[') {
            eat('[');
            compileExpression();
            eat(']');
        }
        eat('=');
        compileExpression();
        eat(';');
    }

    public void compileDo() {
        eat(Keyword.DO);
        eat();
        if (jt.hasMoreTokens() && jt.tokenType() == TokenType.SYMBOL && jt.symbol() == '.') {
            eat('.');
            eat();
        }
        eat('(');
        compileExpressionList();
        eat(')');
        eat(';');
    }

    public void compileWhile() {
        eat(Keyword.WHILE);
        eat('(');
        compileExpression();
        eat(')');
        eat('{');
        compileStatements();
        eat('}');

    }

    public void compileReturn() {
        eat(Keyword.RETURN);
        if (jt.hasMoreTokens() && !(jt.tokenType() == TokenType.SYMBOL && jt.symbol() == ';')) {
            compileExpression();
        }
        eat(';');
    }

    public void compileExpression() {
        appendWithTabs("<expression>");

        compile(this::compileTerm);
        currLevel++;

        while (jt.hasMoreTokens() && jt.tokenType() == TokenType.SYMBOL && OP.contains(jt.symbol())) {
            eat(jt.symbol());
            compileTerm();
        }
        currLevel--;
        appendWithTabs("</expression>");
    }

    private boolean isExpression() {
        return jt.tokenType() == TokenType.IDENTIFIER || jt.tokenType() == TokenType.INT_CONST
                || jt.tokenType() == TokenType.STRING_CONST
                || (jt.tokenType() == TokenType.SYMBOL && (jt.symbol() == '(' || jt.symbol() == '~' || jt.symbol() == '-'))
                || (jt.tokenType() == TokenType.KEYWORD && KEYWORD_CONST.contains(jt.keyword()));
    }

    public void compileTerm() {
        if (!jt.hasMoreTokens())
            return;
        appendWithTabs("<term>");

        currLevel++;
        switch (jt.tokenType()) {
            case INT_CONST:
                eat(jt.intVal());
                break;
            case STRING_CONST:
                eat(jt.stringVal());
                break;
            case IDENTIFIER:
                eat();
                if (jt.hasMoreTokens() && jt.tokenType() == TokenType.SYMBOL) {
                    switch (jt.symbol()) {
                        case '[':
                            eat('[');
                            compileExpression();
                            eat(']');
                            break;
                        case '.':
                            eat('.');
                            eat();
                        case '(':
                            eat('(');
                            compileExpressionList();
                            eat(')');
                            break;
                        default:
                            break;
                    }
                }
                break;
            case KEYWORD:
                eat(jt.keyword());
                break;
            case SYMBOL:
                switch (jt.symbol()) {
                    case '(':
                        eat('(');
                        compileExpression();
                        eat(')');
                        break;
                    case '~':
                    case '-':
                        eat(jt.symbol());
                        compileTerm();
                        break;
                    default:
                        break;
                }
            default:
                break;
        }
        currLevel--;
        appendWithTabs("</term>");
    }

    public void compileExpressionList() {
        appendWithTabs("<expressionList>");
        if (jt.hasMoreTokens() && isExpression()) {
            compile(this::compileExpression);
            while (jt.hasMoreTokens() && jt.tokenType() == TokenType.SYMBOL && jt.symbol() == ',') {
                eat(',');
                compile(this::compileExpression);
            }
        }
        appendWithTabs("</expressionList>");
    }
}

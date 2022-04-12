import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class JackTokenizer {
    private final static Set<Character> SYMBOLS = Set.of('(', ')', '{', '}', '[', ']', ',', '.', ';',
            '+', '-', '*', '/', '&', '=', '~', '<', '>', '|');

    private class Token {
        TokenType tokenType;
        Keyword keyword;
        Object value;

        public Token(TokenType tokenType,
                     Keyword keyword,
                     Object value) {
            this.tokenType = tokenType;
            this.keyword = keyword;
            this.value = value;
        }

        public Token(TokenType tokenType,
                     Object value) {
            this.tokenType = tokenType;
            this.value = value;
        }
    }

    private List<Token> tokens;
    private int currToken = -1;

    public JackTokenizer(File file) {
        this.tokens = new ArrayList<>();
        try {
            Scanner sc = new Scanner(file);
            int index = 0;
            while (sc.hasNext()) {
                String line = sc.nextLine().trim();
                if (line.startsWith("/*")) {
                    if (line.endsWith("*/"))
                        continue;
                    else {
                        while (sc.hasNext() && !line.startsWith("*/"))
                            line = sc.nextLine().trim();
                        index = 1;
                    }
                }
                String[] splitted = line.trim().split("//|\\*/");
                if (index == 1 && splitted.length < 2)
                    index = 0;
                String currLine = splitted.length > 0 ? splitted[index].trim() : "";
                if (!currLine.isBlank())
                    parse(currLine);
            }
            sc.close();
        } catch (FileNotFoundException e) {
            System.out.println("A scanner error occurred.");
            e.printStackTrace();
        }
    }

    private void addToken(TokenType tp, String line, int firstInd, int lastInd) {
        if (tp == null) {
            String str = line.substring(firstInd, lastInd);
            if (Keyword.lookup(str) != null)
                tokens.add(new Token(TokenType.KEYWORD, Keyword.lookup(str), str));
            else
                tokens.add(new Token(TokenType.IDENTIFIER, str));
            return;
        }

        switch (tp) {
            case STRING_CONST:
                tokens.add(new Token(tp, line.substring(firstInd, lastInd)));
                break;
            case INT_CONST:
                tokens.add(new Token(tp, Integer.valueOf(line.substring(firstInd, lastInd))));
                break;
            case SYMBOL:
                tokens.add(new Token(tp, line.charAt(firstInd)));
                break;
            default:
                break;
        }
    }

    private void parse(String currLine) {
        TokenType tp = null;
        int firstInd = -1;
        for (int i = 0; i < currLine.length(); i++) {
            if (currLine.charAt(i) == ' ' && firstInd == -1) {
                continue;
            } else if (currLine.charAt(i) == '"' && tp == null) {
                firstInd = ++i;
                while (i < currLine.length() && currLine.charAt(i) != '"') i++;
                addToken(TokenType.STRING_CONST, currLine, firstInd, i);
                firstInd = -1;
            } else if (SYMBOLS.contains(currLine.charAt(i))) {
                if (firstInd != -1) {
                    addToken(tp, currLine, firstInd, i);
                }
                addToken(TokenType.SYMBOL, currLine, i, i);
                firstInd = -1;
                tp = null;
            } else if (firstInd == -1) {
                firstInd = i;
                if (Character.isDigit(currLine.charAt(i))) {
                    tp = TokenType.INT_CONST;
                }
            } else if (currLine.charAt(i) == ' ') {
                addToken(tp, currLine, firstInd, i);
                firstInd = -1;
                tp = null;
            }
        }
    }

    public boolean hasMoreTokens() {
        return tokens.size() > 0 && currToken < tokens.size();
    }

    public void advance() {
        currToken++;
    }

    public TokenType tokenType() {
        if (currToken >= tokens.size())
            throw new IndexOutOfBoundsException(currToken + "is out of bound for size " + tokens.size());
        return tokens.get(currToken).tokenType;
    }

    public Keyword keyword() {
        if (currToken >= tokens.size())
            throw new IndexOutOfBoundsException(currToken + "is out of bound for size " + tokens.size());
        if (tokens.get(currToken).tokenType != TokenType.KEYWORD)
            throw new UnsupportedOperationException("token type is not a keyword");
        return tokens.get(currToken).keyword;
    }

    public char symbol() {
        if (currToken >= tokens.size())
            throw new IndexOutOfBoundsException(currToken + "is out of bound for size " + tokens.size());
        if (tokens.get(currToken).tokenType != TokenType.SYMBOL)
            throw new UnsupportedOperationException("token type is not a symbol");
        return (char) tokens.get(currToken).value;
    }

    public int intVal() {
        if (currToken >= tokens.size())
            throw new IndexOutOfBoundsException(currToken + "is out of bound for size " + tokens.size());
        if (tokens.get(currToken).tokenType != TokenType.INT_CONST)
            throw new UnsupportedOperationException("token type is not an integer");
        return (int) tokens.get(currToken).value;
    }

    public String identifier() {
        if (currToken >= tokens.size())
            throw new IndexOutOfBoundsException(currToken + "is out of bound for size " + tokens.size());
        if (tokens.get(currToken).tokenType != TokenType.IDENTIFIER)
            throw new UnsupportedOperationException("token type is not an identifier");
        return (String) tokens.get(currToken).value;
    }

    public String stringVal() {
        if (currToken >= tokens.size())
            throw new IndexOutOfBoundsException(currToken + "is out of bound for size " + tokens.size());
        if (tokens.get(currToken).tokenType != TokenType.STRING_CONST)
            throw new UnsupportedOperationException("token type is not a string value");
        return (String) tokens.get(currToken).value;
    }

    public static void main(String[] args) {
        File f = new File(args[0]);
        JackTokenizer jt = new JackTokenizer(f);
        String newFileName = f.getAbsolutePath().replace(".jack", "T.xml");
        Path pathObject = Paths.get(newFileName);
        try {
            if (!Files.exists(pathObject))
                Files.createFile(pathObject);
            BufferedWriter out = Files.newBufferedWriter(pathObject, StandardOpenOption.TRUNCATE_EXISTING);
            StringBuilder sb = new StringBuilder();
            sb.append("<tokens>\n");
            jt.advance();
            while (jt.hasMoreTokens()) {
                String type = jt.tokenType().toString();
                sb.append("<").append(type).append("> ");
                switch (jt.tokenType()) {
                    case SYMBOL:
                        char expected = jt.symbol();
                        String exp = String.valueOf(expected);
                        if (expected == '<')
                            exp = "&lt;";
                        if (expected == '>')
                            exp = "&gt;";
                        if (expected == '&')
                            exp = "&amp;";
                        sb.append(exp);
                        break;
                    case KEYWORD:
                        sb.append(jt.keyword().toString());
                        break;
                    case IDENTIFIER:
                        sb.append(jt.identifier());
                        break;
                    case STRING_CONST:
                        sb.append(jt.stringVal());
                        break;
                    case INT_CONST:
                        sb.append(jt.intVal());
                        break;
                }
                sb.append(" </").append(type).append(">").append("\n");
                jt.advance();
            }
            sb.append("</tokens>");
            out.write(sb.toString());
            out.close();
        } catch (IOException e) {
            System.out.println("A file error occurred.");
            e.printStackTrace();
        }
    }
}

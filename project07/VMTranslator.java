import java.io.IOException;

public class VMTranslator {
    public static void main(String[] args) throws IOException {
        String file = args[0];
        Parser p = new Parser(file);
        CodeWriter cw = new CodeWriter(file);
        while (p.hasMoreCommands()) {
            p.advance();
            if (p.commandType() == Parser.CommandType.C_MATH)
                cw.writeArithmetic(p.getArg1());
            else if (p.commandType() == Parser.CommandType.C_POP || p.commandType() == Parser.CommandType.C_PUSH)
                cw.writePushPop(p.commandType(), p.getArg1(), p.getArg2());
        }
        cw.close();
    }
}

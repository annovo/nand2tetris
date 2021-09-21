import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class VMTranslator {
    private static void startParse(File f, CodeWriter cw) throws IOException {
        Parser p = new Parser(f);
        cw.setFileName(f);
        while (p.hasMoreCommands()) {
            p.advance();
            switch (p.commandType()) {
                case C_LABEL:
                    cw.writeLabel(p.getArg1());
                    break;
                case C_GOTO:
                    cw.writeGOTO(p.getArg1());
                    break;
                case C_IF:
                    cw.writeIF(p.getArg1());
                    break;
                case C_CALL:
                    cw.writeCall(p.getArg1(), p.getArg2());
                    break;
                case C_PUSH:
                case C_POP:
                    cw.writePushPop(p.commandType(), p.getArg1(), p.getArg2());
                    break;
                case C_FUNC:
                    cw.writeFunction(p.getArg1(), p.getArg2());
                    break;
                case C_MATH:
                    cw.writeArithmetic(p.getArg1());
                    break;
                case C_RETURN:
                    cw.writeReturn();
                    break;
                default:
                    throw new IllegalArgumentException("no such command");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);
        CodeWriter cw = new CodeWriter(file);
        cw.writeInit();
        if (file.isFile()) {
            startParse(file, cw);
        } else if (file.isDirectory()) {
            File[] matchingFiles = file.listFiles((dir, name) -> name.endsWith("vm"));
            if (matchingFiles == null)
                return;
            Arrays.stream(matchingFiles).forEach(f -> {
                try {
                    startParse(f, cw);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        cw.close();
    }
}

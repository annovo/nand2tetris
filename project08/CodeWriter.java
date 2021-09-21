import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class CodeWriter {
    private final String newLine = System.lineSeparator();
    private final BufferedWriter out;
    private int condNum = 0;
    private String funcName;
    private String fileName;
    private int returnCnt;
    private final String push = "@SP".concat(newLine)
            .concat("AM=M+1").concat(newLine)
            .concat("A=A-1").concat(newLine)
            .concat("M=D").concat(newLine);


    private enum Arithmetics {
        neg, add, not, sub, and, or, eq, lt, gt
    }

    private enum Labels {
        LCL("local"), CNST("constant"), ARG("argument"), THA("that"), THI("this"), PTN("pointer"), TMP("temp"),
        STC("static");

        private static final Map<String, Labels> BY_STRING = new HashMap<>();

        static {
            for (Labels lbl : values()) {
                BY_STRING.put(lbl.str, lbl);
            }
        }

        private final String str;

        public static Labels valueOfStr(String s) {
            return BY_STRING.get(s);
        }

        Labels(String s) {
            str = s;
        }
    }

    public CodeWriter(File file) throws IOException {
        String newFileName = file.isDirectory() ? file.getAbsolutePath() + System.getProperty("file.separator") + file.getName() + ".asm" : file.getAbsolutePath().replace(".vm", ".asm");
        this.fileName = file.getName();
        Path pathObject = Paths.get(newFileName);
        if (!Files.exists(pathObject))
            Files.createFile(pathObject);
        out = Files.newBufferedWriter(pathObject, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void setFileName(File file) {
        fileName = file.getName().replaceFirst("\\.vm", "");
    }

    public void writeInit() throws IOException {
        out.write(
                "@256".concat(newLine)
                        .concat("D=A").concat(newLine)
                        .concat("@SP").concat(newLine)
                        .concat("M=D").concat(newLine)
        );
        writeCall("Sys.init", 0);
    }

    public void writeLabel(String label) throws IOException {
        if (this.funcName == null)
            this.funcName = this.fileName;
        out.write("(".concat(this.funcName).concat("$").concat(label).concat(")").concat(newLine));
    }

    public void writeIF(String label) throws IOException {
        if (this.funcName == null)
            this.funcName = this.fileName;
        out.write("@SP".concat(newLine)
                .concat("AM=M-1").concat(newLine)
                .concat("D=M").concat(newLine)
                .concat("@").concat(this.funcName).concat("$").concat(label).concat(newLine)
                .concat("D;JNE").concat(newLine));
    }

    public void writeGOTO(String label) throws IOException {
        if (this.funcName == null)
            this.funcName = this.fileName;
        out.write("@".concat(this.funcName).concat("$").concat(label).concat(newLine)
                .concat("0;JMP").concat(newLine));
    }

    public void writeFunction(String funcName, int arg) throws IOException {
        this.funcName = funcName;
        String initLoop = "INIT_ARG_LOOP_" + this.funcName;
        String endLoop = "INIT_ARG_LOOP_END_" + this.funcName;
        this.returnCnt = 0;
        out.write("(".concat(funcName).concat(")").concat(newLine)
                .concat("@").concat(String.valueOf(arg)).concat(newLine)
                .concat("D=A").concat(newLine)
                .concat("@").concat(endLoop).concat(newLine)
                .concat("D;JEQ").concat(newLine)
                .concat("@R14").concat(newLine)
                .concat("M=D").concat(newLine)
                .concat("(").concat(initLoop).concat(")").concat(newLine)
                .concat("@SP").concat(newLine)
                .concat("AM=M+1").concat(newLine)
                .concat("A=A-1").concat(newLine)
                .concat("M=0").concat(newLine)
                .concat("@R14").concat(newLine)
                .concat("MD=M-1").concat(newLine)
                .concat("@").concat(initLoop).concat(newLine)
                .concat("D;JGT").concat(newLine)
                .concat("(").concat(endLoop).concat(")").concat(newLine)
        );
    }

    public void writeCall(String funcName, int arg) throws IOException {
        if (this.funcName == null)
            this.funcName = this.fileName;
        String retlbl = this.funcName + "$ret." + this.returnCnt;
        String looplbl = "CALL_ARG_INIT_LOOP_" + this.funcName + "." + funcName + "." + this.returnCnt;
        String endLoop = "CALL_ARG_INIT_LOOP_END" + this.funcName + "." + funcName + "." + this.returnCnt++;
        out.write("@SP".concat(newLine)
                .concat("D=M").concat(newLine)
                .concat("@R14").concat(newLine)
                .concat("M=D").concat(newLine)
                .concat("@").concat(retlbl).concat(newLine)
                .concat("D=A").concat(newLine)
                .concat(push)
                .concat("@LCL").concat(newLine)
                .concat("D=M").concat(newLine)
                .concat(push)
                .concat("@ARG").concat(newLine)
                .concat("D=M").concat(newLine)
                .concat(push)
                .concat("@THIS").concat(newLine)
                .concat("D=M").concat(newLine)
                .concat(push)
                .concat("@THAT").concat(newLine)
                .concat("D=M").concat(newLine)
                .concat(push)
                .concat("@SP").concat(newLine)
                .concat("D=M").concat(newLine)
                .concat("@LCL").concat(newLine)
                .concat("M=D").concat(newLine)
                .concat("@").concat(String.valueOf(arg)).concat(newLine)
                .concat("D=A").concat(newLine)
                .concat("@").concat(endLoop).concat(newLine)
                .concat("D;JEQ").concat(newLine)
                .concat("@R13").concat(newLine)
                .concat("M=D").concat(newLine)
                .concat("(").concat(looplbl).concat(")").concat(newLine)
                .concat("@R14").concat(newLine)
                .concat("M=M-1").concat(newLine)
                .concat("@R13").concat(newLine)
                .concat("MD=M-1").concat(newLine)
                .concat("@").concat(looplbl).concat(newLine)
                .concat("D;JGT").concat(newLine)
                .concat("(").concat(endLoop).concat(")").concat(newLine)
                .concat("@R14").concat(newLine)
                .concat("D=M").concat(newLine)
                .concat("@ARG").concat(newLine)
                .concat("M=D").concat(newLine)
                .concat("@").concat(funcName).concat(newLine)
                .concat("0;JMP").concat(newLine)
                .concat("(").concat(retlbl).concat(")").concat(newLine)
        );
    }

    public void writeReturn() throws IOException { // save return addr or arg = 0 erase it from mem
        String retAdrr = "@R13".concat(newLine)
                .concat("AM=M-1").concat(newLine)
                .concat("D=M").concat(newLine);
        out.write("@LCL".concat(newLine)
                .concat("D=M").concat(newLine)
                .concat("@R13").concat(newLine)
                .concat("M=D").concat(newLine)
                .concat("@5").concat(newLine)
                .concat("A=D-A").concat(newLine)
                .concat("D=M").concat(newLine)
                .concat("@R14").concat(newLine)
                .concat("M=D").concat(newLine)
                .concat("@SP").concat(newLine)
                .concat("AM=M-1").concat(newLine)
                .concat("D=M").concat(newLine)
                .concat("@ARG").concat(newLine)
                .concat("A=M").concat(newLine)
                .concat("M=D").concat(newLine)
                .concat("D=A+1").concat(newLine)
                .concat("@SP").concat(newLine)
                .concat("M=D").concat(newLine)
                .concat(retAdrr)
                .concat("@THAT").concat(newLine)
                .concat("M=D").concat(newLine)
                .concat(retAdrr)
                .concat("@THIS").concat(newLine)
                .concat("M=D").concat(newLine)
                .concat(retAdrr)
                .concat("@ARG").concat(newLine)
                .concat("M=D").concat(newLine)
                .concat(retAdrr)
                .concat("@LCL").concat(newLine)
                .concat("M=D").concat(newLine)
                .concat("@R14").concat(newLine)
                .concat("A=M;JMP").concat(newLine)
        );
    }


    public void writeArithmetic(String command) throws IOException {
        StringBuilder s = new StringBuilder();
        String condLabel = "SYS_ASM_CONTINUE_";
        String pop = "@SP".concat(newLine)
                .concat("AM=M-1").concat(newLine)
                .concat("D=M").concat(newLine)
                .concat("A=A-1").concat(newLine);
        String cond = "D=M-D".concat(newLine)
                .concat("M=-1").concat(newLine);
        String falseCond = "@SP".concat(newLine)
                .concat("A=M-1").concat(newLine)
                .concat("M=0").concat(newLine);
        s.append("// ").append(command).append(System.lineSeparator());
        switch (Arithmetics.valueOf(command)) {
            case add:
                s.append(pop).append("M=M+D").append(newLine);
                break;
            case sub:
                s.append(pop).append("M=M-D").append(newLine);
                break;
            case neg:
                s.append("@SP").append(newLine)
                        .append("A=M-1").append(newLine)
                        .append("M=-M").append(newLine);
                break;
            case not:
                s.append("@SP").append(newLine)
                        .append("A=M-1").append(newLine)
                        .append("M=!M").append(newLine);
                break;
            case and:
                s.append(pop).append("M=M&D").append(newLine);
                break;
            case or:
                s.append(pop).append("M=M|D").append(newLine);
                break;
            case eq:
                s.append(pop).append(cond)
                        .append("@").append(condLabel).append(condNum).append(newLine)
                        .append("D;JEQ").append(newLine)
                        .append(falseCond)
                        .append("(").append(condLabel).append(condNum++).append(")").append(newLine);
                break;
            case lt:
                s.append(pop).append(cond)
                        .append("@").append(condLabel).append(condNum).append(newLine)
                        .append("D;JLT").append(newLine)
                        .append(falseCond)
                        .append("(").append(condLabel).append(condNum++).append(")").append(newLine);
                break;
            case gt:
                s.append(pop).append(cond)
                        .append("@").append(condLabel).append(condNum).append(newLine)
                        .append("D;JGT").append(newLine)
                        .append(falseCond)
                        .append("(").append(condLabel).append(condNum++).append(")").append(newLine);
                break;
            default:
                throw new IllegalArgumentException("this is not arithmetic command");
        }
        out.write(s.toString());
    }

    private void appendPushPopLong(StringBuilder s, String name, String pop, int a, Parser.CommandType ct) {
        if (a == 0) {
            if (ct == Parser.CommandType.C_PUSH)
                s.append("@").append(name).append(newLine)
                        .append("A=M").append(newLine)
                        .append("D=M").append(newLine);
            else if (ct == Parser.CommandType.C_POP)
                s.append(pop)
                        .append("@").append(name).append(newLine)
                        .append("A=M").append(newLine)
                        .append("M=D").append(newLine);
            else
                throw new IllegalArgumentException("not a push/pop command");
            return;
        }

        s.append("@").append(name).append(newLine)
                .append("D=M").append(newLine)
                .append("@").append(a).append(newLine);

        if (ct == Parser.CommandType.C_PUSH)
            s.append("A=A+D").append(newLine).
                    append("D=M").append(newLine);
        else if (ct == Parser.CommandType.C_POP)
            s.append("D=A+D").append(newLine)
                    .append("@R13").append(newLine)
                    .append("M=D").append(newLine)
                    .append(pop)
                    .append("@R13").append(newLine)
                    .append("A=M").append(newLine)
                    .append("M=D").append(newLine);
        else
            throw new IllegalArgumentException("not a push/pop command");
    }

    private void appendPushPopShort(StringBuilder s, String name, String pop, Parser.CommandType ct) {
        if (ct == Parser.CommandType.C_PUSH)
            s.append("@").append(name).append(newLine)
                    .append("D=M").append(newLine);
        else if (ct == Parser.CommandType.C_POP)
            s.append(pop)
                    .append("@").append(name).append(newLine)
                    .append("M=D").append(newLine);
        else
            throw new IllegalArgumentException("not a push/pop command");
    }

    public void writePushPop(Parser.CommandType ct, String where, int arg) throws IOException {
        StringBuilder s = new StringBuilder();
        String pop = "@SP".concat(newLine)
                .concat("AM=M-1").concat(newLine)
                .concat("D=M").concat(newLine);
        s.append("// ").append(ct).append(" ").append(where).append(" ").append(arg).append(newLine);

        switch (Labels.valueOfStr(where)) {
            case CNST:
                s.append("@").append(arg).append(newLine)
                        .append("D=A").append(newLine);
                break;
            case LCL:
                appendPushPopLong(s, "LCL", pop, arg, ct);
                break;
            case ARG:
                appendPushPopLong(s, "ARG", pop, arg, ct);
                break;
            case THA:
                appendPushPopLong(s, "THAT", pop, arg, ct);
                break;
            case THI:
                appendPushPopLong(s, "THIS", pop, arg, ct);
                break;
            case STC:
                appendPushPopShort(s, fileName + "." + arg, pop, ct);
                break;
            case TMP:
                appendPushPopShort(s, String.valueOf(5 + arg), pop, ct); // temp vars stored from addr 5
                break;
            case PTN: {
                String p;
                if (arg == 0)
                    p = "THIS";
                else if (arg == 1)
                    p = "THAT";
                else
                    throw new IllegalArgumentException("no such pointer");

                appendPushPopShort(s, p, pop, ct);
                break;
            }
            default:
                throw new IllegalArgumentException("no such label");
        }

        if (ct == Parser.CommandType.C_PUSH)
            s.append(push);

        out.write(s.toString());
    }

    public void close() throws IOException {
        out.close();
    }
}

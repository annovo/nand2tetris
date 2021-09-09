import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class CodeWriter {
    //private final File file;
    private final BufferedWriter out;
    private int lineNum;
    private final String funcName;

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

    public CodeWriter(String file) throws IOException {
        String newFileName = file.substring(0, file.lastIndexOf(".")) + ".asm";
        String[] arr = file.split("\\/|\\.");
        funcName = arr[arr.length - 2];
        Path pathObject = Paths.get(newFileName);
        if (!Files.exists(pathObject))
            Files.createFile(pathObject);
        out = Files.newBufferedWriter(pathObject, StandardOpenOption.TRUNCATE_EXISTING);
        lineNum = 0;
    }

    public void writeArithmetic(String command) throws IOException {
        StringBuilder s = new StringBuilder();
        String pop = "@SP\nM=M-1\nA=M\nD=M\nA=A-1\n";
        s.append("// ").append(command).append("\n");
        switch (Arithmetics.valueOf(command)) {
            case add: {
                s.append(pop).append("M=M+D\n");
                lineNum += 6;
                break;
            }
            case sub: {
                s.append(pop).append("M=M-D\n");
                lineNum += 6;
                break;
            }
            case neg: {
                s.append("@SP\nA=M-1\nM=-M\n");
                lineNum += 3;
                break;
            }
            case not: {
                s.append("@SP\nA=M-1\nM=!M\n");
                lineNum += 3;
                break;
            }
            case and: {
                s.append(pop).append("M=M&D\n");
                lineNum += 6;
                break;
            }
            case or: {
                s.append(pop).append("M=M|D\n");
                lineNum += 6;
                break;
            }
            case eq: {
                s.append(pop).append("D=M-D\n").append("M=-1\n");
                lineNum += 12;
                s.append("@").append(lineNum).append("\n").append("D;JEQ\n@SP\nA=M-1\nM=0\n");
                break;
            }
            case lt: {
                s.append(pop).append("D=M-D\n").append("M=-1\n");
                lineNum += 12;
                s.append("@").append(lineNum).append("\n").append("D;JLT\n@SP\nA=M-1\nM=0\n");
                break;
            }
            case gt: {
                s.append(pop).append("D=M-D\n").append("M=-1\n");
                lineNum += 12;
                s.append("@").append(lineNum).append("\n").append("D;JGT\n@SP\nA=M-1\nM=0\n");
                break;
            }
            default:
                throw new IllegalArgumentException("this is not arithmetic command");
        }
        out.write(s.toString());
    }

    private void appendPushPopLong(StringBuilder s, String name, String pop, int a, Parser.CommandType ct) {
        if (a == 0) {
            if (ct == Parser.CommandType.C_PUSH) {
                s.append("@").append(name).append("\nA=M\nD=M\n");
                lineNum += 3;
            } else if (ct == Parser.CommandType.C_POP) {
                s.append("@SP\nAM=M-1\nD=M\n").append("@").append(name).append("\nA=M\nM=D\n");
                lineNum += 6;
            } else
                throw new IllegalArgumentException("not a push/pop command");
            return;
        }

        s.append("@").append(name).append("\nD=M\n@").append(a).append("\nA=A+D\n");
        if (ct == Parser.CommandType.C_PUSH) {
            s.append("D=M\n");
            lineNum += 5;
        } else if (ct == Parser.CommandType.C_POP) {
            s.append("D=A\n").append(pop);
            lineNum += 14;
        } else
            throw new IllegalArgumentException("not a push/pop command");
    }

    private void appendPushPopShort(StringBuilder s, String name, Parser.CommandType ct) {
        if (ct == Parser.CommandType.C_PUSH) {
            s.append("@").append(name).append("\n").append("D=M\n");
            lineNum += 2;
        } else if (ct == Parser.CommandType.C_POP) {
            s.append("@SP\nAM=M-1\nD=M\n").append("@").append(name).append("\nM=D\n");
            lineNum += 5;
        } else
            throw new IllegalArgumentException("not a push/pop command");
    }

    public void writePushPop(Parser.CommandType ct, String where, int arg) throws IOException {
        StringBuilder s = new StringBuilder();
        String push = "@SP\nA=M\nM=D\n@SP\nM=M+1\n";
        String pop = "@SP\nM=M-1\nA=M+1\nM=D\nA=A-1\nD=M\nA=A+1\nA=M\nM=D\n";
        s.append("// ").append(ct).append(" ").append(where).append(" ").append(arg).append("\n");

        switch (Labels.valueOfStr(where)) {
            case CNST: {
                s.append("@").append(arg).append("\nD=A\n");
                lineNum += 2;
                break;
            }
            case LCL: {
                appendPushPopLong(s, "LCL", pop, arg, ct);
                break;
            }
            case ARG: {
                appendPushPopLong(s, "ARG", pop, arg, ct);
                break;
            }
            case THA: {
                appendPushPopLong(s, "THAT", pop, arg, ct);
                break;
            }
            case THI: {
                appendPushPopLong(s, "THIS", pop, arg, ct);
                break;
            }
            case STC: {
                appendPushPopShort(s, funcName + "." + arg, ct);
                break;
            }
            case TMP: {
                appendPushPopLong(s, "5", pop, arg, ct);
                break;
            }
            case PTN: {
                String p;
                if (arg == 0)
                    p = "THIS";
                else if (arg == 1)
                    p = "THAT";
                else
                    throw new IllegalArgumentException("no such pointer");

                appendPushPopShort(s, p, ct);
                break;
            }
            default:
                throw new IllegalArgumentException("no such label");
        }

        if (ct == Parser.CommandType.C_PUSH) {
            s.append(push);
            lineNum += 5;
        }
        out.write(s.toString());
    }

    public void close() throws IOException {
        out.close();
    }
}

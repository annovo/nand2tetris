import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {
    private final List<String> file;
    private int currLine = 0;
    private CommandType currCommand;
    private String arg1;
    private int arg2;

    public enum CommandType {
        C_MATH("arithmetic"), C_PUSH("push"), C_POP("pop"), C_LABEL("label"), C_GOTO("goto"), C_IF("if-goto"),
        C_FUNC("function"), C_RETURN("return"), C_CALL("call");

        private static final Map<String, CommandType> BY_LABEL = new HashMap<>();

        static {
            for (CommandType ct : values())
                BY_LABEL.put(ct.lbl, ct);
        }

        public static CommandType valueOfLbl(String s) {
            return BY_LABEL.get(s);
        }

        private final String lbl;

        CommandType(String label) {
            this.lbl = label;
        }
    }

    public Parser(File f) throws IOException {
        this.file = Files.readAllLines(f.toPath());
    }

    public void advance() {
        String line;
        do {
            line = file.get(currLine++).trim();
            String[] lines = line.split("//");
            line = lines.length != 0 ? lines[0].trim() : "";
        } while (hasMoreCommands() && line.isBlank());
        parse(line);
    }

    private void parse(String line) {
        String[] splitted = line.split(" ");
        switch (splitted.length) {
            case 1: {
                if (CommandType.valueOfLbl(splitted[0]) == CommandType.C_RETURN)
                    currCommand = CommandType.C_RETURN;
                else {
                    currCommand = CommandType.C_MATH;
                    arg1 = splitted[0];
                }
                break;
            }
            case 2: {
                switch (CommandType.valueOfLbl(splitted[0])) {
                    case C_IF:
                        currCommand = CommandType.C_IF;
                        break;
                    case C_GOTO:
                        currCommand = CommandType.C_GOTO;
                        break;
                    case C_LABEL:
                        currCommand = CommandType.C_LABEL;
                        break;
                    default:
                        throw new IllegalArgumentException("no such command");
                }
                arg1 = splitted[1];
                break;
            }
            case 3: {
                switch (CommandType.valueOfLbl(splitted[0])) {
                    case C_POP:
                        currCommand = CommandType.C_POP;
                        break;
                    case C_PUSH:
                        currCommand = CommandType.C_PUSH;
                        break;
                    case C_FUNC:
                        currCommand = CommandType.C_FUNC;
                        break;
                    case C_CALL:
                        currCommand = CommandType.C_CALL;
                        break;
                    default:
                        throw new IllegalArgumentException("no such command");
                }
                arg1 = splitted[1];
                arg2 = Integer.parseInt(splitted[2]);
                break;
            }
            default:
                throw new IllegalArgumentException("no such command");
        }
    }

    public CommandType commandType() {
        return currCommand;
    }

    public String getArg1() {
        if (currCommand == CommandType.C_RETURN)
            throw new IllegalArgumentException("cannot be called when command is return");
        return arg1;
    }

    public int getArg2() {
        if (currCommand != CommandType.C_FUNC && currCommand != CommandType.C_CALL && currCommand != CommandType.C_POP && currCommand != CommandType.C_PUSH)
            throw new IllegalArgumentException("should be called only with push/pop/call/function commands");
        return arg2;
    }

    public boolean hasMoreCommands() {
        return currLine < file.size();
    }

    public static void main(String[] args) {
    }
}

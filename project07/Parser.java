import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        C_MATH("arithmetic"), C_PUSH("push"), C_POP("pop"), C_LABEL("label"), C_GOTO("goto"), C_IF("if"),
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

    public Parser(String f) throws IOException {
        this.file = Files.readAllLines(Paths.get(f));
    }

    public void advance() {
        if (hasMoreCommands()) {
            String line = file.get(currLine++).trim();
            if (line.contains("//"))
                line = line.split("//")[1].trim();
            if (line.isBlank())
                advance();
            else
                parse(line);
        }
    }

    private void parse(String line) {
        String[] splitted = line.split(" ");
        if (splitted.length == 1) {
            currCommand = CommandType.C_MATH;
            arg1 = splitted[0];
        } else if (CommandType.valueOfLbl(splitted[0]) == CommandType.C_PUSH && splitted.length == 3) {
            currCommand = CommandType.C_PUSH;
            arg1 = splitted[1];
            arg2 = Integer.parseInt(splitted[2]);
        } else if (CommandType.valueOfLbl(splitted[0]) == CommandType.C_POP && splitted.length == 3) {
            currCommand = CommandType.C_POP;
            arg1 = splitted[1];
            arg2 = Integer.parseInt(splitted[2]);
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

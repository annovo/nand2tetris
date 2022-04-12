import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class JackAnalyzer {

    private static void write(PrintWriter out, File in) {
        JackTokenizer jt = new JackTokenizer(in);
        CompilationEngine ce = new CompilationEngine(out, jt);
        ce.compileClass();
    }

    private static void parse(File f) {
        String newFileName = f.getAbsolutePath().replace(".jack", "-cmp.xml");
        Path pathObject = Paths.get(newFileName);
        try {
            if (!Files.exists(pathObject))
                Files.createFile(pathObject);
            BufferedWriter bw = Files.newBufferedWriter(pathObject, StandardOpenOption.TRUNCATE_EXISTING);
            PrintWriter out = new PrintWriter(bw);
            write(out, f);
            out.close();
        } catch (IOException e) {
            System.out.println("A file error occurred.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        File file = new File(args[0]);
        if (file.isFile()) {
            parse(file);
        } else if (file.isDirectory()) {
            File[] matchingFiles = file.listFiles((dir, name) -> name.endsWith("jack"));
            if (matchingFiles == null)
                return;
            Arrays.stream(matchingFiles).forEach(f -> {
                parse(f);
            });
        }
    }
}

package src.scripts;

import java.io.File;

import org.evosuite.EvoSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Runs evosuite on a given classfile */
public class EvoRunner {
    private static transient final Logger evoLogger = LoggerFactory.getLogger("EvoRunner");

    public static void runEvosuite(File classFile, File generatedTestDir) {
        File parentDir = classFile.getParentFile();
        if (parentDir == null) {
            evoLogger.error("Cannot find parent path for classfile ", classFile.toString(), " as ",
                    classFile.getParent().toString(), "; Aborting.");
            return;
        }

        String[] arguments = new String[] {
                "-Dcriterion=BRANCH",
                String.format("-projectCP=%s", parentDir.toString()),
                String.format("-class=%s", classFile.getName()),
                String.format("-Doutput_dir='%s'", generatedTestDir.toString())
        };

        EvoSuite.main(arguments);
    }
}

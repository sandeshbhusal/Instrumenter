package src;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.*;

import ch.qos.logback.classic.LoggerContext;
import src.scripts.EvoRunner;
import src.visitors.InstanceVariableVisitor;
import src.visitors.InstrumentingVisitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;

import javax.tools.*;

public class App {
    final static Logger logger = LoggerFactory.getLogger("Instrumenter");

    public static void main(String[] args) {
        if (args.length == 0) {
            logger.error("No arguments passed. Shutting down..");
            return;
        }

        File scanDir = new File(args[0]);
        logger.info("Passed source path: " + scanDir.getAbsolutePath());

        // Make an output directory based on the source path, copy over everything
        // to the new directory.
        File parentPath = scanDir.getParentFile();

        Path Instrumented;
        Path ifInstrumented, collectorInstrumented;

        if (parentPath != null) {
            Instrumented = Paths.get(parentPath.getAbsolutePath(), "Instrumented");
            if (Instrumented.toFile().exists()) {
                logger.error("Instrumented code from previous runs exists. Remove it and run again.");
                return;
            } else {
                if (!Instrumented.toFile().mkdir()) {
                    logger.error("Could not create instrumented code directory. Aborting.");
                    return;
                }
            }

            ifInstrumented = Paths.get(Instrumented.toString(), "IfInstrumented");
            collectorInstrumented = Paths.get(Instrumented.toString(), "CollectorInstrumented");
        } else {
            logger.error("Could not extract a root parent path. This should never happen. Aborting.");
            return;
        }

        logger.info("Creating " + ifInstrumented.toString() + " directory to store branch instrumented code");
        logger.info("Creating " + collectorInstrumented.toString() + " directory to store reporter instrumented code");

        if (!ifInstrumented.toFile().mkdir() || !collectorInstrumented.toFile().mkdir()) {
            logger.error("Could not create either IfInstrumented dir, or CollectorInstrumented dir.");
            logger.error("Aborting.");
            return;
        }

        ArrayList<File> filesToBeCompiled = new ArrayList<>();

        // Begin by copying over all original code to the branchInstrumented directory.
        // We begin with original code because we don't want to modify the branches yet.
        Collection<File> collectedFiles = FileUtils.listFiles(scanDir, null, true);
        for (File inputFile : collectedFiles) {
            if (inputFile.toString().endsWith(".java")) {
                // Copy it over to the branch-instrumentation dir.
                File destination = Paths.get(ifInstrumented.toString(), inputFile.getName()).toFile();
                logger.debug("Copying " + inputFile.toString() + " to " + destination.toString());

                try {
                    FileUtils.copyFile(inputFile, destination);

                    filesToBeCompiled.add(inputFile);
                } catch (IOException e) {
                    logger.error("Could not copy over file " + inputFile.toString() + ". Stack trace: " + e);
                    continue;
                }
            }
        }

        // In a loop, we run the instrumentation.
        int loopCount = 1;

        while (--loopCount >= 0) {
            // Begin by generating a temporary compilation directory.
            File compileDir = Paths.get(FileUtils.getTempDirectory().getAbsolutePath(), "compiled" + loopCount)
                    .toFile();

            if (compileDir.exists()) {
                if (!compileDir.delete()) {
                    logger.error("Compilation directory existed - tried to clean up but failed.");
                    return;
                }
            }

            if (!compileDir.mkdir()) {
                logger.error("Could not create a temp compilation directory");
                return;
            }

            logger.info("Round " + loopCount + " compiling to " + compileDir.toString());

            // Run the compilation here.
            for (File file : filesToBeCompiled) {
                ArrayList<String> argsToJavac = new ArrayList<>();
                argsToJavac.add("bash");
                argsToJavac.add("/home/sandesh/Workspace/thesis/PoCs/AST/astMod/src/main/java/src/scripts");

                try {
                    new ProcessBuilder(argsToJavac).start().wait();

                } catch (InterruptedException | IOException e) {
                    logger.error("Compilation of file", file.toString(), "failed with exception", e.toString());
                }

                String contents;
                try {
                    contents = new String(FileUtils.readFileToByteArray(file));
                    ASTParser parser = ASTParser.newParser(AST.JLS8);
                    parser.setSource(contents.toCharArray());

                    CompilationUnit unit = (CompilationUnit) parser.createAST(null);
                    unit.accept(new ASTVisitor() {
                        @Override
                        public boolean visit(TypeDeclaration node) {
                            InstanceVariableVisitor visitor = new InstanceVariableVisitor(node.getName().toString());

                            node.accept(visitor);
                            List<InstanceVariableVisitor.VariableDeclaration> decls = visitor.declarations;
                            for (InstanceVariableVisitor.VariableDeclaration decl : decls) {
                                System.out.println(decl.className + "." + decl.name + ":" + decl.type.toString());
                            }

                            return true;
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }

            Collection<File> classFiles = FileUtils.listFiles(compileDir, new String[] {"class"}, true);

            // At this point, the compileDir contains all the compiled classfiles.
            // Run Evosuite on the compiled classfiles.
            // Evosuite SHOULD NOT run on the collectorInstrumented Files because they may
            // introduce
            // additional overhead to the entire search process in Evosuite.

            // Run evosuite and generate testcases here.
            for (File classFile: classFiles) {
                String absName = classFile.getAbsolutePath().toString();
                String onlyFileName = absName.substring(0, absName.length() - ".class".length());

                EvoRunner.runEvosuite(Paths.get(onlyFileName).toFile(), compileDir);
            }

            // Run evosuite-generated testcases on the branchInstrumented source code.

            // Trigger collector to generate branch-instrumented code.

            // Switch the new branch-instrumented code with the old branch-instrumented
            // code.

        }

        logger.info("Finished visiting files.");
        return;
    }
}

class CollectorInstrumentationConsumer implements Consumer<File> {
    final static Logger logger = LoggerFactory.getLogger(CollectorInstrumentationConsumer.class.getName());

    CollectorInstrumentationConsumer() {

    }

    @Override
    public void accept(File inputFile) {
        if (inputFile.getAbsolutePath().endsWith(".java")) {
            logger.info("Working on " + inputFile.toString());

            try {
                String contents = new String(FileUtils.readFileToByteArray(inputFile));

                @SuppressWarnings("deprecation") // AST.JLS(K), where K < current JDK version is always
                                                 // deprecated.
                ASTParser parser = ASTParser.newParser(AST.JLS17);

                parser.setSource(contents.toCharArray());
                parser.setKind(ASTParser.K_COMPILATION_UNIT);

                CompilationUnit unit = (CompilationUnit) parser.createAST(null);
                AST ast = unit.getAST();

                ASTRewrite rewrite = ASTRewrite.create(ast);
                InstrumentingVisitor instrumenter = new InstrumentingVisitor(rewrite);

                unit.accept(instrumenter);

                Document document = new Document();
                document.set(contents);

                ImportDeclaration importDeclaration = ast.newImportDeclaration();
                importDeclaration.setName(ast.newName("Instrumenter.Recorder"));

                // Get the list of existing import declarations
                ListRewrite listRewrite = rewrite.getListRewrite(unit, CompilationUnit.IMPORTS_PROPERTY);
                listRewrite.insertLast(importDeclaration, null);

                TextEdit edits = rewrite.rewriteAST(document, null);
                edits.apply(document);

                // Dump the document to a different file.
                BufferedWriter writer = new BufferedWriter(new FileWriter(inputFile.getName()));
                writer.write(document.get());
                writer.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            logger.warn(inputFile.getName() + " is not a java file. Will be copied as-is");
        }
    }
}

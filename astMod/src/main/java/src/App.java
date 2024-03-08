package src;

import java.util.Collection;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.*;

import src.visitors.InstrumentingVisitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class App {
    final static Logger logger = LoggerFactory.getLogger("Instrumenter");

    public static Path Instrumented;
    public static Path ifInstrumented;
    public static Path collectorInstrumented;

    public static void main(String[] args) {
        if (args.length == 0) {
            logger.error("No arguments passed. Shutting down..");
            return;
        }

        File scanDir = new File(args[0]);
        logger.info("Passed source path: " + scanDir.getAbsolutePath());

        File parentPath = scanDir.getParentFile();
        setupPaths(parentPath);
        copyFilesToBranchInsDir(scanDir);
        addReportingCode(ifInstrumented);

        // Start the RMI server.
        try {
            AppServer rmiServer = new AppServer(Instrumented, ifInstrumented, collectorInstrumented);
            LocateRegistry.createRegistry(1111);
            Registry rmiRegistry = LocateRegistry.getRegistry(1111);

            rmiRegistry.rebind("master", rmiServer);
            logger.info("Started RMI server at port: " + Registry.REGISTRY_PORT);

        } catch (RemoteException e) {
            logger.error("Could not start a RMI server on master node.");
            logger.error(e.toString());
            return;
        }
    }

    private static void addReportingCode(Path sourcePath) {
        Collection<File> sources = FileUtils.listFiles(sourcePath.toFile(), new String[] { "java" }, true);
        for (File source : sources) {
            try {
                String contents = new String(FileUtils.readFileToByteArray(source));

                @SuppressWarnings("deprecation") // AST.JLS(K), where K < current JDK version is always
                                                 // deprecated.
                ASTParser parser = ASTParser.newParser(AST.JLS8);

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
                importDeclaration.setName(ast.newName("src.Reporter"));

                ListRewrite listRewrite = rewrite.getListRewrite(unit, CompilationUnit.IMPORTS_PROPERTY);
                listRewrite.insertLast(importDeclaration, null);

                TextEdit edits = rewrite.rewriteAST(document, null);
                edits.apply(document);

                BufferedWriter writer = new BufferedWriter(new FileWriter(source.getAbsolutePath()));
                writer.write(document.get());
                writer.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void setupPaths(File parentPath) {
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
    }

    private static ArrayList<File> copyFilesToBranchInsDir(File scanDir) {
        ArrayList<File> rval = new ArrayList<>();
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

                    rval.add(inputFile);
                } catch (IOException e) {
                    logger.error("Could not copy over file " + inputFile.toString() + ". Stack trace: " + e);
                    continue;
                }
            }
        }

        return rval;
    }
}
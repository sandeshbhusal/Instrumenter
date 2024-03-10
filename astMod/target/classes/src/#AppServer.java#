package src;

import java.nio.file.Path;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import src.interfaces.Master;
import src.interfaces.Runner;

import org.slf4j.*;

public class AppServer extends UnicastRemoteObject implements Master {
    final static Logger logger = LoggerFactory.getLogger("MasterRMIServer");
    Path instrumented;
    Path ifInstrumented;
    Path reportInstrumented;

    protected AppServer() throws RemoteException {
        super();
    }

    public AppServer(Path instrumentedDir, Path branchInstrumentedDir, Path reportInstrumentedDir)
            throws RemoteException {
        super();

        this.instrumented = instrumentedDir;
        this.ifInstrumented = branchInstrumentedDir;
        this.reportInstrumented = reportInstrumentedDir;
    }


    @Override
    public void connect(Runner runner) throws RemoteException {
        logger.info("Client connected.");
        logger.info("Sending instrumentation paths to the client");
        runner.setBranchSourceDir(this.ifInstrumented.toFile());
        runner.setReportSourceDir(this.reportInstrumented.toFile());

        logger.info("Triggering evosuite search on the branch-Instrumented directory");
        runner.runEvoSearch();
    }

    @Override
    public void report(String className, String methodName, String branchExpr, Object... args) throws RemoteException {
        logger.info("Client says: " + String.format("class(%s).method(%s).expr(%s)", className, methodName, branchExpr));
        throw new UnsupportedOperationException("Unimplemented method 'report'");
    }
}

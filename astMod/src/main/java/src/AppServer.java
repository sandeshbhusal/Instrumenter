package src;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import src.interfaces.Master;
import src.interfaces.Runner;

import org.slf4j.*;

public class AppServer extends UnicastRemoteObject implements Master {
    final static Logger logger = LoggerFactory.getLogger("MasterRMIServer");
    
    protected AppServer() throws RemoteException {
        super();
    }

    @Override
    public void report(String className, String methodName, int branchID, Object... args) throws RemoteException {
        logger.info("Client reported metrics for " + className + "." + methodName);
    }

    @Override
    public void connect(Runner runner) throws RemoteException {
        logger.info("Client connected.");
    }
}

import java.rmi.Remote;
import java.rmi.RemoteException;

// Interface remota para os nós de dados (DataNodes)
public interface DataNodeInterface extends Remote {
    boolean uploadPart(String imageName, int partNumber, byte[] data) throws RemoteException;
    byte[] downloadPart(String imageName, int partNumber) throws RemoteException;
    boolean deletePart(String imageName, int partNumber) throws RemoteException;
    boolean ping() throws RemoteException;
}

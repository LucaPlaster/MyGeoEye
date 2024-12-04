import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

// Interface remota para o servidor mestre
public interface MasterServerInterface extends Remote {
    void registerDataNode(String dataNodeId, DataNodeInterface dataNode) throws RemoteException;
    void unregisterDataNode(String dataNodeId) throws RemoteException;
    List<String> listImages() throws RemoteException;
    Map<Integer, DataNodeInterface> getImageParts(String imageName) throws RemoteException;
    boolean storeImage(String imageName, byte[] imageData, int numParts) throws RemoteException;
    boolean deleteImage(String imageName) throws RemoteException;
}

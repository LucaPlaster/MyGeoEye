import java.rmi.Remote;
import java.rmi.RemoteException;

// Interface remota para o serviço de monitoramento
public interface MonitorServiceInterface extends Remote {
    void notifyFailure(String dataNodeId) throws RemoteException;
    void registerMasterServer(MasterServerInterface masterServer) throws RemoteException;
}

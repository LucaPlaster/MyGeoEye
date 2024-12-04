import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicInteger;

// Serviço de monitoramento para detectar falhas e instanciar novos DataNodes
public class MonitorService extends UnicastRemoteObject implements MonitorServiceInterface {
    private MasterServerInterface masterServer;
    private AtomicInteger dataNodeCounter = new AtomicInteger(0);

    protected MonitorService() throws RemoteException {
        super();
    }

    @Override
    public void notifyFailure(String dataNodeId) throws RemoteException {
        System.out.println("MonitorService: Falha detectada no DataNode " + dataNodeId);

        // Remove o DataNode falho do MasterServer
        masterServer.unregisterDataNode(dataNodeId);

        // Instancia um novo DataNode
        String newDataNodeId = "DataNode_" + dataNodeCounter.incrementAndGet();
        DataNode newDataNode = new DataNode(newDataNodeId);
        newDataNode.start();

        // Registra o novo DataNode no MasterServer
        masterServer.registerDataNode(newDataNodeId, newDataNode);
        System.out.println("MonitorService: Novo DataNode " + newDataNodeId + " instanciado e registrado.");
    }

    @Override
    public void registerMasterServer(MasterServerInterface masterServer) throws RemoteException {
        this.masterServer = masterServer;
        System.out.println("MonitorService: MasterServer registrado para monitoramento.");
    }

    public static void main(String[] args) {
        try {
            MonitorService monitorService = new MonitorService();
            Registry registry = LocateRegistry.createRegistry(2000); // Porta específica para o MonitorService
            registry.rebind("MonitorService", monitorService);
            System.out.println("MonitorService iniciado e registrado no RMI Registry.");
        } catch (Exception e) {
            System.err.println("Erro no MonitorService: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicInteger;

// Serviço de monitoramento para detectar falhas e instanciar novos DataNodes
public class MonitorService extends UnicastRemoteObject implements MonitorServiceInterface {
    private MasterServerInterface masterServer; // Referência ao MasterServer para notificações e atualizações
    private AtomicInteger dataNodeCounter = new AtomicInteger(0); // Contador para gerar IDs únicos para novos DataNodes

    // Construtor padrão do MonitorService
    protected MonitorService() throws RemoteException {
        super(); // Chama o construtor da classe UnicastRemoteObject
    }

    // Método chamado para notificar sobre a falha de um DataNode
    @Override
    public void notifyFailure(String dataNodeId) throws RemoteException {
        System.out.println("MonitorService: Falha detectada no DataNode " + dataNodeId);

        // Remove o DataNode falho do registro do MasterServer
        masterServer.unregisterDataNode(dataNodeId);

        // Gera um novo identificador para o DataNode substituto
        String newDataNodeId = "DataNode_" + dataNodeCounter.incrementAndGet();

        // Cria e inicia um novo DataNode
        DataNode newDataNode = new DataNode(newDataNodeId);
        newDataNode.start();

        // Registra o novo DataNode no MasterServer
        masterServer.registerDataNode(newDataNodeId, newDataNode);
        System.out.println("MonitorService: Novo DataNode " + newDataNodeId + " instanciado e registrado.");
    }

    // Registra o MasterServer para ser monitorado
    @Override
    public void registerMasterServer(MasterServerInterface masterServer) throws RemoteException {
        this.masterServer = masterServer; // Armazena a referência ao MasterServer
        System.out.println("MonitorService: MasterServer registrado para monitoramento.");
    }

    // Método principal para iniciar o MonitorService
    public static void main(String[] args) {
        try {
            // Cria uma instância do MonitorService
            MonitorService monitorService = new MonitorService();

            // Cria um registro RMI na porta 2000 para o MonitorService
            Registry registry = LocateRegistry.createRegistry(2000);
            registry.rebind("MonitorService", monitorService); // Registra o MonitorService no RMI Registry

            System.out.println("MonitorService iniciado e registrado no RMI Registry.");
        } catch (Exception e) {
            // Captura erros ao iniciar o MonitorService
            System.err.println("Erro no MonitorService: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

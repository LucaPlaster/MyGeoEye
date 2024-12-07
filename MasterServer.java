import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

// Classe que implementa o servidor mestre responsável pela coordenação de DataNodes
public class MasterServer extends UnicastRemoteObject implements MasterServerInterface {

    // Mapa para registrar DataNodes disponíveis, associando IDs a instâncias remotas
    private Map<String, DataNodeInterface> dataNodes = Collections.synchronizedMap(new HashMap<>());

    // Mapa para rastrear quais partes de cada imagem estão em quais DataNodes
    private Map<String, Map<Integer, String>> imageParts = Collections.synchronizedMap(new HashMap<>());

    // Fator de replicação das partes das imagens (não implementado neste código)
    private int replicationFactor;

    // Referência ao serviço de monitoramento (MonitorService)
    private MonitorServiceInterface monitorService;

    // Construtor do MasterServer, inicializa o fator de replicação e registra no MonitorService
    protected MasterServer(int replicationFactor) throws RemoteException {
        this.replicationFactor = replicationFactor;

        try {
            // Conecta ao MonitorService para registro do MasterServer
            Registry monitorRegistry = LocateRegistry.getRegistry("localhost", 2000);
            monitorService = (MonitorServiceInterface) monitorRegistry.lookup("MonitorService");
            monitorService.registerMasterServer(this);
            System.out.println("MasterServer registrado no MonitorService.");
        } catch (Exception e) {
            System.err.println("Erro ao conectar com o MonitorService: " + e.getMessage());
        }
    }

    // Registra um DataNode no sistema
    @Override
    public void registerDataNode(String dataNodeId, DataNodeInterface dataNode) throws RemoteException {
        dataNodes.put(dataNodeId, dataNode);
        System.out.println("DataNode " + dataNodeId + " registrado.");
    }

    // Remove o registro de um DataNode do sistema
    @Override
    public void unregisterDataNode(String dataNodeId) throws RemoteException {
        dataNodes.remove(dataNodeId);
        System.out.println("DataNode " + dataNodeId + " removido do registro.");
    }

    // Lista os nomes das imagens disponíveis no sistema
    @Override
    public List<String> listImages() throws RemoteException {
        return new ArrayList<>(imageParts.keySet());
    }

    // Recupera informações sobre as partes de uma imagem, retornando os DataNodes que possuem essas partes
    @Override
    public Map<Integer, DataNodeInterface> getImageParts(String imageName) throws RemoteException {
        Map<Integer, DataNodeInterface> partsMap = new HashMap<>();
        Map<Integer, String> parts = imageParts.get(imageName);

        if (parts != null) {
            for (Map.Entry<Integer, String> entry : parts.entrySet()) {
                String dataNodeId = entry.getValue();
                DataNodeInterface dataNode = dataNodes.get(dataNodeId);

                try {
                    // Verifica se o DataNode está acessível
                    dataNode.ping();
                    partsMap.put(entry.getKey(), dataNode);
                } catch (RemoteException e) {
                    // Notifica o serviço de monitoramento caso o DataNode esteja inacessível
                    System.err.println("DataNode " + dataNodeId + " inacessível. Notificando o MonitorService.");
                    notifyMonitorService(dataNodeId);
                    return null; // Retorna null caso a operação não possa ser concluída
                }
            }
            return partsMap;
        } else {
            return null; // Retorna null se a imagem não for encontrada
        }
    }

    // Armazena uma imagem dividindo-a em partes e distribuindo-as pelos DataNodes
    @Override
    public boolean storeImage(String imageName, byte[] imageData, int numParts) throws RemoteException {
        try {
            int partSize = imageData.length / numParts; // Tamanho de cada parte da imagem
        

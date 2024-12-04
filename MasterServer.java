import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

// Classe que implementa o servidor mestre
public class MasterServer extends UnicastRemoteObject implements MasterServerInterface {
    private Map<String, DataNodeInterface> dataNodes = Collections.synchronizedMap(new HashMap<>());
    private Map<String, Map<Integer, String>> imageParts = Collections.synchronizedMap(new HashMap<>());
    private int replicationFactor;
    private MonitorServiceInterface monitorService;

    protected MasterServer(int replicationFactor) throws RemoteException {
        this.replicationFactor = replicationFactor;

        // Conecta-se ao MonitorService
        try {
            Registry monitorRegistry = LocateRegistry.getRegistry("localhost", 2000);
            monitorService = (MonitorServiceInterface) monitorRegistry.lookup("MonitorService");
            monitorService.registerMasterServer(this);
            System.out.println("MasterServer registrado no MonitorService.");
        } catch (Exception e) {
            System.err.println("Erro ao conectar com o MonitorService: " + e.getMessage());
        }
    }

    @Override
    public void registerDataNode(String dataNodeId, DataNodeInterface dataNode) throws RemoteException {
        dataNodes.put(dataNodeId, dataNode);
        System.out.println("DataNode " + dataNodeId + " registrado.");
    }

    @Override
    public void unregisterDataNode(String dataNodeId) throws RemoteException {
        dataNodes.remove(dataNodeId);
        System.out.println("DataNode " + dataNodeId + " removido do registro.");
    }

    @Override
    public List<String> listImages() throws RemoteException {
        return new ArrayList<>(imageParts.keySet());
    }

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
                    System.err.println("DataNode " + dataNodeId + " inacessível. Notificando o MonitorService.");
                    notifyMonitorService(dataNodeId);
                    return null; // Retorna null para indicar que a operação não pôde ser concluída
                }
            }
            return partsMap;
        } else {
            return null;
        }
    }

    @Override
    public boolean storeImage(String imageName, byte[] imageData, int numParts) throws RemoteException {
        try {
            int partSize = imageData.length / numParts;
            Map<Integer, String> partsMap = new HashMap<>();

            List<String> dataNodeIds = new ArrayList<>(dataNodes.keySet());
            if (dataNodeIds.isEmpty()) {
                System.err.println("Nenhum DataNode disponível para armazenar a imagem.");
                return false;
            }

            Collections.shuffle(dataNodeIds);
            int dataNodeIndex = 0;

            for (int i = 0; i < numParts; i++) {
                int start = i * partSize;
                int end = (i == numParts - 1) ? imageData.length : start + partSize;
                byte[] partData = Arrays.copyOfRange(imageData, start, end);

                String dataNodeId = dataNodeIds.get(dataNodeIndex % dataNodeIds.size());
                DataNodeInterface dataNode = dataNodes.get(dataNodeId);

                try {
                    if (dataNode.uploadPart(imageName, i, partData)) {
                        partsMap.put(i, dataNodeId);
                    } else {
                        System.err.println("Falha ao armazenar a parte " + i + " da imagem '" + imageName + "'.");
                        return false;
                    }
                } catch (RemoteException e) {
                    System.err.println("DataNode " + dataNodeId + " inacessível durante o upload. Notificando o MonitorService.");
                    notifyMonitorService(dataNodeId);
                    return false;
                }

                dataNodeIndex++;
            }

            imageParts.put(imageName, partsMap);
            System.out.println("Imagem '" + imageName + "' armazenada com sucesso.");
            return true;
        } catch (Exception e) {
            System.err.println("Erro ao armazenar a imagem: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteImage(String imageName) throws RemoteException {
        Map<Integer, String> parts = imageParts.remove(imageName);
        if (parts != null) {
            for (Map.Entry<Integer, String> entry : parts.entrySet()) {
                String dataNodeId = entry.getValue();
                DataNodeInterface dataNode = dataNodes.get(dataNodeId);

                try {
                    dataNode.deletePart(imageName, entry.getKey());
                } catch (RemoteException e) {
                    System.err.println("DataNode " + dataNodeId + " inacessível durante a exclusão. Notificando o MonitorService.");
                    notifyMonitorService(dataNodeId);
                }
            }
            System.out.println("Imagem '" + imageName + "' deletada com sucesso.");
            return true;
        } else {
            System.out.println("Imagem '" + imageName + "' não encontrada.");
            return false;
        }
    }

    private void notifyMonitorService(String dataNodeId) {
        try {
            monitorService.notifyFailure(dataNodeId);
        } catch (Exception e) {
            System.err.println("Erro ao notificar o MonitorService: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            int replicationFactor = 1;
            if (args.length > 0) {
                replicationFactor = Integer.parseInt(args[0]);
            }

            MasterServer masterServer = new MasterServer(replicationFactor);
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("MasterServer", masterServer);
            System.out.println("MasterServer iniciado e registrado no RMI Registry.");
        } catch (Exception e) {
            System.err.println("Erro no MasterServer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

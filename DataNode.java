import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

// Classe que representa um nó de dados (DataNode)
public class DataNode extends UnicastRemoteObject implements DataNodeInterface {
    // Diretório base onde as partes das imagens serão armazenadas
    private static final String STORAGE_DIR = "data_node_storage/";
    private String dataNodeId; // Identificador único para este DataNode

    // Construtor que inicializa o DataNode com um identificador único
    protected DataNode(String dataNodeId) throws RemoteException {
        this.dataNodeId = dataNodeId;
        // Cria um diretório específico para armazenar os dados deste DataNode
        File dir = new File(STORAGE_DIR + dataNodeId + "/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Método para armazenar uma parte de uma imagem no DataNode
    @Override
    public boolean uploadPart(String imageName, int partNumber, byte[] data) throws RemoteException {
        try {
            // Cria um arquivo para armazenar a parte da imagem
            FileOutputStream fos = new FileOutputStream(STORAGE_DIR + dataNodeId + "/" + imageName + "_part" + partNumber);
            fos.write(data);
            fos.close();
            System.out.println("DataNode " + dataNodeId + ": Parte " + partNumber + " da imagem '" + imageName + "' armazenada.");
            return true; // Retorna true se o upload for bem-sucedido
        } catch (IOException e) {
            // Captura erros ao armazenar a parte da imagem
            System.err.println("Erro ao armazenar a parte da imagem: " + e.getMessage());
            return false; // Retorna false em caso de erro
        }
    }

    // Método para baixar uma parte de uma imagem armazenada no DataNode
    @Override
    public byte[] downloadPart(String imageName, int partNumber) throws RemoteException {
        try {
            // Localiza o arquivo que contém a parte solicitada
            File file = new File(STORAGE_DIR + dataNodeId + "/" + imageName + "_part" + partNumber);
            if (file.exists()) {
                // Lê os dados do arquivo e os retorna como um array de bytes
                byte[] data = Files.readAllBytes(file.toPath());
                System.out.println("DataNode " + dataNodeId + ": Parte " + partNumber + " da imagem '" + imageName + "' enviada.");
                return data; // Retorna os dados lidos
            } else {
                System.out.println("DataNode " + dataNodeId + ": Parte " + partNumber + " da imagem '" + imageName + "' não encontrada.");
                return null; // Retorna null se o arquivo não existir
            }
        } catch (IOException e) {
            // Captura erros ao ler a parte da imagem
            System.err.println("Erro ao ler a parte da imagem: " + e.getMessage());
            return null; // Retorna null em caso de erro
        }
    }

    // Método para deletar uma parte de uma imagem do DataNode
    @Override
    public boolean deletePart(String imageName, int partNumber) throws RemoteException {
        // Localiza o arquivo que contém a parte a ser deletada
        File file = new File(STORAGE_DIR + dataNodeId + "/" + imageName + "_part" + partNumber);
        if (file.exists() && file.delete()) {
            // Retorna true se o arquivo for encontrado e deletado com sucesso
            System.out.println("DataNode " + dataNodeId + ": Parte " + partNumber + " da imagem '" + imageName + "' deletada.");
            return true;
        } else {
            // Retorna false se o arquivo não existir ou não puder ser deletado
            System.out.println("DataNode " + dataNodeId + ": Falha ao deletar a parte " + partNumber + " da imagem '" + imageName + "'.");
            return false;
        }
    }

    // Método para verificar a disponibilidade do DataNode (ping)
    @Override
    public boolean ping() throws RemoteException {
        return true; // Retorna sempre true, indicando que o DataNode está ativo
    }

    // Método para iniciar o DataNode e registrá-lo no RMI Registry
    public void start() {
        try {
            // Obtém o registro RMI
            Registry registry = LocateRegistry.getRegistry();

            // Registra o DataNode no RMI Registry
            registry.rebind("DataNode_" + dataNodeId, this);
            System.out.println("DataNode " + dataNodeId + " registrado no RMI Registry.");

            // Registra o DataNode no MasterServer
            MasterServerInterface master = (MasterServerInterface) registry.lookup("MasterServer");
            master.registerDataNode(dataNodeId, this);
            System.out.println("DataNode " + dataNodeId + " registrado no MasterServer.");
        } catch (Exception e) {
            // Captura erros no registro do DataNode
            System.err.println("Erro ao iniciar o DataNode: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para simular uma falha no DataNode
    public void fail() {
        System.out.println("DataNode " + dataNodeId + " falhando...");
        System.exit(1); // Encerra o processo do DataNode
    }

    // Método principal para inicializar o DataNode
    public static void main(String[] args) {
        try {
            // Obtém o identificador do DataNode a partir dos argumentos
            String dataNodeId = args[0];
            // Cria uma instância do DataNode
            DataNode dataNode = new DataNode(dataNodeId);
            // Inicia o DataNode
            dataNode.start();
        } catch (Exception e) {
            // Captura erros na inicialização do DataNode
            System.err.println("Erro no DataNode: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

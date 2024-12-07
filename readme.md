# MonitorService - Sistema de Monitoramento para DataNodes

## Introdução
O **MonitorService** é um componente de um sistema distribuído projetado para monitorar e gerenciar **DataNodes**. Ele desempenha um papel fundamental ao garantir a resiliência do sistema, detectando falhas e substituindo automaticamente nós defeituosos.

## Funcionalidades

### 1. Detecção de falhas
- Monitora os **DataNodes** registrados no sistema.
- Detecta falhas notificadas pelo **MasterServer**.

### 2. Substituição de DataNodes falhos
- Remove o **DataNode** falho do registro do **MasterServer**.
- Instancia automaticamente um novo **DataNode** com um identificador único.
- Registra o novo **DataNode** no **MasterServer**.

### 3. Registro do MasterServer
- O **MonitorService** registra o **MasterServer** como parte do sistema, permitindo que ele seja monitorado e mantenha a integridade do sistema.

## Fluxo de Operações

1. **Falha detectada:**
   - O **MasterServer** notifica o **MonitorService** sobre um **DataNode** falho por meio do método `notifyFailure`.

2. **Remoção do DataNode falho:**
   - O **MonitorService** chama o método `unregisterDataNode` no **MasterServer** para remover o nó defeituoso.

3. **Instanciação de um novo DataNode:**
   - Um novo **DataNode** é criado com um identificador único, utilizando o contador `dataNodeCounter`.

4. **Registro do novo DataNode:**
   - O novo nó é iniciado e registrado no **MasterServer**, garantindo a continuidade do sistema.

## Estrutura do Código

### Classe `MonitorService`
A classe implementa a interface `MonitorServiceInterface` e herda de `UnicastRemoteObject` para que possa ser acessada remotamente.

### Métodos Principais

#### `notifyFailure(String dataNodeId)`
- **Função:** Detecta a falha de um **DataNode**.
- **Passos:**
  1. Remove o nó falho do **MasterServer**.
  2. Instancia um novo nó substituto.
  3. Registra o novo nó no **MasterServer**.

#### `registerMasterServer(MasterServerInterface masterServer)`
- **Função:** Registra o **MasterServer** no **MonitorService**.
- **Passos:**
  1. Armazena a referência ao **MasterServer**.
  2. Exibe uma mensagem de confirmação.

#### `main(String[] args)`
- **Função:** Inicia o **MonitorService**.
- **Passos:**
  1. Cria uma instância do **MonitorService**.
  2. Registra o serviço no RMI Registry na porta 2000.
  3. Exibe mensagens indicando que o serviço está pronto.

## Exemplo de Execução

1. O **MasterServer** detecta que um **DataNode** não está acessível.
2. Chama o método `notifyFailure` do **MonitorService**, passando o ID do nó defeituoso.
3. O **MonitorService**:
   - Remove o nó do registro do **MasterServer**.
   - Cria e inicia um novo **DataNode**.
   - Registra o novo nó no **MasterServer**.

## Código

```java
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicInteger;

public class MonitorService extends UnicastRemoteObject implements MonitorServiceInterface {
    private MasterServerInterface masterServer;
    private AtomicInteger dataNodeCounter = new AtomicInteger(0);

    protected MonitorService() throws RemoteException {
        super();
    }

    @Override
    public void notifyFailure(String dataNodeId) throws RemoteException {
        System.out.println("MonitorService: Falha detectada no DataNode " + dataNodeId);
        masterServer.unregisterDataNode(dataNodeId);
        String newDataNodeId = "DataNode_" + dataNodeCounter.incrementAndGet();
        DataNode newDataNode = new DataNode(newDataNodeId);
        newDataNode.start();
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
            Registry registry = LocateRegistry.createRegistry(2000);
            registry.rebind("MonitorService", monitorService);
            System.out.println("MonitorService iniciado e registrado no RMI Registry.");
        } catch (Exception e) {
            System.err.println("Erro no MonitorService: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

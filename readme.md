# MonitorService - Sistema de Monitoramento com Java RMI

## Descrição

O **MonitorService** é um componente essencial de um sistema distribuído, implementado usando **Java RMI (Remote Method Invocation)**. Ele é responsável por monitorar, gerenciar falhas e substituir automaticamente **DataNodes** defeituosos para garantir a continuidade do serviço.

Este serviço trabalha em conjunto com outros componentes do sistema distribuído, como o **MasterServer** e os **DataNodes**, utilizando **Java RMI** para facilitar a comunicação remota entre eles.

---

## Objetivo

O objetivo deste projeto é implementar um sistema distribuído resiliente, que:
- **Detecte falhas automaticamente:** Identifique DataNodes inacessíveis.
- **Recupere o sistema automaticamente:** Remova nós defeituosos e substitua por novos.
- **Distribua e gerencie dados:** Garanta alta disponibilidade por meio de um monitoramento ativo e de substituições dinâmicas.

---

## Estrutura do Sistema

### Componentes

1. **MonitorService**
   - Monitora os **DataNodes** registrados no sistema.
   - Detecta falhas notificadas pelo **MasterServer**.
   - Cria e registra novos **DataNodes** para substituir os defeituosos.

2. **MasterServer**
   - Coordena os **DataNodes** no sistema.
   - Distribui partes de dados para os nós disponíveis.
   - Notifica o **MonitorService** sobre falhas detectadas.

3. **DataNode**
   - Armazena partes de dados do sistema distribuído.
   - É monitorado pelo **MasterServer**.
   - Pode ser substituído automaticamente em caso de falhas.

4. **Java RMI**
   - Facilita a comunicação remota entre o **MonitorService**, **MasterServer** e os **DataNodes**.
   - Permite que métodos sejam invocados remotamente como se fossem locais.

---

## Funcionalidades do MonitorService

### 1. **Notificação de Falhas**
- **Método:** `notifyFailure(String dataNodeId)`
- **Descrição:** Detecta falhas em **DataNodes**.
- **O que faz:**
  - Remove o nó defeituoso do sistema por meio do **MasterServer**.
  - Cria um novo nó substituto.
  - Registra o novo nó no **MasterServer** para continuidade da operação.

### 2. **Registro do MasterServer**
- **Método:** `registerMasterServer(MasterServerInterface masterServer)`
- **Descrição:** Registra o **MasterServer** no **MonitorService**.
- **O que faz:**
  - Estabelece a comunicação entre o **MasterServer** e o **MonitorService**.
  - Permite que o **MasterServer** notifique falhas ao **MonitorService**.

### 3. **Instanciação de Novos DataNodes**
- **Descrição:** Garante que, após a falha de um nó, o sistema substitua automaticamente o nó defeituoso por um novo.
- **O que faz:**
  - Gera um identificador único para o novo **DataNode**.
  - Inicia o novo **DataNode**.
  - Registra o nó recém-criado no **MasterServer**.

---

## Fluxo de Operação do Sistema

1. O **MasterServer** detecta a falha de um **DataNode**.
2. O **MasterServer** notifica o **MonitorService**.
3. O **MonitorService**:
   - Remove o nó defeituoso do sistema.
   - Cria e inicia um novo **DataNode**.
   - Registra o novo nó no **MasterServer**.
4. O sistema continua funcionando normalmente, sem interrupções.

---

## Código Explicado

### Classe `MonitorService`

#### **1. Atributos**
- `masterServer`:
  - Referência ao **MasterServer**, usada para remover nós defeituosos e registrar novos nós.
- `dataNodeCounter`:
  - Um contador que gera identificadores únicos para novos **DataNodes**.

#### **2. Método `notifyFailure(String dataNodeId)`**
- **Entrada:**
  - `dataNodeId`: O identificador do **DataNode** que falhou.
- **O que faz:**
  - Remove o nó defeituoso do registro no **MasterServer**.
  - Cria um novo **DataNode** com um identificador único.
  - Inicia o novo **DataNode** e o registra no **MasterServer**.
- **Saída:** 
  - Substitui o nó falho por um novo.

#### **3. Método `registerMasterServer(MasterServerInterface masterServer)`**
- **Entrada:**
  - `masterServer`: A referência ao **MasterServer**.
- **O que faz:**
  - Registra o **MasterServer** no **MonitorService** para monitoramento e comunicação.
- **Saída:**
  - Estabelece a conexão entre o **MasterServer** e o **MonitorService**.

#### **4. Método `main(String[] args)`**
- **O que faz:**
  - Inicia o **MonitorService**.
  - Cria e registra o serviço no RMI Registry na porta 2000.
- **Saída:**
  - Serviço pronto para monitorar o sistema.

---

## Exemplo de Código do MonitorService

```java
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

# Sistema de Gerenciamento Distribuído de Imagens

Este projeto implementa um sistema de gerenciamento distribuído de imagens utilizando **Java RMI (Remote Method Invocation)**. O sistema é composto por um **servidor mestre (MasterServer)**, múltiplos **nós de dados (DataNodes)**, e um **serviço de monitoramento (MonitorService)**, além de um **cliente** para interação com o sistema.

---

## O que é RMI?

**Java RMI (Remote Method Invocation)** é uma tecnologia que permite a execução de métodos em objetos remotos como se fossem locais. Ele é usado para implementar sistemas distribuídos, onde as operações podem ocorrer em diferentes máquinas conectadas em uma rede.

**Características do RMI:**
1. Comunicação transparente entre cliente e servidor.
2. Uso de interfaces para definir métodos remotos.
3. Gerenciamento automático de serialização/deserialização de objetos.

---

## Estrutura do Projeto

### **1. MasterServer (Servidor Mestre)**

#### **Funções Principais**
- Gerenciar as imagens armazenadas no sistema.
- Distribuir as partes das imagens entre os **DataNodes**.
- Coordenar operações de upload, download e exclusão de imagens.
- Detectar falhas nos **DataNodes** através do **MonitorService**.

#### **Métodos**

##### **1. Método `registerDataNode(String dataNodeId, DataNodeInterface dataNode)`**
- **Entrada:**
  - `dataNodeId`: Identificador único do **DataNode**.
  - `dataNode`: Instância do **DataNodeInterface**.
- **O que faz:**
  - Registra um novo **DataNode** no sistema.
- **Saída:**
  - Atualiza o registro de **DataNodes** ativos.

##### **2. Método `unregisterDataNode(String dataNodeId)`**
- **Entrada:**
  - `dataNodeId`: Identificador do **DataNode** que será removido.
- **O que faz:**
  - Remove o registro do **DataNode** em caso de falha.
- **Saída:**
  - Atualiza o registro de **DataNodes** ativos.

##### **3. Método `listImages()`**
- **Entrada:**
  - Nenhuma.
- **O que faz:**
  - Retorna uma lista de imagens disponíveis no sistema.
- **Saída:**
  - Lista de nomes das imagens armazenadas.

##### **4. Método `storeImage(String imageName, byte[] imageData, int numParts)`**
- **Entrada:**
  - `imageName`: Nome da imagem.
  - `imageData`: Dados binários da imagem.
  - `numParts`: Número de partes em que a imagem será dividida.
- **O que faz:**
  - Divide a imagem em partes e distribui entre os **DataNodes**.
- **Saída:**
  - Armazena a imagem e atualiza o mapeamento entre as partes e os **DataNodes**.

##### **5. Método `deleteImage(String imageName)`**
- **Entrada:**
  - `imageName`: Nome da imagem.
- **O que faz:**
  - Remove as partes da imagem de todos os **DataNodes**.
- **Saída:**
  - Atualiza o registro de imagens e remove os dados do sistema.

---

### **2. DataNode (Nó de Dados)**

#### **Funções Principais**
- Armazenar partes de imagens.
- Fornecer partes das imagens sob demanda para download.
- Excluir partes de imagens quando solicitado.
- Reportar sua disponibilidade ao servidor mestre.

#### **Métodos**

##### **1. Método `uploadPart(String imageName, int partNumber, byte[] data)`**
- **Entrada:**
  - `imageName`: Nome da imagem.
  - `partNumber`: Número da parte da imagem.
  - `data`: Dados binários da parte.
- **O que faz:**
  - Armazena uma parte da imagem localmente.
- **Saída:**
  - Confirmação do armazenamento.

##### **2. Método `downloadPart(String imageName, int partNumber)`**
- **Entrada:**
  - `imageName`: Nome da imagem.
  - `partNumber`: Número da parte a ser recuperada.
- **O que faz:**
  - Lê e retorna os dados da parte da imagem armazenada.
- **Saída:**
  - Dados binários da parte solicitada.

##### **3. Método `deletePart(String imageName, int partNumber)`**
- **Entrada:**
  - `imageName`: Nome da imagem.
  - `partNumber`: Número da parte a ser excluída.
- **O que faz:**
  - Exclui a parte da imagem localmente.
- **Saída:**
  - Confirmação da exclusão.

##### **4. Método `ping()`**
- **Entrada:**
  - Nenhuma.
- **O que faz:**
  - Verifica se o **DataNode** está ativo.
- **Saída:**
  - Retorna `true` se o nó estiver ativo.

---

### **3. MonitorService (Serviço de Monitoramento)**

#### **Funções Principais**
- Monitorar falhas nos **DataNodes**.
- Notificar o **MasterServer** sobre falhas detectadas.
- Instanciar novos **DataNodes** em caso de falhas para manter a disponibilidade.

#### **Métodos**

##### **1. Método `notifyFailure(String dataNodeId)`**
- **Entrada:**
  - `dataNodeId`: Identificador do nó que falhou.
- **O que faz:**
  - Notifica o **MasterServer** sobre a falha.
  - Remove o nó falho do sistema.
  - Instancia um novo **DataNode** para substituí-lo.
- **Saída:**
  - Substitui o nó falho por um novo.

##### **2. Método `registerMasterServer(MasterServerInterface masterServer)`**
- **Entrada:**
  - `masterServer`: A referência ao **MasterServer**.
- **O que faz:**
  - Registra o **MasterServer** no **MonitorService** para monitoramento e comunicação.
- **Saída:**
  - Estabelece a conexão entre o **MasterServer** e o **MonitorService**.

##### **3. Método `main(String[] args)`**
- **O que faz:**
  - Inicia o **MonitorService**.
  - Cria e registra o serviço no RMI Registry na porta 2000.
- **Saída:**
  - Serviço pronto para monitorar o sistema.

---

### **4. Client (Cliente)**

#### **Funções Principais**
- Interagir com o **MasterServer** para realizar operações.
- Permitir que o usuário faça:
  - Upload de imagens.
  - Download de imagens.
  - Exclusão de imagens.
  - Listagem de imagens disponíveis.
  - Teste de desempenho.

#### **Fluxo Geral**
1. O cliente se conecta ao **MasterServer** via RMI.
2. Realiza as operações solicitadas pelo usuário.

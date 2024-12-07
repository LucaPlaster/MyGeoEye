import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

// Classe Cliente para interagir com o servidor MasterServer
public class Client {
    // Diretório onde as imagens baixadas serão salvas
    private static final String DOWNLOAD_DIR = "client_downloads/";

    public static void main(String[] args) {
        try {
            // Cria o diretório de downloads, se ainda não existir
            File dir = new File(DOWNLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Conecta ao registro RMI e obtém a referência do MasterServer
            Registry registry = LocateRegistry.getRegistry();
            MasterServerInterface master = (MasterServerInterface) registry.lookup("MasterServer");

            // Interface de entrada para o usuário
            Scanner scanner = new Scanner(System.in);
            String option;

            do {
                // Menu principal
                System.out.println("\nEscolha uma opção:");
                System.out.println("1. Upload de imagem");
                System.out.println("2. Listar imagens");
                System.out.println("3. Baixar imagem");
                System.out.println("4. Deletar imagem");
                System.out.println("5. Teste de desempenho");
                System.out.println("6. Sair");
                System.out.print("Opção: ");
                option = scanner.nextLine();

                // Opções do menu com chamadas para métodos específicos
                switch (option) {
                    case "1":
                        uploadImage(master, scanner);
                        break;
                    case "2":
                        listImages(master);
                        break;
                    case "3":
                        downloadImage(master, scanner);
                        break;
                    case "4":
                        deleteImage(master, scanner);
                        break;
                    case "5":
                        testPerformance(master, scanner);
                        break;
                    case "6":
                        System.out.println("Encerrando o cliente.");
                        break;
                    default:
                        System.out.println("Opção inválida.");
                }
            } while (!option.equals("6"));

        } catch (Exception e) {
            // Tratamento de erros gerais do cliente
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para enviar uma imagem ao servidor
    private static void uploadImage(MasterServerInterface master, Scanner scanner) {
        try {
            // Solicita o caminho do arquivo ao usuário
            System.out.print("Digite o caminho da imagem a ser enviada: ");
            String filePath = scanner.nextLine();
            File file = new File(filePath);

            if (!file.exists()) {
                System.out.println("Arquivo não encontrado.");
                return;
            }

            // Lê os dados da imagem do arquivo
            FileInputStream fis = new FileInputStream(file);
            byte[] imageData = new byte[(int) file.length()];
            fis.read(imageData);
            fis.close();

            // Solicita o número de partes para dividir a imagem
            System.out.print("Digite o número de partes para dividir a imagem: ");
            int numParts = Integer.parseInt(scanner.nextLine());

            // Envia a imagem ao servidor
            if (master.storeImage(file.getName(), imageData, numParts)) {
                System.out.println("Imagem enviada com sucesso.");
            } else {
                System.out.println("Falha ao enviar a imagem.");
            }
        } catch (Exception e) {
            // Tratamento de erros no envio de imagens
            System.err.println("Erro ao enviar a imagem: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para listar as imagens disponíveis no servidor
    private static void listImages(MasterServerInterface master) {
        try {
            List<String> images = master.listImages();
            System.out.println("Imagens disponíveis:");
            for (String image : images) {
                System.out.println("- " + image);
            }
        } catch (Exception e) {
            // Tratamento de erros na listagem
            System.err.println("Erro ao listar imagens: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para baixar uma imagem do servidor
    private static void downloadImage(MasterServerInterface master, Scanner scanner) {
        try {
            System.out.print("Digite o nome da imagem a ser baixada: ");
            String imageName = scanner.nextLine();

            // Obtém as partes da imagem armazenadas nos DataNodes
            Map<Integer, DataNodeInterface> partsMap = master.getImageParts(imageName);

            if (partsMap == null || partsMap.isEmpty()) {
                System.out.println("Imagem não encontrada ou indisponível.");
                return;
            }

            // Baixa todas as partes da imagem
            List<byte[]> imageParts = new ArrayList<>();
            for (int i = 0; i < partsMap.size(); i++) {
                DataNodeInterface dataNode = partsMap.get(i);
                byte[] partData = dataNode.downloadPart(imageName, i);
                if (partData != null) {
                    imageParts.add(partData);
                } else {
                    System.out.println("Falha ao baixar a parte " + i + " da imagem.");
                    return;
                }
            }

            // Reconstrói a imagem a partir das partes baixadas
            int totalSize = imageParts.stream().mapToInt(part -> part.length).sum();
            byte[] imageData = new byte[totalSize];
            int currentIndex = 0;
            for (byte[] part : imageParts) {
                System.arraycopy(part, 0, imageData, currentIndex, part.length);
                currentIndex += part.length;
            }

            // Salva a imagem reconstruída no diretório de downloads
            FileOutputStream fos = new FileOutputStream(DOWNLOAD_DIR + imageName);
            fos.write(imageData);
            fos.close();

            System.out.println("Imagem '" + imageName + "' baixada com sucesso.");
        } catch (Exception e) {
            // Tratamento de erros no download de imagens
            System.err.println("Erro ao baixar a imagem: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para deletar uma imagem no servidor
    private static void deleteImage(MasterServerInterface master, Scanner scanner) {
        try {
            System.out.print("Digite o nome da imagem a ser deletada: ");
            String imageName = scanner.nextLine();

            // Solicita ao servidor que delete a imagem
            if (master.deleteImage(imageName)) {
                System.out.println("Imagem deletada com sucesso.");
            } else {
                System.out.println("Imagem não encontrada ou falha ao deletar.");
            }
        } catch (Exception e) {
            // Tratamento de erros na exclusão de imagens
            System.err.println("Erro ao deletar a imagem: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para testar o desempenho do sistema
    private static void testPerformance(MasterServerInterface master, Scanner scanner) {
        try {
            System.out.print("Digite o número de imagens para o teste (10, 50, 200): ");
            int numImages = Integer.parseInt(scanner.nextLine());

            // Cria uma lista de nomes de imagens para o teste
            List<String> imageNames = new ArrayList<>();
            for (int i = 1; i <= numImages; i++) {
                imageNames.add("imagem_teste_" + i + ".jpg");
            }

            // Teste de inserção de imagens
            long startInsertion = System.currentTimeMillis();
            for (String imageName : imageNames) {
                byte[] imageData = new byte[1024 * 50]; // Imagem de 50KB
                new Random().nextBytes(imageData); // Preenche com dados aleatórios
                master.storeImage(imageName, imageData, 5);
            }
            long endInsertion = System.currentTimeMillis();
            System.out.println("Tempo de inserção de " + numImages + " imagens: " + (endInsertion - startInsertion) + " ms");

            // Teste de recuperação de imagens
            long startRetrieval = System.currentTimeMillis();
            for (String imageName : imageNames) {
                Map<Integer, DataNodeInterface> partsMap = master.getImageParts(imageName);
                if (partsMap != null) {
                    for (int i = 0; i < partsMap.size(); i++) {
                        DataNodeInterface dataNode = partsMap.get(i);
                        dataNode.downloadPart(imageName, i);
                    }
                }
            }
            long endRetrieval = System.currentTimeMillis();
            System.out.println("Tempo de recuperação de " + numImages + " imagens: " + (endRetrieval - startRetrieval) + " ms");

        } catch (Exception e) {
            // Tratamento de erros no teste de desempenho
            System.err.println("Erro no teste de desempenho: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

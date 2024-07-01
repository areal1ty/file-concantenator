import java.io.*;
import java.util.*;

class FileNode {
    File file;
    List<FileNode> dependencies;

    FileNode(File file) {
        this.file = file;
        this.dependencies = new ArrayList<>();
    }
}

public class FileConcantenator {
    private static final String START_FOLDER = "src";
        public static void main(String[] args) {
            File currentDir = new File(START_FOLDER);
            List<FileNode> readingOrder = new ArrayList<>();
            getContent(currentDir, readingOrder);

            List<FileNode> sortedFiles = sortTopologically(readingOrder);
            if (sortedFiles == null) {
                System.out.println("Error. Circular dependency happen");
                writeInErrorFile(readingOrder);
            } else {
                concatenate(sortedFiles);
            }
        }

        public static void getContent(File dir, List<FileNode> orderToRead) {
            File[] content = dir.listFiles();
            for (File file : Objects.requireNonNull(content, "file is empty")) {
                if (file.isDirectory()) {
                    getContent(file, orderToRead);
                } else {
                    if (file.getName().endsWith(".txt")) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                            FileNode currentNode = getFileNode(orderToRead, file);

                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.contains("require")) {
                                    String dependencyPath = line.split("'")[1];
                                    File dependencyFile = new File(file.getParentFile(), dependencyPath);
                                    FileNode dependencyNode = getFileNode(orderToRead, dependencyFile);
                                    currentNode.dependencies.add(dependencyNode);
                                }
                            }
                        } catch (IOException ex) {
                            System.out.println(ex.getMessage());
                        }
                    }
                }
            }
        }

        private static FileNode getFileNode(List<FileNode> orderToRead, File file) {
            for (FileNode node : orderToRead) {
                if (node.file.equals(file)) {
                    return node;
                }
            }

            FileNode newNode = new FileNode(file);
            orderToRead.add(newNode);
            return newNode;
        }

        public static List<FileNode> sortTopologically(List<FileNode> orderToRead) {
            List<FileNode> sortedFiles = new ArrayList<>();
            Set<FileNode> visitedNodes = new HashSet<>();
            Set<FileNode> nodesInProgress = new HashSet<>();

            for (FileNode node : orderToRead) {
                if (!visitedNodes.contains(node) && !hasCircularDependency(node, visitedNodes, nodesInProgress, sortedFiles)) {
                    return null;
                }
            }

            return sortedFiles;
        }

        private static Boolean hasCircularDependency(FileNode n, Set<FileNode> visitedNodes, Set<FileNode> nodesInProgress, List<FileNode> sortedFiles) {
            Object[] result = findCircularDependency(n);
            if ((Boolean) result[0]) {
                return false;
            }

            if (!visitedNodes.contains(n)) {
                nodesInProgress.add(n);
                visitedNodes.add(n);
                nodesInProgress.remove(n);

                if (n.dependencies.isEmpty()) {
                    sortedFiles.add(0, n);
                } else {
                    sortedFiles.add(n);
                }
            }
            return true;
        }

        private static Object[] findCircularDependency(FileNode node) {
            String fileName = node.file.getName().replace(".txt", "");

            for (int i = 0; i < node.dependencies.size(); i++) {
                String dependencyName = node.dependencies.get(i).file.getName();
                if (fileName.equals(dependencyName)) {
                    return new Object[]{true, node.file.getName(), dependencyName};
                }
            }

            return new Object[]{false};
        }

        private static void writeInErrorFile(List<FileNode> orderToRead) {
            for (FileNode node : orderToRead) {
                Object[] result = findCircularDependency(node);

                if ((Boolean) result[0]) {
                    try (PrintWriter writer = new PrintWriter("output_error.txt")) {
                        writer.println(result[1] + " has a circular dependency with " + result[2] + ".txt");
                    } catch (IOException e) {
                        System.out.println("Error occurred while writing error message. " + e.getMessage());
                    }
                }
            }
        }

        private static void concatenate(List<FileNode> sortedOrder) {
            try (PrintWriter writer = new PrintWriter("output_success.txt")) {
                for (FileNode n : sortedOrder) {
                    if (n.file.getName().endsWith(".txt")) {
                        String fileContent = readContent(n.file);
                        writer.println(fileContent);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error occurred while writing error message. " + e.getMessage());
            }
        }

        private static String readContent(File file) throws IOException {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            return content.toString();
        }
}

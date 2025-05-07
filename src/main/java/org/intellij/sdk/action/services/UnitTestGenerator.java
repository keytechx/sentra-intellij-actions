package org.intellij.sdk.action.services;

import com.intellij.openapi.progress.ProgressIndicator;
import lombok.Getter;
import lombok.Setter;
import org.intellij.sdk.action.dto.ApiResponse;
import org.intellij.sdk.action.dto.CancellationToken;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public class UnitTestGenerator {
    private String projectBaseDir = "";

    public void generateAndSaveUnitTestForAllCategories(
            String codeContent,
            String functionName,
            String fileName,
            AtomicBoolean cancelToken,
            ProgressIndicator progressIndicator) throws ExecutionException, InterruptedException {
        List<String> categories = List.of("Happy", "Negative", "Edge", "Throw Exception");
        String key = UUID.randomUUID().toString();
        StringBuilder generatedTests = new StringBuilder();

        for (String category : categories) {
            if (cancelToken.get()) {
                return;
            }
            String newTestNames = generateAndSaveUnitTest(
                    key,
                    codeContent,
                    functionName,
                    category,
                    fileName,
                    generatedTests.toString(),
                    progressIndicator);
            generatedTests.append(newTestNames);
        }
    }

    public String generateAndSaveUnitTest(
            String key,
            String codeContent,
            String functionName,
            String category,
            String fileName,
            String generatedTests,
            ProgressIndicator progressIndicator) throws ExecutionException, InterruptedException {
        String logMessage = "Generating unit tests for: " + functionName + "-" + category;
        System.out.println(logMessage);
        progressIndicator.setText(logMessage);
        // Call to genUnitTest (this should be implemented as per your requirements)
        ApiResponse apiResult = null;
        try {
            apiResult = ApiService.genUnitTest(key, functionName, codeContent, category, getAccessToken(), generatedTests);
        } catch (IOException ex) {
            return "";
        }

        String outputFolder = initializeOutputFolder();
        if (outputFolder == null) {
            return "";
        }

        Path fileBaseName = Paths.get(fileName).getFileName();
        String extension = CodeAnalyzerService.getFileExtension(fileName);
        String fileBaseNameWithoutExtension = CodeAnalyzerService.getFileNameWithoutExtension(fileBaseName.toString());
        Path fileFolder = Paths.get(outputFolder, fileBaseNameWithoutExtension);
        Path categoryFolder = fileFolder.resolve(category);

        try {
            Files.createDirectories(categoryFolder);

            // Write the result to a file
            Path filePath = categoryFolder.resolve(functionName + "." + extension);
            Files.writeString(filePath, apiResult.getUnitTest());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        return apiResult.getGeneratedTests();
    }

    public String initializeOutputFolder() {
        // Define the output folder path
        String outputFolder = projectBaseDir + "/sentra-unittests";

        // Check if the folder exists, create it if not
        File outputDir = new File(outputFolder);
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs(); // Create directories if they don't exist
            if (!created) {
                return "";
            }
        }
        return outputFolder;
    }

    public String getAccessToken() {
        return TokenService.getStoredAccessToken();
    }

    // Method to extract file extension (similar to `path.extname` in JavaScript)
    public String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex);
        }
        return "";  // Return empty if no extension is found
    }

    public void doGenUnitTest(
            String fileName,
            String fileType,
            String fileContent,
            String functionsCode,
            AtomicBoolean cancelToken,
            ProgressIndicator progressIndicator
    ) {
        System.out.println("File content: " + fileContent);

        try {
            List<String> imports = CodeAnalyzerService.extractDependencies(fileType, fileContent);
            String classNameFull = CodeAnalyzerService.extractClassNameCodeLine(fileType, fileContent);
            String className = CodeAnalyzerService.extractClassName(fileType, classNameFull);
            List<String> functions = CodeAnalyzerService.extractCodeFunctions(fileType, className, functionsCode);

            boolean isReact;
            if ("tsx".equals(fileType) || "ts".equals(fileType)) {
                isReact = "React".equals(CodeAnalyzerService.detectFramework(functionsCode));
            } else {
                isReact = false;
            }

            // Using CompletableFuture to simulate async functionality
            CompletableFuture.runAsync(() -> {
                try {
                    if (functions == null || functions.isEmpty()) {
                        // Cannot split functions, generate for the whole file
                        generateAndSaveUnitTestForAllCategories(fileContent, className, fileName, cancelToken, progressIndicator);
                    } else {
                        int totalMatches = functions.size();
                        List<String> functionNames = new java.util.ArrayList<>();

                        for (int i = 0; i < totalMatches; i++) {
                            String func = functions.get(i);
                            String codeContent = imports + "\n" + classNameFull + "\n    " + func + "\n}";

                            String functionName = CodeAnalyzerService.extractFunctionName(fileType, func, isReact);
                            if ("py".equals(fileType)) {
                                codeContent = imports + "\n" + func;
                            }

                            if (functionName == null || functionName.isEmpty()) {
                                continue;
                            }

                            functionName = CodeAnalyzerService.getUniqueFunctionName(functionNames, functionName);
                            functionNames.add(functionName);

                            generateAndSaveUnitTestForAllCategories(codeContent, functionName, fileName, cancelToken, progressIndicator);

                            // Simulate progress reporting
                            double progress = ((double) (i + 1) / totalMatches) * 100;
                            System.out.println(String.format("%d%% completed", (int) progress));
                        }
                    }

                    System.out.println("Generating unit tests finished!");

                } catch (Exception e) {
                    System.err.println("Error during unit test generation: " + e.getMessage());
                }
            }).join(); // Wait for async task to finish
        } catch (Exception e) {
            System.err.println("Error during unit test generation: " + e.getMessage());
        }
    }
}

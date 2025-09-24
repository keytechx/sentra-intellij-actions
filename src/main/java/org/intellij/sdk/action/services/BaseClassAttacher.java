package org.intellij.sdk.action.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import org.intellij.sdk.action.dto.ExtractBaseClassResponse;
import org.intellij.sdk.action.dto.MergeClassResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class BaseClassAttacher {
    private static final Logger LOG = Logger.getInstance(BaseClassAttacher.class);

    public CompletableFuture<String> attachBaseClass(
            String workspaceRoot,
            String fileContent,
            String fileType,
            AtomicBoolean cancelToken,
            @NotNull ProgressIndicator progressIndicator) throws IOException {

        if (cancelToken.get()) {
            return CompletableFuture.completedFuture(fileContent);
        }

        String className =CodeAnalyzerService.extractClassName(fileType, fileContent);

        ExtractBaseClassResponse baseClassResponse = ApiService.extractBaseClass(fileContent);

        String baseClassName = baseClassResponse.getBaseClass();
        System.out.println("Base class name: " + baseClassName);

        // No base class found
        if (baseClassName == null || "N/A".equals(baseClassName) || className.equals(baseClassName)) {
            return CompletableFuture.completedFuture(fileContent);
        }

        // Regex to find base class
        String regex = "class\\s+" + baseClassName + "\\b";

        // Base class in the same file
        if (fileContent.matches(regex)) {
            progressIndicator.setText("The current class has a base class. Processing...");

            if (cancelToken.get()) {
                return CompletableFuture.completedFuture(fileContent);
            }

            MergeClassResponse mergedClass = ApiService.mergeClass(fileContent);
            return CompletableFuture.completedFuture(mergedClass.getMergedClass());
        }

        // Find the base class file based on its name
        Optional<Path> baseClassFilePath = CodeAnalyzerService.findBaseClassFile(workspaceRoot, baseClassName, fileType);
        if (baseClassFilePath.isPresent()) {
            try {
                String baseFileContent = new String(Files.readAllBytes(baseClassFilePath.get()));
                return recursivelyAttachAndMergeBaseClass(workspaceRoot, baseFileContent, fileType, fileContent, progressIndicator, cancelToken);
            } catch (IOException e) {
                LOG.error("Failed to read base class file: " + baseClassFilePath.get(), e);
            }
        }

        // Find base class based on file content
        String baseFileContent = findBaseClassByContent(workspaceRoot, fileType, regex);
        return recursivelyAttachAndMergeBaseClass(workspaceRoot, baseFileContent, fileType, fileContent, progressIndicator, cancelToken);
    }

    public CompletableFuture<String> recursivelyAttachAndMergeBaseClass(
            String workspaceRoot,
            String baseFileContent,
            String fileType,
            final String fileContent,
            @NotNull ProgressIndicator progressIndicator,
            AtomicBoolean cancelToken) throws IOException {

        // If no base file content is provided, return the current file content
        if (baseFileContent == null || baseFileContent.isEmpty()) {
            return CompletableFuture.completedFuture(fileContent);
        }

        // Report progress
        progressIndicator.setText("The current class has a base class. Processing...");

        // Attach the base class to the file content
        return attachBaseClass(workspaceRoot, baseFileContent, fileType, cancelToken, progressIndicator)
                .thenCompose(attachedBaseClassContent -> {
                    // Concatenate the file content with the base class content
                    String fullFileContent = fileContent + "\n\n" + attachedBaseClassContent;

                    // Check if cancellation is requested
                    if (cancelToken.get()) {
                        return CompletableFuture.completedFuture(fullFileContent);
                    }

                    // Merge the class and return the result
                    MergeClassResponse mergedClass = null;
                    try {
                        mergedClass = ApiService.mergeClass(fileContent);
                        return CompletableFuture.completedFuture(mergedClass.getMergedClass());
                    } catch (IOException e) {
                        return CompletableFuture.completedFuture(fullFileContent);
                    }
                });
    }

    public static String findBaseClassByContent(String workspaceRoot, String fileType, String classRegex) throws IOException {
        Path startPath = Paths.get(workspaceRoot);
        Pattern pattern = Pattern.compile(classRegex);  // Compile the classRegex as a Pattern

        // Walk through the directory structure recursively
        try (Stream<Path> paths = Files.walk(startPath)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                if (Files.isRegularFile(path) && path.toString().endsWith("." + fileType)) {
                    String content = new String(Files.readAllBytes(path));  // Read the file content

                    if (pattern.matcher(content).find()) {
                        return content;  // Return the content of the first match
                    }
                }
            }
        }
        return null;  // Return null if no match is found
    }
}


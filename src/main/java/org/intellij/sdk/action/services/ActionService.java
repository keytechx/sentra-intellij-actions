package org.intellij.sdk.action.services;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.roots.ProjectRootManager;

import java.util.Objects;

@Service
public class ActionService {
    public static String getSelectedFunction(AnActionEvent event) {
        // If an element is selected in the editor, add info about it.
        Editor editor = event.getData(CommonDataKeys.EDITOR); // Get the editor
        if (editor != null) {
            String selectedText = editor.getSelectionModel().getSelectedText(); // Get the selected text
            return selectedText != null && !selectedText.isEmpty() ? selectedText : "";
        }
        return "";
    }

    public static String getFullCodeFile(AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return "";
        }
        Document document = editor.getDocument(); // Get the document (file content)
        return document.getText(); // This contains the full code of the file
    }

    public static String getFilePath(AnActionEvent event) {
        // Get the file associated with the current editor
        VirtualFile virtualFile = getVirtualFile(event);
        if (virtualFile == null) {
            Messages.showErrorDialog("No virtual file found.", "Error");
            return "";
        }
        return virtualFile.getPath();
    }

    public static String getFileName(AnActionEvent event) {
        // Get the file associated with the current editor
        VirtualFile virtualFile = getVirtualFile(event);
        if (virtualFile == null) {
            Messages.showErrorDialog("No virtual file found.", "Error");
            return "";
        }
        return virtualFile.getName();
    }

    public static VirtualFile getVirtualFile(AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return null;
        }

        // Get the file associated with the current editor
        return event.getData(CommonDataKeys.VIRTUAL_FILE);
    }

    public static String getProjectBaseDir(AnActionEvent event, String fileType) {
        // Get the current project
        Project project = event.getProject();
        if (project == null) {
            return "";
        }

        // Get the module from the project
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        Module[] modules = moduleManager.getModules();
        if (modules.length == 0) {
            return "";
        }
        // Get the module root manager to access the content roots
        Module module = modules[0];
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        ContentEntry[] contentEntries = moduleRootManager.getContentEntries();

        String testRootFolder = null;

        // Loop through the content entries and check for 'src/test/java'
        for (ContentEntry contentEntry : contentEntries) {
            for (SourceFolder sourceFolder : contentEntry.getSourceFolders()) {
                if ("java".equals(fileType) && sourceFolder.getUrl().contains("src/test/java")) {
                    testRootFolder = Objects.requireNonNull(sourceFolder.getFile()).getPath();
                    break;
                }
            }
            if (testRootFolder != null) {
                break;
            }
        }

        return Objects.requireNonNullElse(testRootFolder, project.getBasePath());
    }

    public static String getWorkspaceRoot(AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return "";
        }

        VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentRoots();
        if (contentRoots.length > 0) {
            return contentRoots[0].getPath(); // Use the first content root
        }

        return "";
    }
}

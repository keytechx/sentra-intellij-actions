// Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.intellij.sdk.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.intellij.sdk.action.services.ActionService;
import org.intellij.sdk.action.services.CodeAnalyzerService;
import org.intellij.sdk.action.services.TokenService;
import org.intellij.sdk.action.services.UnitTestGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Action class to demonstrate how to interact with the IntelliJ Platform.
 * The only action this class performs is to provide the user with a popup dialog as feedback.
 * Typically, this class is instantiated by the IntelliJ Platform framework based on declarations
 * in the plugin.xml file.
 * But when added at runtime, this class is instantiated by an action group.
 */
public class GenerateUtForSelectedFunctions extends AnAction {
  private final AtomicBoolean cancelToken = new AtomicBoolean(false);

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  /**
   * This default constructor is used by the IntelliJ Platform framework to instantiate this class based on plugin.xml
   * declarations. Only needed in {@link GenerateUtForSelectedFunctions} class because a second constructor is overridden.
   */
  public GenerateUtForSelectedFunctions() {
    super();
  }

  /**
   * This constructor is used to support dynamically added menu actions.
   * It sets the text, description to be displayed for the menu item.
   * Otherwise, the default AnAction constructor is used by the IntelliJ Platform.
   *
   * @param text        The text to be displayed as a menu item.
   * @param description The description of the menu item.
   * @param icon        The icon to be used with the menu item.
   */
  @SuppressWarnings("ActionPresentationInstantiatedInCtor") // via DynamicActionGroup
  public GenerateUtForSelectedFunctions(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
    super(text, description, icon);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    String title = event.getPresentation().getDescription();

    if (!TokenService.generateAccessToken(title)) {
      return;
    }

    String selectedFunction = ActionService.getSelectedFunction(event);
    String fileContent = ActionService.getFullCodeFile(event);
    if (selectedFunction.isEmpty() || fileContent.isEmpty()) {
      Messages.showMessageDialog(
              "No active text editor or no selection.",
              title,
              Messages.getInformationIcon());
      return;
    }
    String fullFileName = ActionService.getFileName(event);
    String fileType = CodeAnalyzerService.getFileExtension(fullFileName);
    if (fileType.isEmpty()) {
      Messages.showMessageDialog(
              "File type was not found.",
              title,
              Messages.getInformationIcon());
      return;
    }

    String filePath = ActionService.getFilePath(event);
    String projectBaseDir = ActionService.getProjectBaseDir(event, fileType);

    ProgressManager.getInstance().run(new Task.Backgroundable(event.getProject(), "Generating unit tests") {

      @Override
      public void run(@NotNull ProgressIndicator progressIndicator) {
        try {
          UnitTestGenerator generator = new UnitTestGenerator();
          generator.setProjectBaseDir(projectBaseDir);
          generator.doGenUnitTest(filePath, fileType, fileContent, selectedFunction, cancelToken, progressIndicator);
        } catch (Exception ex) {
          Messages.showErrorDialog("Error during unit test generation: " + ex.getMessage(), "Error");
        }
      }

      @Override
      public void onCancel() {
        // Handle the cancellation action
        cancelToken.set(true);
        Messages.showInfoMessage("Task was canceled.", "Cancelled");
      }
    });
  }

  @Override
  public void update(AnActionEvent e) {
    // Set the availability based on whether a project is open
    Project project = e.getProject();
    e.getPresentation().setEnabledAndVisible(project != null);
  }
}

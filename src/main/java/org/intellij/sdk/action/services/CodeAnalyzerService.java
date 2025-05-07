package org.intellij.sdk.action.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeAnalyzerService {
    public enum Framework {
        Angular, React, Unknown
    }

    public static Framework detectFramework(String codeBlock) {
        if (codeBlock.matches("(?s).*import\\s+\\{[^}]*}.*@angular/.*")) return Framework.Angular;
        if (codeBlock.matches("(?s).*@(Component|NgModule|Injectable|Directive|Pipe)\\(.*")) return Framework.Angular;
        if (codeBlock.matches("(?s).*\\bng(Model|If|For)\\b.*")) return Framework.Angular;
        if (codeBlock.matches("(?s).*(import\\s+React|from\\s+\"react\"|from\\s+'react').*")) return Framework.React;
        if (codeBlock.matches("(?s).*use(State|Effect|Context|Reducer|Memo).*")) return Framework.React;
        if (codeBlock.matches("(?s).*<\\s*[A-Z][A-Za-z]*[^>]*>.*import\\s+React.*")) return Framework.React;
        return Framework.Unknown;
    }

    public static List<String> extractDependencies(String fileType, String content) {
        String regex = "^\\s*import\\s+[\\w.]+;\\s*$";
        switch (fileType) {
            case "cs": regex = "^\\s*using\\s+[\\w.]+;\\s*$"; break;
            case "py": regex = "^\\s*(import\\s+[\\w.]+|from\\s+[\\w.]+\\s+import\\s+[\\w\\*,\\s]+)"; break;
        }

        Matcher matcher = Pattern.compile(regex, Pattern.MULTILINE).matcher(content);
        List<String> results = new ArrayList<>();
        while (matcher.find()) {
            results.add(matcher.group());
        }
        return results;
    }

    public static String extractClassNameCodeLine(String fileType, String content) {
        String regex = "(?:(public|protected|private)?\\s*(abstract|final)?\\s*class\\s+\\w+(?:\\s+extends\\s+[^{\\n]+)?(?:\\s+implements\\s+[^{\\n]+)?\\s*\\{)";
        switch (fileType) {
            case "cs": regex = "(?:(public|protected|private)?\\s*(abstract|sealed|static)?\\s*class\\s+\\w+(?:\\s*:\\s*[^{\\n]+)?\\s*\\{)"; break;
            case "ts":
            case "tsx": regex = "(?:(export\\s+)?(abstract|class)\\s+\\w+(?:\\s+extends\\s+[^{\\n]+)?(?:\\s+implements\\s+[^{\\n]+)?\\s*\\{)"; break;
            case "py": regex = "class\\s+([a-zA-Z_][a-zA-Z0-9_]*)"; break;
        }

        Matcher matcher = Pattern.compile(regex, Pattern.DOTALL).matcher(content);
        return matcher.find() ? matcher.group() : "";
    }

    public static String extractClassName(String fileType, String classNameLine) {
        if (fileType.equals("ts") || fileType.equals("tsx")) {
            String[] parts = classNameLine.split("\\s+");
            return parts[0].equals("export") ? parts[2] : parts[1];
        }

        Matcher matcher = Pattern.compile("class\\s+(\\w+)").matcher(classNameLine);
        return matcher.find() ? matcher.group(1) : "";
    }

    public static List<String> extractCodeFunctions(String fileType, String className, String content) {
        String regex = "";

        if ("java".equals(fileType)) {
            regex = "(?:(public|protected|private|static|final|synchronized|abstract|native|transient|volatile)\\s+)+"
                    + "(?:<[^<>]+>\\s+)?[\\w<>\\[\\],?]+\\s+(?!"+className+"\\b)[A-Za-z_]\\w*\\s*\\([^)]*\\)"
                    + "(?:\\s*throws\\s+[\\w.,\\s]+)?\\s*\\{(?:[^{}]|\\{(?:[^{}]|\\{[^{}]*\\})*\\})*\\}";
        } else if ("cs".equals(fileType)) {
            regex = "(?:(public|protected|private|static|sealed|abstract|override|virtual|internal|extern|partial|async|unsafe)\\s+)*"
                    + "[\\w<>\\[\\],?]+(?:<[^<>]+>)?\\s+(?!"+className+"\\b)[A-Za-z_]\\w*\\s*\\([^)]*\\)\\s*"
                    + "(?:=>\\s*[^;]+;|\\{(?:[^{}]|\\{(?:[^{}]|\\{[^{}]*\\})*\\})*\\})";
        } else if ("py".equals(fileType)) {
            regex = "(?<=\\n|^)\\s*def\\s+(?!__init__\\b)([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\([^)]*\\)\\s*:(.*?)\\n(?=\\s*(?:#|@|def\\s+|\\s*$))";
        }

        Matcher matcher = Pattern.compile(regex, Pattern.DOTALL).matcher(content);
        List<String> results = new ArrayList<>();
        while (matcher.find()) {
            results.add(matcher.group());
        }
        return results;
    }

    public static String extractFunctionName(String fileType, String lineContent, boolean isReact) {
        if (fileType.equals("java") || fileType.equals("cs")) {
            Matcher matcher = Pattern.compile("[\\w<>,?()\\s]+\\s+(\\w+)\\s*\\(").matcher(lineContent);
            return matcher.find() ? matcher.group(1) : "";
        }

        if ("ts".equals(fileType) || "tsx".equals(fileType)) {
            return isReact ? lineContent.split("=")[0].trim() : lineContent.split("\\(")[0].trim();
        }

        if ("py".equals(fileType)) {
            String[] parts = lineContent.split(" ");
            return parts[0].contains("def") && parts.length > 1 ? parts[1].split("\\(")[0] : "";
        }

        return "";
    }

    public static String getUniqueFunctionName(List<String> functionNames, String newFunction) {
        Map<String, Integer> nameCount = new HashMap<>();

        for (String name : functionNames) {
            String baseName = name.split("_Overload")[0];
            nameCount.put(baseName, nameCount.getOrDefault(baseName, 0) + 1);
        }

        if (nameCount.containsKey(newFunction)) {
            return newFunction + "_Overload" + (nameCount.get(newFunction) + 1);
        }

        return newFunction;
    }

    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex + 1);
        }
        return "";  // Return empty if no extension is found
    }

    public static String getFileNameWithoutExtension(String fileName) {
        String result = fileName;
        // Remove the file extension
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            result = fileName.substring(0, dotIndex);
        }
        return result;
    }
}

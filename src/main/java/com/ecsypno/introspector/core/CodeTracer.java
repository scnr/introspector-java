package com.ecsypno.introspector.core;

import java.nio.file.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CodeTracer {

    public static class Trace {
        private static final Map<String, List<String>> sourceCache = new ConcurrentHashMap<>();

        private final String className;
        private final String methodName;
        private final String filePath;
        private final int lineNumber;
        private final String sourceCode;
        private final String fileContents;
        private final long timestamp;
        
        public Trace(String className, String methodName, String filePath, 
                    int lineNumber) {
            this.className = className;
            this.methodName = methodName;
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.fileContents   = getFileContents(filePath);
            this.sourceCode     = getSourceCode(filePath, lineNumber);
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public String getFilePath() { return filePath; }
        public int getLineNumber() { return lineNumber; }
        public String getSourceCode() { return sourceCode; }
        public String getFileContents() { return fileContents; }
        public long getTimestamp() { return timestamp; }

        private String getSourceCode(String filePath, int lineNumber) {
            try {
                List<String> lines = sourceCache.computeIfAbsent(filePath, path -> {
                    try {
                        return Files.readAllLines(Paths.get(path));
                    } catch (IOException e) {
                        return List.of();
                    }
                });

                if (lineNumber > 0 && lineNumber <= lines.size()) {
                    return lines.get(lineNumber - 1);
                }
                
                return String.join("\n", lines);
            } catch (Exception e) {
                return "Unable to read source: " + e.getMessage();
            }
        }

        private String getFileContents(String filePath) {
            try {
                List<String> lines = sourceCache.computeIfAbsent(filePath, path -> {
                    try {
                        Path sourcePath = Paths.get(path);
                        if (Files.exists(sourcePath)) {
                            return Files.readAllLines(sourcePath);
                        }
                    } catch (IOException e) {
                        System.err.println("[INTROSPECTOR] Error reading file: " + path + " - " + e.getMessage());
                    }
                    return List.of();
                });
        
                if (lines.isEmpty()) {
                    return "";
                }
        
                return String.join("\n", lines);
        
            } catch (Exception e) {
                System.err.println("[INTROSPECTOR] Error processing source: " + e.getMessage());
                return "Error reading source";
            }
        }

    }

    private static ThreadLocal<List<Trace>> threadLocalTraces = ThreadLocal.withInitial(ArrayList::new);

    public static List<Trace> getTraces() {
        return new ArrayList<>(threadLocalTraces.get());
    }

    public static void clearTraces() {
        threadLocalTraces.get().clear();
    }

    public static void logTrace(Trace trace) {
        threadLocalTraces.get().add(trace);
    }

    public static void traceLine(String className, String methodName, String filePath, int lineNumber) {
        Trace trace = createTrace(className, methodName, filePath, lineNumber);
        logTrace(trace);
    }

    private static Trace createTrace(String className, String methodName, String filePath, int lineNumber) {
        // Implementation of creating a Trace object
        return new Trace(className, methodName, filePath, lineNumber);
    }
}
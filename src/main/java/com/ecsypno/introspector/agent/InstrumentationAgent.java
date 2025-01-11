package com.ecsypno.introspector.agent;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InstrumentationAgent {
    private static final Map<String, String> options = new HashMap<>();
    private static final String DEFAULT_SOURCE_DIR = "src/main/java/";

    private static final Set<String> VALID_OPTIONS = Set.of(
        "path_start_with",
        "path_ends_with", 
        "path_include_pattern",
        "path_exclude_pattern",
        "source_directory"
    );

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[INTROSPECTOR] Initializing Codename SCNR Introspector agent...");
        
        try {
            parseAgentArgs(agentArgs);
            validateRequiredOptions();
            ClassTransformer.setInstrumentation(inst);
            inst.addTransformer(new ClassTransformer(), true);
        } catch (IllegalArgumentException e) {
            System.err.println("[INTROSPECTOR] Agent initialization failed: " + e.getMessage());
            System.err.println("[INTROSPECTOR] Valid options are: " + String.join(", ", VALID_OPTIONS));
        }
    }

    private static void parseAgentArgs(String agentArgs) {
        // Set default source directory
        options.put("source_directory", DEFAULT_SOURCE_DIR);

        if (agentArgs == null || agentArgs.isEmpty()) {
            return;
        }

        String[] args = agentArgs.split(",");
        for (String arg : args) {
            String[] keyValue = arg.split("=");
            if (keyValue.length != 2) {
                throw new IllegalArgumentException("Invalid argument format: " + arg);
            }
            
            String key = keyValue[0].trim();
            if (!VALID_OPTIONS.contains(key)) {
                throw new IllegalArgumentException("Invalid option: " + key);
            }
            
            options.put(key, keyValue[1].trim());
        }
    }

    private static void validateRequiredOptions() {
        if (!options.containsKey("source_directory")) {
            throw new IllegalArgumentException("Required option missing: source_directory");
        }
    }

    public static String getOption(String key) {
        if (!VALID_OPTIONS.contains(key)) {
            throw new IllegalArgumentException("Invalid option requested: " + key);
        }
        return options.get(key);
    }
}

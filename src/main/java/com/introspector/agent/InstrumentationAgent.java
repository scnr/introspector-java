package com.introspector.agent;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

public class InstrumentationAgent {
    
    private static final Map<String, String> options = new HashMap<>();

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("Initializing Codename SCNR Introspector agent...");
        
        parseAgentArgs(agentArgs);
        
        if( getOption("source_directory") == null ) {
            options.put("source_directory", "src/main/java");
        }
        

        ClassTransformer.setInstrumentation(inst);
        inst.addTransformer(new ClassTransformer(), true);
    }
    
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("Attaching Codename SCNR Introspector agent...");
        parseAgentArgs(agentArgs);
        ClassTransformer.setInstrumentation(inst);
        inst.addTransformer(new ClassTransformer(), true);
    }

    private static void parseAgentArgs(String agentArgs) {
        if (agentArgs != null && !agentArgs.isEmpty()) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                String[] keyValue = arg.split("=");
                if (keyValue.length == 2) {
                    options.put(keyValue[0], keyValue[1]);
                }
            }
        }
    }

    public static String getOption(String key) {
        return options.get(key);
    }
}
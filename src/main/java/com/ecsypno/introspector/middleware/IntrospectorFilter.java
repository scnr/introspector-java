package com.ecsypno.introspector.middleware;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.ecsypno.introspector.core.CodeTracer;
import com.ecsypno.introspector.core.CodeTracer.Trace;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@WebFilter("/*")
public class IntrospectorFilter implements Filter {

    private static class ResponseWrapper extends HttpServletResponseWrapper {
        private final CharArrayWriter writer;

        public ResponseWrapper(HttpServletResponse response) {
            super(response);
            writer = new CharArrayWriter();
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(writer);
        }

        @Override
        public String toString() {
            return writer.toString();
        }

        public void writeTo(PrintWriter out) throws IOException {
            out.write(writer.toString());
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            ResponseWrapper wrapper = new ResponseWrapper(httpResponse);
            List<Trace> traces = new ArrayList<>();

            try {
                CodeTracer.clearTraces();
                chain.doFilter(request, wrapper);
                traces = CodeTracer.getTraces();
            } finally {
                String ct = response.getContentType();
    
                if (httpRequest.getHeader("X-Scnr-Introspector-Trace") != null &&
                    ct != null && ct.contains("html")) {
    
                    String content = wrapper.toString();
                    String tracesAsJson = formatTracesAsJson(traces, httpRequest.getHeader("X-Scnr-Engine-Scan-Seed"));
                    String modifiedContent = injectTraces(content, tracesAsJson);
    
                    traces.clear();
                    writeResponse(httpResponse, modifiedContent);
                } else {
                    wrapper.writeTo(httpResponse.getWriter());
                }

                CodeTracer.clearTraces();
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private String formatTracesAsJson(List<Trace> traces, String seed) {
        StringBuilder json = new StringBuilder();
        String osType = getOSType();
    
        json.append("\n<!-- ").append(seed).append("\n");
        json.append("{\n");
        json.append("    \"platforms\": [\"java\"");
        if (osType != null) {
            json.append(", \"").append(osType).append("\"");
        }
        json.append("],\n");
        json.append("    \"execution_flow\": {\n");
        json.append("        \"points\": [\n");
    
        for (int i = 0; i < traces.size(); i++) {
            Trace trace = traces.get(i);
            json.append("            {\n");
            json.append("                \"method_name\": \"").append(trace.getMethodName()).append("\",\n");
            json.append("                \"class_name\": \"").append(trace.getClassName()).append("\",\n");
            json.append("                \"path\": \"").append(trace.getFilePath()).append("\",\n");
            json.append("                \"line_number\": ").append(trace.getLineNumber()).append(",\n");
            json.append("                \"source\": \"").append(escapeJsonString(trace.getSourceCode())).append("\",\n");
            json.append("                \"file_contents\": \"").append(escapeJsonString(trace.getFileContents())).append("\"\n");
            json.append("            }").append(i < traces.size() - 1 ? "," : "").append("\n");
        }
    
        json.append("        ]\n");
        json.append("    }\n");
        json.append("}\n");
        json.append(seed).append(" -->\n");
        return json.toString();
    }

    public String getOSType() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "windows";
        } else if (osName.contains("nix")) {
            return "unix";
        } else if (osName.contains("nux")) {
            return "linux";
        } else if (osName.contains("aix")) {
            return "aix";
        } else if (osName.contains("sunos")) {
            return "solaris";
        } else {
            return null;
        }
    }

    private String injectTraces(String content, String traces) {
        return content + traces;
    }

    private void writeResponse(HttpServletResponse response, String content) throws IOException {
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(contentBytes.length);
        response.getWriter().write(content);
    }

    private String escapeJsonString(String value) {
        StringBuilder escapedValue = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':
                    escapedValue.append("\\\"");
                    break;
                case '\\':
                    escapedValue.append("\\\\");
                    break;
                case '\b':
                    escapedValue.append("\\b");
                    break;
                case '\f':
                    escapedValue.append("\\f");
                    break;
                case '\n':
                    escapedValue.append("\\n");
                    break;
                case '\r':
                    escapedValue.append("\\r");
                    break;
                case '\t':
                    escapedValue.append("\\t");
                    break;
                default:
                    if (c < 0x20 || c > 0x7E) {
                        escapedValue.append(String.format("\\u%04x", (int) c));
                    } else {
                        escapedValue.append(c);
                    }
            }
        }
        return escapedValue.toString();
    }

    @Override
    public void destroy() {
    }
}
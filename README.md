# Codename SCNR Introspector (Java)

## Options

* `path_start_with`
* `path_ends_with`
* `path_include_pattern`
* `path_exclude_pattern`
* `source_directory`

## Load middleware

```bash
MAVEN_OPTS="-javaagent:introspector-1.0.jar=path_start_with=com/example" mvn clean package tomcat7:run
```

## Verify

You should see this at the beginning:

```
[INTROSPECTOR] Initializing Codename SCNR Introspector agent...
[INTROSPECTOR] Setting up instrumentation.
```

And messages like this after initialization for each traced line:

```
[INTROSPECTOR] Injecting trace code for com/example/XssServlet.<init> line 11 in src/main/java//com/example/XssServlet.java
```

```bash
curl -i http://localhost:8080/ -H "X-Scnr-Engine-Scan-Seed:Test" -H "X-Scnr-Introspector-Trace:1" -H "X-SCNR-Request-ID:1"
```

At the end of the HTTP response you should be seeing something like:

```html
<!-- Test
{
    "platforms": ["java"],
    "execution_flow": {
        "points": [
            {
                "method_name": "doGet",
                "class_name": "com/example/SampleWebApp",
                "path": "src/main/java//com/example/SampleWebApp.java",
                "line_number": 16,
                "source": "        resp.setContentType(\"text/html\");",
                "file_contents": "package com.example;\n\nimport java.io.IOException;\nimport javax.servlet.ServletException;\nimport javax.servlet.annotation.WebServlet;\nimport javax.servlet.http.HttpServlet;\nimport javax.servlet.http.HttpServletRequest;\nimport javax.servlet.http.HttpServletResponse;\n\n@WebServlet(\"/\")\npublic class SampleWebApp extends HttpServlet {\n    @Override\n    protected void doGet(HttpServletRequest req, HttpServletResponse resp) \n            throws ServletException, IOException {\n\n        resp.setContentType(\"text/html\");\n\n        resp.getWriter().println(\"<html><body>\");\n        resp.getWriter().println(\"<ul>\");\n        resp.getWriter().println(\"<li><a href='/xss'>XSS</a></li>\");\n        resp.getWriter().println(\"<li><a href='/cmd'>OS Command Injection</a></li>\");\n        resp.getWriter().println(\"</ul>\");\n        resp.getWriter().println(\"</body></html>\");\n    }\n}"
            },
            [...]
        ]
    }
}
Test -->
```

## License

All rights reserved Ecsypno Single Member P.C.

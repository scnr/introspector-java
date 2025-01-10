# Codename SCNR Introspector (Java)

## Options

* `path_start_with`
* `path_ends_with`
* `path_include_pattern`
* `path_exclude_pattern`

## Load middleware

```bash
MAVEN_OPTS="-javaagent:introspector-1.0.jar=path_start_with=com/example" mvn clean package tomcat7:run
```

## Verify

```bash
curl -i http://localhost:5055/ -H "X-Scnr-Engine-Scan-Seed:Test" -H "X-Scnr-Introspector-Trace:1" -H "X-SCNR-Request-ID:1"
```

## License

All rights reserved Ecsypno Single Member P.C.

# ABL Annotations Manifest

The ABL Engine relies on a strict set of runtime annotations to construct its dynamic execution pipeline and dependency graph. The following annotations are the definitive set implemented in the `com.anyonehub.abl.annotations` package and actively parsed by the `AblRuntimeScanner`.

### 1. `@AblCompile`
- **Target:** `AnnotationTarget.CLASS`
- **Description:** Marks a class that needs to be dynamically compiled and loaded by the runtime engine. The compiler pipeline scans for this tag to determine which classes must be transpiled to Dalvik bytecode.

### 2. `@AblEntryPoint`
- **Target:** `AnnotationTarget.FUNCTION`
- **Description:** Identifies the exact function where the compiled execution pipeline should begin. The engine reflects upon the compiled Dalvik classes to invoke the method tagged with this annotation.

### 3. `@AblInject`
- **Target:** `AnnotationTarget.PROPERTY`, `AnnotationTarget.CONSTRUCTOR`
- **Description:** Tags dependencies that the `ModuleResolver` must inject at runtime before execution. The dependency injection container uses this to wire properties or constructor parameters dynamically.

### 4. `@AblModule`
- **Target:** `AnnotationTarget.CLASS`
- **Description:** Defines a configuration module that provides dependencies for the execution graph. The engine parses modules to seed the `RuntimeContainer` with available factories and singleton bindings.

> **Audit Note:** All 4 annotations are strictly validated and correctly mapped within the `AblRuntimeScanner` logic. There are no dangling or unhandled annotations in the pipeline.

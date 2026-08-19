@echo off
echo Verifying and Publishing ABL Runtime Compiler Engine...
call gradlew.bat clean shadowJar publishToMavenLocal
if %errorlevel% neq 0 (
    echo [ERROR] Build or Publish failed.
    exit /b %errorlevel%
)
echo Successfully published ABL artifacts to local Maven repository!

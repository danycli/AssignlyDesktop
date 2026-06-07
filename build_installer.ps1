# Build script for Assignly Desktop EXE Installer
$ErrorActionPreference = "Stop"

Write-Host "=== Step 1: Configuring PATH for Maven ===" -ForegroundColor Cyan
$vscodeMvn = Get-ChildItem -Path "$env:USERPROFILE\.vscode\extensions" -Filter "mvn.cmd" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
if ($vscodeMvn) {
    $mvnDir = [System.IO.Path]::GetDirectoryName($vscodeMvn)
    Write-Host "Found VS Code Embedded Maven at: $vscodeMvn" -ForegroundColor Green
    $env:PATH += ";$mvnDir"
} else {
    Write-Host "VS Code Embedded Maven not found. Relying on system PATH." -ForegroundColor Yellow
}

# Verify maven is available
where.exe mvn.cmd
if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven (mvn.cmd) is not in the system path and could not be auto-detected."
}

Write-Host "=== Step 2: Packaging project jar ===" -ForegroundColor Cyan
# Run package without clean to avoid language server file lock issues
mvn package -DskipTests

Write-Host "=== Step 3: Copying dependencies to target/libs ===" -ForegroundColor Cyan
mvn dependency:copy-dependencies -DoutputDirectory=target/libs

# Clean up test-only dependencies to shrink app footprint even further!
Write-Host "=== Step 3b: Removing test-only dependencies to shrink size ===" -ForegroundColor Cyan
Remove-Item -Path "target/libs/junit-*" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "target/libs/mockito-*" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "target/libs/byte-buddy-*" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "target/libs/objenesis-*" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "target/libs/apiguardian-*" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "target/libs/opentest4j-*" -Force -ErrorAction SilentlyContinue

Write-Host "=== Step 4: Copying main jar to target/libs ===" -ForegroundColor Cyan
Copy-Item -Path "target/assignly-desktop-1.0.0.jar" -Destination "target/libs/assignly-desktop-1.0.0.jar" -Force

Write-Host "=== Step 5: Generating custom, lightweight Java Runtime Image (jlink) ===" -ForegroundColor Cyan
# Remove old runtime if exists
Remove-Item -Path "target/custom-runtime" -Recurse -Force -ErrorAction SilentlyContinue

jlink --add-modules java.base,java.sql,java.naming,java.desktop,java.xml,jdk.unsupported,jdk.charsets,java.logging,java.management,java.scripting,jdk.xml.dom,jdk.jsobject,jdk.crypto.ec,jdk.crypto.mscapi,jdk.crypto.cryptoki,java.xml.crypto,java.net.http `
      --output target/custom-runtime `
      --strip-debug `
      --no-man-pages `
      --no-header-files `
      --compress=2

Write-Host "=== Step 6: Configuring PATH for WiX Toolset ===" -ForegroundColor Cyan
$wixPath = "C:\Program Files (x86)\WiX Toolset v3.14\bin"
if (Test-Path $wixPath) {
    Write-Host "Found WiX Toolset at: $wixPath" -ForegroundColor Green
    $env:PATH += ";$wixPath"
} else {
    Write-Error "WiX Toolset v3.14 was not found at $wixPath. Please verify the installation path."
}

Write-Host "=== Step 7: Building EXE Installer with jpackage ===" -ForegroundColor Cyan
# Clear old installer output directory
Remove-Item -Path "target/installer" -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "target/installer" | Out-Null

jpackage --type exe `
         --input target/libs `
         --dest target/installer `
         --name "AssignlyDesktop" `
         --main-jar assignly-desktop-1.0.0.jar `
         --main-class com.assignly.Launcher `
         --runtime-image target/custom-runtime `
         --icon "src/main/resources/com/assignly/images/favicon.ico" `
         --app-version "1.0.0" `
         --vendor "Assignly" `
         --win-dir-chooser `
         --win-shortcut `
         --win-menu `
         --win-menu-group "Assignly"

Write-Host "=== Build SUCCESS ===" -ForegroundColor Green
Write-Host "Installer is available at: target/installer/AssignlyDesktop-1.0.0.exe" -ForegroundColor Green

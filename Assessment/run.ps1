if ((-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) -and (Test-Path -LiteralPath 'C:\Program Files\Java\jdk-11\bin\javac.exe')) {
    $env:JAVA_HOME = 'C:\Program Files\Java\jdk-11'
}
Push-Location $PSScriptRoot
try {
    & mvn clean spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "Maven exited with code $LASTEXITCODE" }
} finally { Pop-Location }

@echo off
setlocal enabledelayedexpansion

echo ================================
echo Instalador de Certificado Cosmos DB Emulator
echo ================================
echo.

REM Configurar variables
set JAVA_HOME=C:\Users\ACER\OneDrive-CIBERTEC\Documents\libreria_jdk\jdk-17.0.11+9
set KEYTOOL=%JAVA_HOME%\bin\keytool.exe
set CACERTS=%JAVA_HOME%\lib\security\cacerts
set COSMOS_HOST=127.0.0.1
set COSMOS_PORT=8081
set CERT_ALIAS=cosmosdb
set KEYSTORE_PASS=changeit

echo Configuracion:
echo - JAVA_HOME: %JAVA_HOME%
echo - Cacerts: %CACERTS%
echo - Cosmos DB: %COSMOS_HOST%:%COSMOS_PORT%
echo.

REM Verificar que Java Home existe
if not exist "%JAVA_HOME%" (
    echo ERROR: JAVA_HOME no encontrado: %JAVA_HOME%
    echo Por favor verifica la ruta de Java.
    pause
    exit /b 1
)

REM Verificar que keytool existe
if not exist "%KEYTOOL%" (
    echo ERROR: keytool no encontrado: %KEYTOOL%
    pause
    exit /b 1
)

REM Verificar que cacerts existe
if not exist "%CACERTS%" (
    echo ERROR: cacerts no encontrado: %CACERTS%
    pause
    exit /b 1
)

echo Paso 1: Verificando si el certificado ya existe...
"%KEYTOOL%" -list -keystore "%CACERTS%" -storepass %KEYSTORE_PASS% -alias %CERT_ALIAS% >nul 2>&1
if !errorlevel! == 0 (
    echo El certificado '%CERT_ALIAS%' YA EXISTE en cacerts.
    echo.
    choice /c SN /m "¿Deseas reemplazarlo? (S/N)"
    if !errorlevel! == 2 (
        echo Operacion cancelada.
        pause
        exit /b 0
    )

    echo Eliminando certificado existente...
    "%KEYTOOL%" -delete -keystore "%CACERTS%" -storepass %KEYSTORE_PASS% -alias %CERT_ALIAS%
    if !errorlevel! neq 0 (
        echo ERROR: No se pudo eliminar el certificado existente.
        pause
        exit /b 1
    )
    echo Certificado existente eliminado.
) else (
    echo El certificado '%CERT_ALIAS%' NO existe en cacerts.
)

echo.
echo Paso 2: Verificando conexion con Cosmos DB Emulator...
netstat -an | findstr ":8081" >nul
if !errorlevel! neq 0 (
    echo ERROR: Cosmos DB Emulator no parece estar ejecutandose en puerto 8081.
    echo Por favor verifica que Docker este ejecutando los contenedores.
    echo.
    echo Ejecuta: docker ps
    echo Deberia mostrar el contenedor cosmosdb-emulator ejecutandose.
    pause
    exit /b 1
)
echo Cosmos DB Emulator detectado en puerto 8081.

echo.
echo Paso 3: Descargando certificado SSL...
set CERT_FILE=%TEMP%\cosmosdb.crt

REM Crear script temporal de PowerShell para obtener el certificado
set PS_SCRIPT=%TEMP%\get_cosmos_cert.ps1
echo $ErrorActionPreference = 'Stop' > "%PS_SCRIPT%"
echo try { >> "%PS_SCRIPT%"
echo     $tcpClient = New-Object System.Net.Sockets.TcpClient >> "%PS_SCRIPT%"
echo     $tcpClient.Connect('%COSMOS_HOST%', %COSMOS_PORT%) >> "%PS_SCRIPT%"
echo     $sslStream = New-Object System.Net.Security.SslStream($tcpClient.GetStream(), $false, {$true}) >> "%PS_SCRIPT%"
echo     $sslStream.AuthenticateAsClient('%COSMOS_HOST%') >> "%PS_SCRIPT%"
echo     $cert = $sslStream.RemoteCertificate >> "%PS_SCRIPT%"
echo     $certBytes = $cert.Export([System.Security.Cryptography.X509Certificates.X509ContentType]::Cert) >> "%PS_SCRIPT%"
echo     [System.IO.File]::WriteAllBytes('%CERT_FILE%', $certBytes) >> "%PS_SCRIPT%"
echo     $sslStream.Close() >> "%PS_SCRIPT%"
echo     $tcpClient.Close() >> "%PS_SCRIPT%"
echo     Write-Host "Certificado descargado exitosamente" >> "%PS_SCRIPT%"
echo } catch { >> "%PS_SCRIPT%"
echo     Write-Host "Error al descargar certificado: $_" -ForegroundColor Red >> "%PS_SCRIPT%"
echo     exit 1 >> "%PS_SCRIPT%"
echo } >> "%PS_SCRIPT%"

powershell.exe -ExecutionPolicy Bypass -File "%PS_SCRIPT%"
if !errorlevel! neq 0 (
    echo ERROR: No se pudo descargar el certificado.
    del "%PS_SCRIPT%" 2>nul
    pause
    exit /b 1
)

del "%PS_SCRIPT%" 2>nul

if not exist "%CERT_FILE%" (
    echo ERROR: El archivo de certificado no se creo.
    pause
    exit /b 1
)

echo Certificado descargado en: %CERT_FILE%

echo.
echo Paso 4: Importando certificado a cacerts...
echo ADVERTENCIA: Se requieren permisos de administrador para modificar cacerts.
echo.

REM Intentar importar el certificado
"%KEYTOOL%" -importcert -file "%CERT_FILE%" -keystore "%CACERTS%" -storepass %KEYSTORE_PASS% -alias %CERT_ALIAS% -noprompt
if !errorlevel! neq 0 (
    echo ERROR: No se pudo importar el certificado.
    echo Posibles causas:
    echo 1. Permisos insuficientes - ejecuta como administrador
    echo 2. Cacerts protegido contra escritura
    echo.
    echo Intentando con runas...
    runas /user:Administrator "%KEYTOOL% -importcert -file %CERT_FILE% -keystore %CACERTS% -storepass %KEYSTORE_PASS% -alias %CERT_ALIAS% -noprompt"
    if !errorlevel! neq 0 (
        echo ERROR: Importacion fallida incluso con runas.
        del "%CERT_FILE%" 2>nul
        pause
        exit /b 1
    )
)

echo Certificado importado exitosamente!

echo.
echo Paso 5: Verificando instalacion...
"%KEYTOOL%" -list -keystore "%CACERTS%" -storepass %KEYSTORE_PASS% -alias %CERT_ALIAS%
if !errorlevel! neq 0 (
    echo ERROR: El certificado no se encuentra en cacerts despues de la importacion.
    del "%CERT_FILE%" 2>nul
    pause
    exit /b 1
)

echo.
echo Paso 6: Limpiando archivos temporales...
del "%CERT_FILE%" 2>nul

echo.
echo ================================
echo INSTALACION COMPLETADA!
echo ================================
echo.
echo El certificado de Cosmos DB Emulator ha sido instalado exitosamente.
echo Alias: %CERT_ALIAS%
echo.
echo Ahora puedes ejecutar tu aplicacion Spring Boot.
echo Si aun hay problemas de SSL, reinicia tu aplicacion.
echo.
echo Para verificar que el certificado esta instalado, ejecuta:
echo "%KEYTOOL%" -list -keystore "%CACERTS%" -storepass %KEYSTORE_PASS% -alias %CERT_ALIAS%
echo.
pause
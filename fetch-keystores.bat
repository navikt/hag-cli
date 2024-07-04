@echo off

set KEYTSTORE_PROD_NAME=aiven-prod-keystore.p12
set TRUSTORE_PROD_NAME=aiven-prod-truststore.jks
set KEYTSTORE_DEV_NAME=aiven-dev-keystore.p12
set TRUSTORE_DEV_NAME=aiven-dev-truststore.jks
set SCRIPT_DIR=%~dp0

if "%1" == "" (
    echo Usage: %~n0 ^<prod secret name^> [^<dev secret name^>]
    echo You must provide prod secret name.
    exit /b 1
)

call:kx prod-gcp
call:getSecrets %1 %KEYTSTORE_PROD_NAME% %TRUSTORE_PROD_NAME% "%SCRIPT_DIR%config\prod-aiven.properties"

if not "%2" == "" (
    call:kx dev-gcp
    call:getSecrets %2 %KEYTSTORE_DEV_NAME% %TRUSTORE_DEV_NAME% "%SCRIPT_DIR%config\dev-aiven.properties"
)

exit /b 0

:kx
    kubectl config use-context %1
exit /b 0

:checkKeystore
    keytool -list -v -keystore %1 -storepass changeme -storetype PKCS12 > nul
    if errorlevel 1 (
        echo Keystore is corrupt, or bad password
    ) else (
        echo Keystore %1 seem to be ok ...
    )
exit /b 0

:checkTruststore
    keytool -list -v -keystore %1 -storepass changeme > nul
    if errorlevel 1 (
        echo Truststore is corrupt, or bad password
    ) else (
        echo Trustore %1 seem to be ok ...
    )
exit /b 0

:getSecrets
    for /f %%a in (
        'kubectl get secret %1 -n helsearbeidsgiver -o jsonpath^={.data.KAFKA_BROKERS} ^| base64 -di'
    ) do (
        set BROKERS=%%a
    )
    echo Brokers: %BROKERS%

    kubectl get secret %1 -n helsearbeidsgiver -o jsonpath={.data.client\.keystore\.p12} | tee %2.base64 | base64 -di > %2
    kubectl get secret %1 -n helsearbeidsgiver -o jsonpath={.data.client\.truststore\.jks} | tee %3.base64 | base64 -di > %3

    call:checkKeystore %2
    call:checkTruststore %3

    echo Writing properties to %4
    echo config = aiven> %4
    echo aiven.brokers.url = %BROKERS%>> %4
    echo aiven.truststore.path = %~dp0%3| sed -e 's/\\\/\//g' >> %4
    echo aiven.truststore.password = changeme>> %4
    echo aiven.keystore.path = %~dp0%2| sed -e 's/\\\/\//g' >> %4
    echo aiven.keystore.password = changeme>> %4
exit /b 0

@echo off

set BINARY_PATH=%USERPROFILE%\hag-cli\rr.jar
set DOWNLOAD_URL=https://github.com/navikt/hag-cli/releases/latest/download/app.jar
set EXIT_CODE_UPDATED_VERSION=10
if not exist %BINARY_PATH% (
    curl -fsLo - %DOWNLOAD_URL% > %BINARY_PATH%
) else (
    java -jar %BINARY_PATH% %1 check_version --download %TEMP%\rr-updated.jar
    if errorlevel %EXIT_CODE_UPDATED_VERSION% (
        echo New version was downloaded
        move %TEMP%\rr-updated.jar %BINARY_PATH%
    )
)
java -jar %BINARY_PATH% %*

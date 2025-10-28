@echo off
echo ========================================
echo  NML Online - Demarrage du Frontend
echo ========================================
echo.

cd /d "%~dp0"

echo Verification de l'installation des dependances...
if not exist "node_modules" (
    echo Installation des dependances npm...
    call npm install
    if errorlevel 1 (
        echo Erreur lors de l'installation des dependances
        pause
        exit /b 1
    )
)

echo.
echo Demarrage du serveur de developpement...
echo Le frontend sera accessible sur: http://localhost:5174
echo.
echo IMPORTANT: Assurez-vous que le backend Spring Boot tourne sur le port 8080
echo.

call npm run dev

pause


# AI-Ready Database Initialization Script
# Created: 2026-05-28

# Database connection configuration
$DbHost = "localhost"
$DbPort = 5432
$DbName = "devdb"
$DbUser = "devuser"
$DbPassword = "Dev@2026#Local"

# SQL script directory
$SqlDir = "I:\AI-Ready\backend\sql"

# SQL script list
$SqlFiles = @(
    "01_core_base.sql",
    "02_init_data.sql",
    "03_erp_modules.sql",
    "04_crm_modules.sql"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AI-Ready Database Initialization" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check if PostgreSQL is installed
$PgPath = Get-ChildItem -Path "C:\Program Files" -Filter "psql.exe" -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1

if ($PgPath) {
    Write-Host "Found PostgreSQL at: $($PgPath.FullName)" -ForegroundColor Green
    
    # Execute SQL scripts using psql
    foreach ($SqlFile in $SqlFiles) {
        $FilePath = Join-Path $SqlDir $SqlFile
        
        if (Test-Path $FilePath) {
            Write-Host "Executing: $SqlFile" -ForegroundColor Cyan
            
            $Env:PGPASSWORD = $DbPassword
            & $PgPath.FullName -h $DbHost -p $DbPort -U $DbUser -d $DbName -f $FilePath
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "Success: $SqlFile" -ForegroundColor Green
            } else {
                Write-Host "Failed: $SqlFile" -ForegroundColor Red
            }
        } else {
            Write-Host "File not found: $SqlFile" -ForegroundColor Red
        }
    }
    
    Write-Host "Database initialization completed" -ForegroundColor Green
} else {
    Write-Host "PostgreSQL not found" -ForegroundColor Yellow
    Write-Host "Please manually execute SQL scripts using pgAdmin" -ForegroundColor Yellow
    Write-Host "SQL script directory: $SqlDir" -ForegroundColor Yellow
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Manual Import Instructions:" -ForegroundColor Yellow
Write-Host "1. Open pgAdmin" -ForegroundColor Yellow
Write-Host "2. Connect to database: $DbName" -ForegroundColor Yellow
Write-Host "3. Execute SQL scripts in order:" -ForegroundColor Yellow
foreach ($SqlFile in $SqlFiles) {
    Write-Host "   - $SqlFile" -ForegroundColor Yellow
}
Write-Host "========================================" -ForegroundColor Cyan
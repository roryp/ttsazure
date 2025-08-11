# PowerShell script to load environment variables and run the Spring Boot application

# Check if .env file exists
if (Test-Path .env) {
    Write-Host "Loading environment variables from .env file..." -ForegroundColor Green
    
    # Read and set environment variables
    Get-Content .env | ForEach-Object {
        if ($_ -match '^([^#].*)=(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($name, $value, [EnvironmentVariableTarget]::Process)
            Write-Host "Set $name" -ForegroundColor Gray
        }
    }
} else {
    Write-Host "Warning: .env file not found. Using default values or system environment variables." -ForegroundColor Yellow
}

# Run the Spring Boot application
Write-Host "Starting Spring Boot application..." -ForegroundColor Green
& mvn spring-boot:run

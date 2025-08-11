# Azure OpenAI Text-to-Speech Application

A Spring Boot web application that uses Azure OpenAI's TTS (Text-to-Speech) service to convert text into natural-sounding speech. The application runs on Azure Container Apps and uses Managed Identity for secure authentication.

## Features

- **Six Voice Options**: Alloy, Echo, Fable, Onyx, Nova, Shimmer
- **Voice Styling**: Add emotion or style to your generated speech
- **Real-time Audio**: Inline audio player with autoplay option
- **Download Support**: Download generated audio as MP3 files
- **Responsive UI**: Modern, accessible web interface using Thymeleaf
- **Azure Container Apps**: Scalable containerized deployment
- **Managed Identity**: Secure authentication without API keys
- **Auto-expiring Cache**: 10-minute TTL for generated audio files

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Web Browser    │───▶│ Container Apps  │───▶│ Azure OpenAI    │
│                 │    │  (Spring Boot)  │    │   (TTS Model)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │ Container       │
                       │ Registry (ACR)  │
                       └─────────────────┘
```

## Prerequisites

- [Azure Developer CLI (azd)](https://learn.microsoft.com/en-us/azure/developer/azure-developer-cli/install-azd)
- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)
- [Java 21](https://adoptium.net/)
- [Docker](https://www.docker.com/get-started) (for local builds)
- Azure subscription with access to Azure OpenAI

## Quick Start

### First-time Setup

1. **Clone and navigate to the repository**
   ```bash
   git clone <repository-url>
   cd tts-ai-app
   ```

2. **Login to Azure**
   ```bash
   azd auth login
   az login
   ```

3. **Set your Azure subscription**
   ```bash
   az account set -s "<your-subscription-id>"
   ```

4. **Deploy the application**
   ```bash
   azd up
   ```

   This will:
   - Create the resource group `tts-ai-app` in `eastus2`
   - Provision all Azure resources (Container Apps, ACR, OpenAI, etc.)
   - Build and deploy the application
   - Configure Managed Identity permissions

5. **Get your application URL**
   ```bash
   az containerapp show -n tts-ai-app -g tts-ai-app --query properties.configuration.ingress.fqdn -o tsv
   ```

### Subsequent Deployments

For code changes, you can deploy just the application:

```bash
azd deploy
```

Or for infrastructure + application changes:

```bash
azd up
```

## Local Development

### Running Locally

1. **Authenticate with Azure**
   ```bash
   az login
   ```

2. **Set environment variables** (get values from Azure)
   ```bash
   export AZURE_OPENAI_ENDPOINT="https://your-openai-resource.openai.azure.com/"
   export AZURE_OPENAI_DEPLOYMENT="gpt-4o-mini-tts"
   export AZURE_OPENAI_MODEL="gpt-4o-mini-tts"
   ```

3. **Run the application**
   ```bash
   cd src
   mvn spring-boot:run
   ```

4. **Access the application**
   Open http://localhost:8080 in your browser

### Building Docker Image Locally

```bash
cd src
docker build -t tts-ai-app .
docker run -p 8080:8080 \
  -e AZURE_OPENAI_ENDPOINT="your-endpoint" \
  -e AZURE_OPENAI_DEPLOYMENT="gpt-4o-mini-tts" \
  -e AZURE_OPENAI_MODEL="gpt-4o-mini-tts" \
  tts-ai-app
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `AZURE_OPENAI_ENDPOINT` | Azure OpenAI service endpoint | Required |
| `AZURE_OPENAI_DEPLOYMENT` | Deployment name for TTS model | `gpt-4o-mini-tts` |
| `AZURE_OPENAI_MODEL` | TTS model name | `gpt-4o-mini-tts` |

### Azure Resources Created

- **Resource Group**: `tts-ai-app` (eastus2)
- **Container Apps Environment**: Hosts the application
- **Container App**: `tts-ai-app` - The main application
- **Container Registry**: Stores Docker images
- **Azure OpenAI**: Cognitive Services with GPT-4o Mini TTS deployment
- **Log Analytics**: Application monitoring and logs
- **Application Insights**: Application performance monitoring

### Managed Identity Permissions

The Container App's system-assigned managed identity has:

- **Cognitive Services User** role on the Azure OpenAI account
- **AcrPull** role on the Container Registry

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/` | Main TTS form interface |
| `POST` | `/tts` | Generate speech from text |
| `GET` | `/audio/{id}` | Stream audio file |
| `GET` | `/audio/{id}?download=1` | Download audio file |
| `GET` | `/health` | Health check endpoint |

## GitHub Actions CI/CD

The repository includes automated deployment via GitHub Actions.

### Setup

1. **Create a Service Principal**
   ```bash
   az ad sp create-for-rbac --name "tts-ai-app-github" \
     --role Contributor \
     --scopes /subscriptions/{subscription-id} \
     --json-auth
   ```

2. **Configure GitHub Secrets**
   - `AZURE_CLIENT_ID`: Service principal client ID
   - `AZURE_TENANT_ID`: Azure tenant ID
   - `AZURE_SUBSCRIPTION_ID`: Azure subscription ID

3. **Configure GitHub Variables**
   - `AZURE_ENV_NAME`: Environment name (e.g., `tts-ai-app`)
   - `AZURE_LOCATION`: Azure region (e.g., `eastus2`)

### Federated Credentials (Recommended)

For enhanced security, set up federated credentials:

```bash
az ad app federated-credential create \
  --id {app-id} \
  --parameters '{
    "name": "tts-ai-app-github-main",
    "issuer": "https://token.actions.githubusercontent.com",
    "subject": "repo:{org}/{repo}:ref:refs/heads/main",
    "audiences": ["api://AzureADTokenExchange"]
  }'
```

## Troubleshooting

### Common Issues

1. **403 Forbidden errors**
   - Wait 5-10 minutes for role assignment propagation
   - Verify Managed Identity has correct permissions
   - Check Azure OpenAI resource is accessible

2. **Container app won't start**
   - Check application logs: `az containerapp logs show -n tts-ai-app -g tts-ai-app`
   - Verify environment variables are set correctly
   - Ensure container image was built successfully

3. **Audio generation fails**
   - Verify Azure OpenAI deployment is active
   - Check input text length (max 1000 characters)
   - Ensure selected voice is valid

### Useful Commands

```bash
# View application logs
az containerapp logs show -n tts-ai-app -g tts-ai-app --follow

# Check container app status
az containerapp show -n tts-ai-app -g tts-ai-app

# View OpenAI deployments
az cognitiveservices account deployment list \
  --name {openai-account-name} \
  --resource-group tts-ai-app

# Check managed identity
az containerapp identity show -n tts-ai-app -g tts-ai-app

# Force new deployment
azd deploy --force

# Clean up resources
azd down --force --purge
```

## Development with GitHub Codespaces

This repository is Codespaces-ready:

1. Click "Code" → "Create codespace on main"
2. Wait for environment setup
3. Run `azd auth login` and follow setup instructions
4. Deploy with `azd up`

## Security Features

- **No API Keys**: Uses Managed Identity for all Azure service authentication
- **Secure Storage**: Audio files expire automatically after 10 minutes
- **Input Validation**: Text length limits and voice validation
- **Container Security**: Non-root user execution in Docker container
- **Network Security**: HTTPS-only ingress with Azure Container Apps

## Performance & Monitoring

- **Auto-scaling**: Container Apps scales based on demand
- **Health Checks**: Built-in health monitoring
- **Application Insights**: Performance and usage analytics
- **Log Analytics**: Centralized logging and monitoring

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## Support

For issues and questions:
- Check the [troubleshooting section](#troubleshooting)
- Review Azure Container Apps documentation
- Check Azure OpenAI service status

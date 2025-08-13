# 🎵 Azure OpenAI TTS Audio Soundboard

A modern, interactive text-to-speech application built with Spring Boot and Azure OpenAI's **gpt-4o-mini-tts** model (2025-03-20). Transform any text into natural-sounding speech with multiple voice options, customizable styles, and advanced tone guidance.

![Azure OpenAI TTS](https://img.shields.io/badge/Azure%20OpenAI-gpt--4o--mini--tts-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-green)
![Java](https://img.shields.io/badge/Java-21-orange)
![Azure Container Apps](https://img.shields.io/badge/Azure-Container%20Apps-blue)
![License](https://img.shields.io/badge/License-MIT-blue)

## ✨ Features

### 🎤 **Voice Selection**
- **11 Premium Voices**: Alloy, Ash, Ballad, Coral, Echo, Fable, Nova, Onyx, Sage, Shimmer, Verse
- **Interactive Voice Buttons** with visual feedback and hover effects
- **Random Voice Selection** for variety and experimentation
- **Modern Grid Layout** that adapts to all screen sizes

### 🎭 **Advanced Vibe System**
- **12 Predefined Vibes** with detailed voice instructions:
  - **Excited** - High energy and enthusiastic delivery
  - **Calm** - Peaceful and soothing tone
  - **Professional** - Clear, authoritative business communication
  - **Friendly** - Warm and approachable conversation
  - **Mysterious** - Enigmatic and intriguing atmosphere
  - **Dramatic** - Theatrical and compelling presentation
  - **Playful** - Light-hearted and fun delivery
  - **Whisper** - Intimate and hushed communication
  - **Angry** - Controlled intensity and frustration
  - **Sad** - Gentle melancholy and empathy
  - **Cheerful** - Bright and joyful expression
  - **Sarcastic** - Witty and ironic delivery

- **Sample Scripts** for each vibe to get started quickly
- **Shuffle Vibes** functionality to discover new combinations
- **Dynamic Vibe Loading** with interactive selection

### 🎚️ **Audio Controls**
- **Multiple Audio Formats**: MP3 (recommended), WAV (high quality), Opus (compact)
- **Auto-play Support** with fallback for browser restrictions
- **Download Options** for generated audio files
- **Real-time Audio Streaming** with instant playback

### 💻 **User Interface**
- **Modern Soundboard Design** inspired by professional audio tools
- **Responsive Layout** that works on desktop, tablet, and mobile
- **Interactive Elements** with smooth animations and transitions
- **Visual Feedback** for all user actions and selections
- **Character Counter** with smart validation (up to 4,000 characters)

## 🚀 Quick Start

### Prerequisites
- [Azure Developer CLI (azd)](https://aka.ms/azd-install)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (for containerization)
- Azure subscription with access to East US or East US 2 regions
- No need for existing Azure OpenAI resources - azd creates everything!

### 1. Clone the Repository
```bash
git clone https://github.com/roryp/ttsazure.git
cd ttsazure
```

### 2. Deploy to Azure
```bash
# Initialize and deploy everything in one command
azd up
```

That's it! The `azd up` command will:
- ✅ Create a new Azure OpenAI resource
- ✅ Deploy the gpt-4o-mini model with TTS support (version 2025-03-20)
- ✅ Set up Container Registry for your app
- ✅ Create Container Apps environment
- ✅ Configure Managed Identity with token-based authentication
- ✅ Build and deploy your application
- ✅ Provide you with the live application URL

### 3. Access Your Application
After deployment completes, azd will display your application URL:
```
Your app is live at: https://your-app-name.azurecontainerapps.io
```

### 4. Local Development (Optional)
For local testing with existing Azure resources:
```bash
# Install Java 21 and Maven first
mvn spring-boot:run
# Access at: http://localhost:8080
```

## 📱 How to Use

### Step 1: Select a Voice
Click on any of the 11 voice buttons to choose your preferred voice. You can also click "Random" to let the system pick one for you.

### Step 2: Choose a Vibe (Optional)
Select from 12 predefined vibes to automatically configure the voice style and get sample text. You can:
- Browse the current selection of vibes
- Click "🎲 Shuffle Vibes" to see different options
- View detailed descriptions and sample scripts
- Click "📝 Use Vibe Script" to populate the text area

### Step 3: Enter Your Text
- Type or paste your text (up to 4,000 characters)
- Use the provided vibe script or create your own content
- The character counter helps you stay within limits

### Step 4: Generate Speech
- Choose your preferred audio format (MP3, WAV, or Opus)
- Click "🎵 Generate Voice" to create your audio
- The audio will automatically play when ready

### Step 5: Download & Share
- Use the built-in audio player controls
- Download the generated audio file
- Share or use the audio in your projects

## 🔧 API Endpoints

The application provides several REST endpoints for advanced usage:

### Audio Generation
- `POST /tts` - Generate speech from form data
- `POST /api/quick-tts` - JSON API for programmatic access

### Audio Retrieval
- `GET /audio/{id}` - Stream generated audio file
- `GET /audio/{id}?download=true` - Download audio file

### Vibe Management
- `GET /api/vibes` - Get random vibes (default: 6)
- `GET /api/vibes?count=12` - Get specific number of vibes
- `GET /api/vibe/{name}` - Get specific vibe details

### Health Check
- `GET /health` - Application health and cache status

## 🏗️ Architecture

### Technology Stack
- **Backend**: Spring Boot 3.5.4 with Java 21
- **Frontend**: Thymeleaf templates with modern CSS and JavaScript
- **AI Service**: Azure OpenAI **gpt-4o-mini-tts** (2025-03-20) with GlobalStandard deployment
- **Cloud Platform**: Azure Container Apps with Container Registry
- **Authentication**: Azure Managed Identity with token-based authentication (no API keys)
- **Audio Processing**: Java HttpClient with streaming support
- **Caching**: In-memory audio store with TTL management

### Project Structure
```
ttsazure/
├── src/
│   ├── main/
│   │   ├── java/com/ttsapp/tts/
│   │   │   ├── TtsApplication.java      # Main application
│   │   │   ├── TtsController.java       # Web controller
│   │   │   ├── OpenAIService.java       # Azure OpenAI integration (with managed identity)
│   │   │   ├── AudioStore.java          # Audio caching
│   │   │   └── VibeService.java         # Vibe management
│   │   └── resources/
│   │       ├── templates/index.html     # Main UI template
│   │       ├── static/styles.css        # Modern styling
│   │       ├── vibes.json              # Vibe configurations
│   │       └── application.yml         # Spring configuration (no secrets)
├── infra/                              # Azure infrastructure (Bicep)
├── pom.xml                             # Maven dependencies
└── README.md                           # This file
```

## 🎨 Customization

### Adding New Vibes
Edit `src/main/resources/vibes.json` to add new voice styles:
```json
{
  "name": "Custom Vibe",
  "description": "Voice instructions here...",
  "script": "Sample text for this vibe..."
}
```

### Styling Changes
Modify `src/main/resources/static/styles.css` to customize:
- Color schemes and themes
- Button layouts and animations
- Responsive breakpoints
- Visual effects and transitions

### Voice Configuration
Update the voice list in `TtsController.java`:
```java
private static final List<String> AVAILABLE_VOICES = List.of(
    "alloy", "ash", "ballad", "coral", "echo", 
    "fable", "nova", "onyx", "sage", "shimmer", "verse"
);
```

## � Authentication & Security

### Managed Identity Authentication
This application uses **Azure Managed Identity** for secure, keyless authentication:

- **No API Keys**: Zero secrets stored in code, configuration, or environment variables
- **Automatic Token Management**: Azure handles token generation, rotation, and renewal
- **DefaultAzureCredential**: Uses the standard Azure identity library for seamless authentication
- **Bearer Token Authentication**: Each API call obtains a fresh token for `https://cognitiveservices.azure.com`
- **RBAC Integration**: Managed identity has minimal required permissions (Cognitive Services OpenAI User)

### How It Works
1. **Container Apps assigns** a managed identity to the application
2. **Application requests** an access token using `DefaultAzureCredential`
3. **Azure AD issues** a bearer token for the Cognitive Services scope
4. **API calls use** the token in `Authorization: Bearer <token>` headers
5. **Tokens auto-refresh** when they expire (no manual intervention needed)

### Security Benefits
- 🔒 **Zero Secret Management**: No keys to rotate, store, or secure
- 🔄 **Automatic Rotation**: Tokens refresh automatically every hour
- 🎯 **Least Privilege**: Identity has only the minimum required permissions
- 📊 **Audit Trail**: All authentication events logged in Azure AD
- 🛡️ **No Network Exposure**: Tokens never leave the Azure security boundary

## �🔒 Additional Security

## 🔒 Additional Security

- **Managed Identity Authentication**: Uses Azure DefaultAzureCredential with automatic token rotation
- **No API Keys**: Zero secrets stored in configuration or environment variables
- **Token-Based Auth**: Bearer tokens obtained dynamically for each API call
- **RBAC Permissions**: Minimal required permissions with Cognitive Services OpenAI User role
- **Input Validation**: Text length and format validation
- **Error Handling**: Graceful error messages without exposing internals
- **CORS Support**: Configurable for different deployment environments

## 🚀 Deployment

### Azure Deployment (Recommended)
Use Azure Developer CLI for one-command deployment:

```bash
# Deploy everything to Azure
azd up
```

This single command handles:
- 🏗️ **Infrastructure Creation**: Azure OpenAI, Container Registry, Container Apps
- 🤖 **AI Model Deployment**: gpt-4o-mini with TTS support (2025-03-20)
- 🔒 **Security Setup**: Managed Identity, RBAC roles, token-based authentication
- 📦 **Application Build**: Containerizes and deploys your Spring Boot app
- 🌐 **Network Configuration**: Public endpoints with CORS and health checks

### Environment Management
```bash
# List environments
azd env list

# Switch environments
azd env select <environment-name>

# View environment variables
azd env get-values

# Clean up resources
azd down
```

### Local Development
For development and testing:
```bash
# Ensure Java 21+ is installed
java --version

# Run locally (connects to Azure resources using your Azure CLI login)
az login  # Login to Azure first
mvn spring-boot:run
```

**Note**: Local development uses your Azure CLI credentials through `DefaultAzureCredential`. No API keys needed!

### Production Updates
```bash
# Deploy code changes
azd deploy

# Update infrastructure
azd provision

# Full redeploy
azd up
```

## 📊 Monitoring

### Health Checks
- Application health: `GET /health`
- Cache statistics included in health endpoint
- Spring Boot Actuator endpoints available

### Logging
- Structured logging with configurable levels
- Request/response tracking
- Audio generation metrics
- Error tracking and reporting

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Make your changes and test thoroughly
4. Commit with clear messages: `git commit -m "Add feature description"`
5. Push to your fork: `git push origin feature-name`
6. Create a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

### Common Issues

**First Time Setup**
```bash
# Install Azure Developer CLI
# Windows: winget install microsoft.azd
# macOS: brew install azure-dev
# Linux: curl -fsSL https://aka.ms/install-azd.sh | bash

# Then deploy
azd up
```

**Authentication Issues**
```bash
# Login to Azure
azd auth login

# Check current subscription
azd config get defaults.subscription

# Set specific subscription if needed
azd config set defaults.subscription <subscription-id>

# For local development, ensure Azure CLI is logged in
az login
az account show  # Verify correct subscription
```

**Managed Identity Issues**
- Verify the managed identity has "Cognitive Services OpenAI User" role
- Check that the Container App is using the correct managed identity
- Ensure the Azure OpenAI resource is in the same subscription
- Use `azd monitor` to check authentication logs

**Deployment Failures**
- Ensure you have Contributor role on the Azure subscription
- Check that your subscription has Azure OpenAI service enabled
- Verify you're deploying to East US or East US 2 (supported regions)
- Use `azd monitor` to check deployment logs

**Model Availability**
- gpt-4o-mini with TTS is only available in East US and East US 2
- The infrastructure automatically selects the correct region
- No manual model deployment needed - azd handles everything

**Resource Cleanup**
```bash
# Remove all Azure resources
azd down --purge

# Keep data but stop services
azd down
```

### Getting Help
- Open an issue on GitHub for bugs
- Check existing issues for solutions  
- Use `azd monitor` to view application logs
- Review Azure OpenAI documentation for API limits
- Check deployment status with `azd env get-values`
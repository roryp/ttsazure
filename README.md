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
- Java 21 or higher
- Maven 3.6+
- Azure OpenAI account with **gpt-4o-mini-tts** model deployment
- Azure subscription with access to East US 2 or Sweden Central regions

### 1. Clone the Repository
```bash
git clone https://github.com/roryp/ttsazure.git
cd ttsazure
```

### 2. Configure Environment Variables
Update the `.env` file with your Azure OpenAI credentials:
```env
AZURE_OPENAI_ENDPOINT=https://your-resource.cognitiveservices.azure.com
AZURE_OPENAI_DEPLOYMENT=gpt-4o-mini-tts
AZURE_OPENAI_MODEL=gpt-4o-mini-tts
AZURE_OPENAI_MODEL_VERSION=2025-03-20
AZURE_CLIENT_ID=your-managed-identity-client-id
AZURE_TENANT_ID=your-tenant-id
```

> **Note**: For Azure Container Apps deployment, the application uses Managed Identity authentication. For local development, you can use API keys.

### 3. Run the Application
```bash
# Load environment variables and start the application
source .env
mvn spring-boot:run
```

### 4. Access the Application
Open your browser and navigate to:
```
http://localhost:8080
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
- **Authentication**: Azure Managed Identity for secure cloud access
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
│   │   │   ├── OpenAIService.java       # Azure OpenAI integration
│   │   │   ├── AudioStore.java          # Audio caching
│   │   │   └── VibeService.java         # Vibe management
│   │   └── resources/
│   │       ├── templates/index.html     # Main UI template
│   │       ├── static/styles.css        # Modern styling
│   │       ├── vibes.json              # Vibe configurations
│   │       └── application.yml         # Spring configuration
├── .env                                 # Environment variables
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

## 🔒 Security

- **Environment Variables**: Sensitive data stored in `.env` (gitignored)
- **Input Validation**: Text length and format validation
- **Error Handling**: Graceful error messages without exposing internals
- **CORS Support**: Configurable for different deployment environments

## 🚀 Deployment

### Local Development
```bash
source .env
mvn spring-boot:run
```

### Azure Container Apps (Recommended)
Use Azure Developer CLI (azd) for seamless cloud deployment:

```bash
# Initialize and deploy
azd init
azd up
```

This will:
- Deploy infrastructure using Bicep templates
- Create Azure OpenAI resource with gpt-4o-mini-tts model
- Set up Container Apps environment with proper networking
- Configure Managed Identity for secure authentication
- Deploy the application container

### Manual Production Build
```bash
mvn clean package
java -jar target/tts-0.0.1-SNAPSHOT.jar
```

### Docker Deployment
```bash
docker build -t tts-app .
docker run -p 8080:8080 --env-file .env tts-app
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

**Environment Variables Not Found**
```bash
# Make sure to source the .env file
source .env
mvn spring-boot:run
```

**Audio Not Playing**
- Check browser autoplay settings
- Ensure audio format is supported
- Verify Azure OpenAI API credentials
- Confirm gpt-4o-mini-tts model is deployed correctly

**Build Failures**
- Ensure Java 21+ is installed
- Verify Maven dependencies are resolved
- Check internet connectivity for Azure services

**Deployment Issues**
- Ensure you're deploying to East US 2 or Sweden Central (supported regions)
- Verify Azure subscription has access to OpenAI services
- Check that Managed Identity has proper role assignments

**Model Not Available**
- gpt-4o-mini-tts is only available in East US 2 and Sweden Central
- Ensure your Azure OpenAI resource is in a supported region
- Verify the model version (2025-03-20) is correctly specified

### Getting Help
- Open an issue on GitHub for bugs
- Check existing issues for solutions
- Review Azure OpenAI documentation for API limits
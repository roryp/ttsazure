<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

# Azure OpenAI TTS Application - Project Setup Complete ✅

This workspace contains a complete Azure OpenAI Text-to-Speech application built with:

## ✅ Completed Components

- [x] **Infrastructure as Code** - Complete Bicep templates for Container Apps deployment
- [x] **Spring Boot Application** - Full TTS web app with Thymeleaf UI  
- [x] **Azure OpenAI Integration** - Managed Identity authentication with GPT-4o Mini TTS model
- [x] **Container Support** - Multi-stage Dockerfile for production deployment
- [x] **CI/CD Pipeline** - GitHub Actions workflow for automated deployment
- [x] **VS Code Tasks** - Build and run tasks configured
- [x] **Documentation** - Comprehensive README with setup instructions

## 🚀 Next Steps

1. **Deploy to Azure**: Run `azd up` to provision resources and deploy
2. **Local Development**: Use `mvn spring-boot:run` in the `src` directory
3. **GitHub Actions**: Configure secrets for automated deployment

## 📁 Project Structure

```
tts-ai-app/
├── infra/                  # Bicep infrastructure templates
├── src/                    # Spring Boot application source
├── .github/workflows/      # CI/CD pipeline
├── azure.yaml             # Azure Developer CLI configuration
└── README.md              # Complete setup instructions
```

The project is ready for deployment and development!

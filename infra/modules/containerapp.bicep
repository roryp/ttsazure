param name string
param location string = resourceGroup().location
param tags object = {}

param containerAppsEnvironmentName string
param containerRegistryName string
param exists bool = false
param openaiEndpoint string
param openaiAccountName string

param containerCpuCoreCount string = '0.5'
param containerMemory string = '1.0Gi'
param containerMinReplicas int = 1
param containerMaxReplicas int = 10

resource containerAppsEnvironment 'Microsoft.App/managedEnvironments@2022-10-01' existing = {
  name: containerAppsEnvironmentName
}

resource containerRegistry 'Microsoft.ContainerRegistry/registries@2021-09-01' existing = {
  name: containerRegistryName
}

resource openaiAccount 'Microsoft.CognitiveServices/accounts@2023-05-01' existing = {
  name: openaiAccountName
}

// Grant the Container App managed identity AcrPull role on the container registry
var acrPullDefinitionId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '7f951dda-4ed3-4680-a7ca-43fe172d538d')
resource containerAppAcrPullRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(containerApp.id, containerRegistry.id, acrPullDefinitionId)
  scope: containerRegistry
  properties: {
    principalId: containerApp.identity.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: acrPullDefinitionId
  }
}

// Grant the Container App managed identity Cognitive Services User role on the OpenAI account
var cognitiveServicesUserDefinitionId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'a97b65f3-24c7-4388-baec-2e87135dc908')
resource containerAppOpenAIRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(containerApp.id, openaiAccount.id, cognitiveServicesUserDefinitionId)
  scope: openaiAccount
  properties: {
    principalId: containerApp.identity.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: cognitiveServicesUserDefinitionId
  }
}

resource containerApp 'Microsoft.App/containerApps@2022-10-01' = {
  name: name
  location: location
  tags: union(tags, {'azd-service-name': 'web'})
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    managedEnvironmentId: containerAppsEnvironment.id
    configuration: {
      ingress: {
        external: true
        targetPort: 8080
        transport: 'http'
        allowInsecure: false
      }
      registries: [
        {
          server: containerRegistry.properties.loginServer
          identity: 'system'
        }
      ]
    }
    template: {
      containers: [
        {
          image: exists ? 'mcr.microsoft.com/azuredocs/containerapps-helloworld:latest' : '${containerRegistry.properties.loginServer}/tts-ai-app:latest'
          name: 'main'
          env: [
            {
              name: 'AZURE_OPENAI_ENDPOINT'
              value: openaiEndpoint
            }
            {
              name: 'AZURE_OPENAI_DEPLOYMENT'
              value: 'gpt-4o-mini-tts'
            }
            {
              name: 'AZURE_OPENAI_MODEL'
              value: 'gpt-4o-mini-tts'
            }
          ]
          resources: {
            cpu: json(containerCpuCoreCount)
            memory: containerMemory
          }
        }
      ]
      scale: {
        minReplicas: containerMinReplicas
        maxReplicas: containerMaxReplicas
      }
    }
  }
}

output defaultDomain string = containerAppsEnvironment.properties.defaultDomain
output identityPrincipalId string = containerApp.identity.principalId
output name string = containerApp.name
output uri string = 'https://${containerApp.properties.configuration.ingress.fqdn}'

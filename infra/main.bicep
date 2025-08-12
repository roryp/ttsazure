targetScope = 'resourceGroup'

@description('Primary location for all resources')
param location string = 'eastus2'

@description('Name of the environment')
param environmentName string

@maxLength(64)
@description('Name of the Container Apps environment')
param containerAppsEnvironmentName string = ''

@description('Name of the container app')
param containerAppName string = ''

@description('Name of the container registry')
param containerRegistryName string = ''

@description('Name of the managed identity')
param managedIdentityName string = ''

@description('Container image to deploy')
param containerImage string = ''

@description('Name of the Azure OpenAI resource')
param openAiAccountName string = ''

@description('SKU for Azure OpenAI resource')
param openAiSku string = 'S0'

@description('Name of the OpenAI deployment')
param openAiDeploymentName string = 'gpt-4o-mini-tts'

@description('OpenAI model to deploy')
param openAiModelName string = 'gpt-4o-mini-tts'

@description('OpenAI model version with TTS support')
param openAiModelVersion string = '2025-03-20'

@description('OpenAI model capacity (TPM in thousands)')
param openAiModelCapacity int = 10

// Generate a unique resource token
var resourceToken = toLower(uniqueString(subscription().id, resourceGroup().id, location, environmentName))

// Create resource names with resource token
var actualContainerAppsEnvironmentName = !empty(containerAppsEnvironmentName) ? containerAppsEnvironmentName : 'cae-${resourceToken}'
var actualContainerAppName = !empty(containerAppName) ? containerAppName : 'ca-${resourceToken}'
var resourceTokenShort = take(replace(resourceToken, '-', ''), 10)
var actualContainerRegistryName = !empty(containerRegistryName) ? containerRegistryName : toLower('cr${resourceTokenShort}')
var actualManagedIdentityName = !empty(managedIdentityName) ? managedIdentityName : 'mi-${resourceToken}'
var actualOpenAiAccountName = !empty(openAiAccountName) ? openAiAccountName : 'openai-${resourceToken}'

// Create resource group tags
var tags = {
  'azd-env-name': environmentName
}

// Create managed identity
resource managedIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: actualManagedIdentityName
  location: location
  tags: tags
}

// Create Azure OpenAI resource
resource openAiAccount 'Microsoft.CognitiveServices/accounts@2024-10-01' = {
  name: actualOpenAiAccountName
  location: location
  tags: tags
  kind: 'OpenAI'
  sku: {
    name: openAiSku
  }
  properties: {
    customSubDomainName: actualOpenAiAccountName
    publicNetworkAccess: 'Enabled'
    networkAcls: {
      defaultAction: 'Allow'
    }
  }
}

// Deploy the OpenAI model with TTS support
resource openAiDeployment 'Microsoft.CognitiveServices/accounts/deployments@2024-10-01' = {
  parent: openAiAccount
  name: openAiDeploymentName
  sku: {
    name: 'GlobalStandard'
    capacity: openAiModelCapacity
  }
  properties: {
    model: {
      format: 'OpenAI'
      name: openAiModelName
      version: openAiModelVersion
    }
    versionUpgradeOption: 'OnceCurrentVersionExpired'
    raiPolicyName: 'Microsoft.DefaultV2'
  }
}

// Create Container Registry
resource containerRegistry 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: actualContainerRegistryName
  location: location
  tags: tags
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: false
  }
}

// Grant the managed identity AcrPull role on the container registry
resource acrPullRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: containerRegistry
  name: guid(containerRegistry.id, managedIdentity.id, 'b24988ac-6180-42a0-ab88-20f7382dd24c')
  properties: {
    roleDefinitionId: resourceId('Microsoft.Authorization/roleDefinitions', 'b24988ac-6180-42a0-ab88-20f7382dd24c') // AcrPull
    principalType: 'ServicePrincipal'
    principalId: managedIdentity.properties.principalId
  }
}

// Grant the managed identity AcrPush role on the container registry for deployment
resource acrPushRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: containerRegistry
  name: guid(containerRegistry.id, managedIdentity.id, '8311e382-0749-4cb8-b61a-304f252e45ec')
  properties: {
    roleDefinitionId: resourceId('Microsoft.Authorization/roleDefinitions', '8311e382-0749-4cb8-b61a-304f252e45ec') // AcrPush
    principalType: 'ServicePrincipal'
    principalId: managedIdentity.properties.principalId
  }
}

// Grant the managed identity Cognitive Services OpenAI User role on the Azure OpenAI resource
resource openAiRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: openAiAccount
  name: guid(openAiAccount.id, managedIdentity.id, '5e0bd9bd-7b93-4f28-af87-19fc36ad61bd')
  properties: {
    roleDefinitionId: resourceId('Microsoft.Authorization/roleDefinitions', '5e0bd9bd-7b93-4f28-af87-19fc36ad61bd') // Cognitive Services OpenAI User
    principalType: 'ServicePrincipal'
    principalId: managedIdentity.properties.principalId
  }
}

// Create Container Apps environment
resource containerAppsEnvironment 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: actualContainerAppsEnvironmentName
  location: location
  tags: tags
  properties: {
    workloadProfiles: [
      {
        name: 'Consumption'
        workloadProfileType: 'Consumption'
      }
    ]
  }
}

// Create Container App
resource containerApp 'Microsoft.App/containerApps@2024-03-01' = {
  name: actualContainerAppName
  location: location
  tags: union(tags, {
    'azd-service-name': 'api'
  })
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${managedIdentity.id}': {}
    }
  }
  dependsOn: [
    acrPullRoleAssignment
    acrPushRoleAssignment
    openAiRoleAssignment
    openAiDeployment
  ]
  properties: {
    managedEnvironmentId: containerAppsEnvironment.id
    workloadProfileName: 'Consumption'
    configuration: {
      ingress: {
        external: true
        targetPort: 8080
        allowInsecure: false
        corsPolicy: {
          allowedOrigins: ['https://*.azurecontainerapps.io', 'http://localhost:3000', 'http://localhost:8080']
          allowedMethods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS']
          allowedHeaders: ['Content-Type', 'Authorization', 'Accept']
          exposeHeaders: ['Content-Type']
          maxAge: 86400
          allowCredentials: true
        }
      }
      registries: [
        {
          server: containerRegistry.properties.loginServer
          identity: managedIdentity.id
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'ttsazure'
          image: !empty(containerImage) ? containerImage : 'mcr.microsoft.com/azuredocs/containerapps-helloworld:latest'
          env: [
            {
              name: 'AZURE_OPENAI_ENDPOINT'
              value: openAiAccount.properties.endpoint
            }
            {
              name: 'AZURE_OPENAI_DEPLOYMENT'
              value: openAiDeploymentName
            }
            {
              name: 'AZURE_OPENAI_MODEL'
              value: openAiModelName
            }
            {
              name: 'AZURE_OPENAI_MODEL_VERSION'
              value: openAiModelVersion
            }
            {
              name: 'AZURE_CLIENT_ID'
              value: managedIdentity.properties.clientId
            }
            {
              name: 'AZURE_TENANT_ID'
              value: tenant().tenantId
            }
            {
              name: 'PORT'
              value: '8080'
            }
          ]
          resources: {
            cpu: json('1.0')
            memory: '2Gi'
          }
          probes: [
            {
              type: 'Liveness'
              httpGet: {
                path: '/health'
                port: 8080
              }
              initialDelaySeconds: 30
              periodSeconds: 30
              timeoutSeconds: 10
              failureThreshold: 3
            }
            {
              type: 'Readiness'
              httpGet: {
                path: '/health'
                port: 8080
              }
              initialDelaySeconds: 10
              periodSeconds: 5
              timeoutSeconds: 5
              failureThreshold: 3
            }
          ]
        }
      ]
      scale: {
        minReplicas: 0
        maxReplicas: 3
        rules: [
          {
            name: 'http-scaling'
            http: {
              metadata: {
                concurrentRequests: '10'
              }
            }
          }
        ]
      }
    }
  }
}

// Output the application URL and other important values
output AZURE_CONTAINER_REGISTRY_ENDPOINT string = containerRegistry.properties.loginServer
output AZURE_CONTAINER_REGISTRY_NAME string = containerRegistry.name
output SERVICE_API_IDENTITY_PRINCIPAL_ID string = managedIdentity.properties.principalId
output SERVICE_API_NAME string = containerApp.name
output SERVICE_API_URI string = 'https://${containerApp.properties.configuration.ingress.fqdn}'
output AZURE_CONTAINER_APPS_ENVIRONMENT_NAME string = containerAppsEnvironment.name
output AZURE_LOCATION string = location
output AZURE_TENANT_ID string = tenant().tenantId
output RESOURCE_GROUP_ID string = resourceGroup().id
output RESOURCE_GROUP_NAME string = resourceGroup().name
output AZURE_RESOURCE_TOKEN string = resourceToken
output AZURE_OPENAI_ENDPOINT string = openAiAccount.properties.endpoint
output AZURE_OPENAI_ACCOUNT_NAME string = openAiAccount.name
output AZURE_OPENAI_DEPLOYMENT_NAME string = openAiDeploymentName
output AZURE_OPENAI_MODEL_VERSION string = openAiModelVersion

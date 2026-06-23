<script setup>
import { ref, reactive, computed, onMounted } from 'vue'

// Global state
const connectionMode = ref('simulation') // 'simulation' or 'api'
const backendUrl = ref('http://localhost:8080')
const logs = ref([])
const activeTask = ref(null) // Holds the current task waiting for HITL

// Project context state
const project = reactive({
  id: '',
  name: 'E-Commerce Core API',
  description: 'Un sistema de backend hexagonal robusto con Spring Boot, seguridad por tokens, PostgreSQL local y despliegue automatizado.',
  repositoryUrl: 'git@github.com:tino/ecommerce-core-api.git',
  currentPhase: 'ANALYSIS', // ANALYSIS, ARCHITECTURE, DEVELOPMENT, TESTING, DEPLOYMENT, COMPLETED
  status: 'IDLE', // IDLE, RUNNING, PAUSED, WAITING_FOR_HITL, FAILED, COMPLETED
  phaseOutputs: {
    ANALYSIS: '',
    ARCHITECTURE: '',
    DEVELOPMENT: '',
    TESTING: '',
    DEPLOYMENT: ''
  }
})

// Subagents status
const agents = reactive([
  {
    role: 'PRODUCT_OWNER',
    name: 'PO Analyst Agent',
    description: 'Traduce requisitos informales a especificaciones en formato Gherkin.',
    status: 'IDLE', // IDLE, IN_PROGRESS, WAITING_FOR_HITL, COMPLETED, REJECTED
    avatar: '📋',
    output: ''
  },
  {
    role: 'ARCHITECT',
    name: 'Technical Architect Agent',
    description: 'Diseña Bounded Contexts, esquemas SQL y contratos OpenAPI.',
    status: 'IDLE',
    avatar: '📐',
    output: ''
  },
  {
    role: 'BACKEND',
    name: 'Spring Boot Backend Agent',
    description: 'Escribe código hexagonal limpio en Java 21 y controladores REST.',
    status: 'IDLE',
    avatar: '☕',
    output: ''
  },
  {
    role: 'QA',
    name: 'Adversarial QA Agent',
    description: 'Implementa y ejecuta tests unitarios y de integración buscando fallas.',
    status: 'IDLE',
    avatar: '🛡️',
    output: ''
  },
  {
    role: 'SRE',
    name: 'DevOps / SRE Agent',
    description: 'Genera configuraciones Docker, despliegue y aprovisionamiento VPS.',
    status: 'IDLE',
    avatar: '🚀',
    output: ''
  }
])

// Stepper navigation structure
const phases = [
  { key: 'ANALYSIS', name: 'Requisitos', icon: '📋' },
  { key: 'ARCHITECTURE', name: 'Arquitectura', icon: '📐' },
  { key: 'DEVELOPMENT', name: 'Backend', icon: '☕' },
  { key: 'TESTING', name: 'QA Tests', icon: '🛡️' },
  { key: 'DEPLOYMENT', name: 'Despliegue', icon: '🚀' },
  { key: 'COMPLETED', name: 'Finalizado', icon: '✅' }
]

// Feedback field for rejections
const reviewerFeedback = ref('')

// Tabs and Auditor state
const activeTab = ref('hitl')
const selectedAgentForAudit = ref(null)
const selectedAgentTasks = ref([])
const projectsList = ref([])

// Collapsible panels state
const isLeftCollapsed = ref(false)
const isCenterCollapsed = ref(false)

const isRightMaximized = computed(() => isLeftCollapsed.value && isCenterCollapsed.value)

const toggleRightMaximize = () => {
  if (isRightMaximized.value) {
    isLeftCollapsed.value = false
    isCenterCollapsed.value = false
  } else {
    isLeftCollapsed.value = true
    isCenterCollapsed.value = true
  }
}

// Utility functions
const addLog = (message, type = 'info') => {
  const time = new Date().toLocaleTimeString()
  logs.value.unshift({ time, message, type })
}

const updateAgentStatus = (role, status, output = '') => {
  const agent = agents.find(a => a.role === role)
  if (agent) {
    agent.status = status
    if (output) agent.output = output
  }
}

// Reset pipeline
const resetPipeline = () => {
  project.currentPhase = 'ANALYSIS'
  project.phaseOutputs = { ANALYSIS: '', ARCHITECTURE: '', DEVELOPMENT: '', TESTING: '', DEPLOYMENT: '' }
  agents.forEach(a => {
    a.status = 'IDLE'
    a.output = ''
  })
  activeTask.value = null
  selectedAgentForAudit.value = null
  selectedAgentTasks.value = []
  activeTab.value = 'hitl'
  logs.value = []
  addLog('Sistema agéntico reiniciado. Listo para iniciar nueva iteración.', 'info')
}

// Helper to resolve phase for a given role
const getPhaseForRole = (role) => {
  switch (role) {
    case 'PRODUCT_OWNER': return 'ANALYSIS'
    case 'ARCHITECT': return 'ARCHITECTURE'
    case 'BACKEND': return 'DEVELOPMENT'
    case 'QA': return 'TESTING'
    case 'SRE': return 'DEPLOYMENT'
    default: return 'COMPLETED'
  }
}

// Helper to check if a phase requires human review
const requiresHitlPhase = (phase) => {
  return phase === 'ARCHITECTURE' || phase === 'DEPLOYMENT'
}

// Update agent statuses based on latest project context
const updateAgentStatusesFromContext = (context) => {
  const phasesOrder = ['ANALYSIS', 'ARCHITECTURE', 'DEVELOPMENT', 'TESTING', 'DEPLOYMENT', 'COMPLETED']
  const currentIndex = phasesOrder.indexOf(context.currentPhase)
  
  agents.forEach(agent => {
    const agentPhase = getPhaseForRole(agent.role)
    const agentIndex = phasesOrder.indexOf(agentPhase)
    
    if (agentIndex < currentIndex) {
      agent.status = 'COMPLETED'
      agent.output = context.phaseOutputs[agentPhase] || ''
    } else if (agentIndex === currentIndex) {
      if (requiresHitlPhase(agentPhase)) {
        agent.status = 'WAITING_FOR_HITL'
        agent.output = context.phaseOutputs[agentPhase] || ''
      } else {
        agent.status = 'IN_PROGRESS'
        agent.output = ''
      }
    } else {
      agent.status = 'IDLE'
      agent.output = ''
    }
  })
}

// Load active tasks from the backend for HITL review
const loadActiveTask = async (context) => {
  try {
    const res = await fetch(`${backendUrl.value}/api/v1/projects/${context.id}/tasks`)
    if (res.ok) {
      const allTasks = await res.json()
      const pendingTask = allTasks.find(t => t.status === 'WAITING_FOR_HITL')
      if (pendingTask) {
        activeTask.value = {
          id: pendingTask.id,
          title: pendingTask.title,
          description: pendingTask.description,
          outputData: pendingTask.outputData,
          role: pendingTask.agentRole
        }
        activeTab.value = 'hitl'
        
        const agent = agents.find(a => a.role === pendingTask.agentRole)
        if (agent) {
          agent.status = 'WAITING_FOR_HITL'
          agent.output = pendingTask.outputData
        }
      } else {
        activeTask.value = null
      }
    }
  } catch (err) {
    console.error("Error loading active task", err)
  }
}

// Polling variables and helpers for async updates
let pollingInterval = null

const startPolling = () => {
  if (pollingInterval) return
  pollingInterval = setInterval(async () => {
    // Si cambiamos de modo o no hay proyecto, parar
    if (connectionMode.value !== 'api' || !project.id) {
      stopPolling()
      return
    }
    await refreshProjectState()
  }, 2500)
}

const stopPolling = () => {
  if (pollingInterval) {
    clearInterval(pollingInterval)
    pollingInterval = null
  }
}

// Refresh project state and sync with the backend
const refreshProjectState = async () => {
  if (connectionMode.value !== 'api' || !project.id) return
  
  try {
    const res = await fetch(`${backendUrl.value}/api/v1/projects/${project.id}`)
    if (res.ok) {
      const context = await res.json()
      
      project.currentPhase = context.currentPhase
      project.status = context.status || 'IDLE'
      project.phaseOutputs.ANALYSIS = context.phaseOutputs.ANALYSIS || ''
      project.phaseOutputs.ARCHITECTURE = context.phaseOutputs.ARCHITECTURE || ''
      project.phaseOutputs.DEVELOPMENT = context.phaseOutputs.DEVELOPMENT || ''
      project.phaseOutputs.TESTING = context.phaseOutputs.TESTING || ''
      project.phaseOutputs.DEPLOYMENT = context.phaseOutputs.DEPLOYMENT || ''
      
      updateAgentStatusesFromContext(context)
      await loadActiveTask(context)

      // Iniciar o detener polling en base al estado del proyecto
      if (project.status === 'RUNNING') {
        startPolling()
      } else {
        stopPolling()
      }
    }
  } catch (err) {
    addLog(`Error al sincronizar con el backend: ${err.message}`, 'error')
  }
}

// Fetch tasks for audit logs inspection of a selected agent
const auditAgent = async (agent) => {
  selectedAgentForAudit.value = agent
  selectedAgentTasks.value = []
  activeTab.value = 'audit'
  
  if (connectionMode.value === 'api' && project.id) {
    try {
      const res = await fetch(`${backendUrl.value}/api/v1/projects/${project.id}/tasks`)
      if (res.ok) {
        const allTasks = await res.json()
        selectedAgentTasks.value = allTasks.filter(t => t.agentRole === agent.role)
      }
    } catch (err) {
      addLog(`Error al cargar historial de tareas de ${agent.name}: ${err.message}`, 'error')
    }
  } else {
    // Simulation fallback
    if (agent.output || agent.status === 'IN_PROGRESS') {
      selectedAgentTasks.value = [
        {
          id: 'sim-task-' + agent.role,
          agentRole: agent.role,
          title: `Generar salida para la fase ${getPhaseForRole(agent.role)}`,
          description: `Procesamiento simulado en entorno local.`,
          status: agent.status,
          inputData: 'Especificaciones de requisitos e inputs anteriores.',
          outputData: agent.output || 'Procesando en segundo plano...'
        }
      ]
    }
  }
}

// Unified trigger entrypoint
const triggerPipeline = async () => {
  if (connectionMode.value === 'simulation') {
    await startSimulationPipeline()
  } else {
    await startApiPipeline()
  }
}

// API Real Pipeline execution
const startApiPipeline = async () => {
  resetPipeline()
  addLog('Iniciando pipeline real contra el API...', 'primary')
  updateAgentStatus('PRODUCT_OWNER', 'IN_PROGRESS')
  project.currentPhase = 'ANALYSIS'

  try {
    const res = await fetch(`${backendUrl.value}/api/v1/projects/orchestrate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: project.name,
        description: project.description,
        repositoryUrl: project.repositoryUrl
      })
    })

    if (!res.ok) {
      throw new Error(`Servidor retornó código ${res.status}`)
    }

    const context = await res.json()
    project.id = context.id
    addLog('Pipeline procesado por la factoría del backend.', 'success')

    await loadProjectsList()
    await refreshProjectState()

  } catch (err) {
    addLog(`Error al conectar con la API: ${err.message}`, 'error')
    updateAgentStatus('PRODUCT_OWNER', 'IDLE')
  }
}

// Main execution triggers (Simulation Mode)
const startSimulationPipeline = async () => {
  resetPipeline()
  addLog('Iniciando pipeline agéntico SDLC...', 'primary')
  
  // 1. PRODUCT OWNER PHASE
  project.currentPhase = 'ANALYSIS'
  updateAgentStatus('PRODUCT_OWNER', 'IN_PROGRESS')
  addLog('Agente Product Owner iniciado. Analizando descripción del proyecto...', 'info')
  
  await delay(2500)
  
  const gherkinStories = `Feature: E-Commerce Product Registration
  As a Store Administrator
  I want to register new products in the catalog
  So that customers can see and buy them
  
  Scenario: Product successfully registered
    Given the administrator has a valid product request
    When they submit POST /api/v1/products with a name and price
    Then the system should save the product and return status 201`
  
  project.phaseOutputs.ANALYSIS = gherkinStories
  updateAgentStatus('PRODUCT_OWNER', 'COMPLETED', gherkinStories)
  addLog('Agente Product Owner ha finalizado. Historias Gherkin generadas.', 'success')
  
  // 2. ARCHITECTURE PHASE
  project.currentPhase = 'ARCHITECTURE'
  updateAgentStatus('ARCHITECT', 'IN_PROGRESS')
  addLog('Agente Arquitecto Técnico iniciado. Diseñando contratos y base de datos...', 'info')
  
  await delay(3000)
  
  const archSpec = `### Especificación Técnica de Arquitectura
  - **Bounded Context**: ProductCatalog
  - **Entidad de Dominio**: Product(UUID id, String name, Double price)
  - **Base de Datos**: PostgreSQL schema 'product_contexts'
  - **Contrato OpenAPI**:
    \`\`\`yaml
    openapi: 3.1.0
    paths:
      /api/v1/products:
        post:
          summary: Registrar Producto
          requestBody:
            content:
              application/json:
                schema:
                  properties:
                    name: { type: string }
                    price: { type: number }
    \`\`\``
  
  project.phaseOutputs.ARCHITECTURE = archSpec
  updateAgentStatus('ARCHITECT', 'WAITING_FOR_HITL', archSpec)
  activeTask.value = {
    id: 'a8b792e0-25c1-4560-8b10-6819b56f7ef0',
    title: 'Aprobar Arquitectura Base',
    description: 'Por favor valida el esquema de base de datos y contrato OpenAPI generado por el Arquitecto Técnico.',
    outputData: archSpec,
    role: 'ARCHITECT'
  }
  addLog('Agente Arquitecto completó el borrador. Esperando aprobación humana (HITL)...', 'warning')
}

// Simulated delay helper
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))

// Approve task handler (HITL)
const approveTask = async () => {
  if (!activeTask.value) return
  
  const role = activeTask.value.role
  addLog(`Tarea del agente ${role} APROBADA por el supervisor.`, 'success')
  
  if (connectionMode.value === 'api') {
    try {
      // Intentar enviar aprobación real
      const response = await fetch(`${backendUrl.value}/api/v1/tasks/${activeTask.value.id}/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ approved: true, feedback: '' })
      })
      if (response.ok) {
        addLog('Aprobación sincronizada con el backend Spring Boot.', 'success')
        await refreshProjectState()
      } else {
        addLog('Fallo al sincronizar aprobación con el servidor backend.', 'error')
      }
    } catch (err) {
      addLog(`Error de red con backend: ${err.message}`, 'error')
    }
  } else {
    // Modo simulación
    updateAgentStatus(role, 'COMPLETED')
    activeTask.value = null
    
    if (role === 'ARCHITECT') {
      runDevelopmentPhase()
    } else if (role === 'SRE') {
      project.currentPhase = 'COMPLETED'
      addLog('¡Ciclo SDLC Multi-Agente finalizado con éxito!', 'success')
    }
  }
}

// Reject task handler (HITL)
const rejectTask = async () => {
  if (!activeTask.value) return
  if (!reviewerFeedback.value.trim()) {
    alert('Por favor provee comentarios (feedback) explicando por qué rechazas la entrega.')
    return
  }
  
  const role = activeTask.value.role
  addLog(`Tarea del agente ${role} RECHAZADA. Motivo: ${reviewerFeedback.value}`, 'error')
  
  if (connectionMode.value === 'api') {
    try {
      const response = await fetch(`${backendUrl.value}/api/v1/tasks/${activeTask.value.id}/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ approved: false, feedback: reviewerFeedback.value })
      })
      if (response.ok) {
        addLog('Rechazo y comentarios enviados al backend.', 'info')
        reviewerFeedback.value = ''
        await refreshProjectState()
      }
    } catch (err) {
      addLog(`Error de red al enviar rechazo: ${err.message}`, 'error')
    }
  } else {
    // Modo simulación
    updateAgentStatus(role, 'REJECTED')
    activeTask.value = null
    reviewerFeedback.value = ''
  }
}

// Simulated backend + QA phases after architectural approval
const runDevelopmentPhase = async () => {
  // 3. DEVELOPMENT PHASE
  project.currentPhase = 'DEVELOPMENT'
  updateAgentStatus('BACKEND', 'IN_PROGRESS')
  addLog('Agente Backend iniciado. Escribiendo lógica de dominio e infraestructura...', 'info')
  
  await delay(3000)
  
  const codeOutput = `// ProductController.java
  @RestController
  @RequestMapping("/api/v1/products")
  @RequiredArgsConstructor
  public class ProductController {
      private final CreateProductUseCase useCase;
      
      @PostMapping
      public ResponseEntity<Product> create(@RequestBody CreateProductRequest req) {
          return ResponseEntity.status(HttpStatus.CREATED).body(useCase.execute(req));
      }
  }`
  
  project.phaseOutputs.DEVELOPMENT = codeOutput
  updateAgentStatus('BACKEND', 'COMPLETED', codeOutput)
  addLog('Agente Backend finalizó de escribir el código Java.', 'success')
  
  // 4. TESTING PHASE
  project.currentPhase = 'TESTING'
  updateAgentStatus('QA', 'IN_PROGRESS')
  addLog('Agente QA (Adversario) iniciado. Generando tests JUnit 5...', 'info')
  
  await delay(2500)
  
  const qaOutput = `// ProductControllerTest.java
  @SpringBootTest
  class ProductControllerTest {
      @MockitoBean private ProductRepository repository;
      
      @Test
      void shouldRegisterProductSuccessfully() {
          // Assertions testing REST compliance
      }
  }`
  
  project.phaseOutputs.TESTING = qaOutput
  updateAgentStatus('QA', 'COMPLETED', qaOutput)
  addLog('Agente QA finalizó. Todos los tests compilados y verificados con éxito.', 'success')
  
  // 5. DEPLOYMENT (SRE) PHASE
  project.currentPhase = 'DEPLOYMENT'
  updateAgentStatus('SRE', 'IN_PROGRESS')
  addLog('Agente DevOps iniciado. Generando Dockerfile y configurando despliegue local...', 'info')
  
  await delay(3000)
  
  const dockerfile = `FROM eclipse-temurin:21-jdk-jammy AS build
  COPY . .
  RUN ./mvnw clean install`
  
  project.phaseOutputs.DEPLOYMENT = dockerfile
  updateAgentStatus('SRE', 'WAITING_FOR_HITL', dockerfile)
  activeTask.value = {
    id: 'c9f0b123-11b9-4456-aa11-8899cc1234a1',
    title: 'Aprobar Despliegue DevOps',
    description: 'Por favor revisa el Dockerfile y configuraciones de despliegue antes de empaquetar.',
    outputData: dockerfile,
    role: 'SRE'
  }
  addLog('Agente DevOps completó la configuración. Esperando aprobación humana (HITL)...', 'warning')
}

// Load all projects from the API
const loadProjectsList = async () => {
  if (connectionMode.value !== 'api') return
  try {
    const res = await fetch(`${backendUrl.value}/api/v1/projects`)
    if (res.ok) {
      projectsList.value = await res.json()
    }
  } catch (err) {
    console.error("Error loading projects list", err)
  }
}

// Select a project and refresh its states
const selectProject = async (proj) => {
  project.id = proj.id
  project.name = proj.name
  project.description = proj.description
  project.repositoryUrl = proj.repositoryUrl
  project.status = proj.status || 'IDLE'
  
  addLog(`Proyecto cargado: ${proj.name} (Estado: ${project.status})`, 'info')
  await refreshProjectState()

  if (project.status === 'RUNNING') {
    startPolling()
  } else {
    stopPolling()
  }
}

// Save as new project in Database
const createNewProject = async () => {
  if (!project.name.trim() || !project.description.trim()) {
    alert('Por favor completa el nombre y la descripción para crear el proyecto.')
    return
  }
  
  if (connectionMode.value === 'api') {
    try {
      const res = await fetch(`${backendUrl.value}/api/v1/projects`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: project.name,
          description: project.description,
          repositoryUrl: project.repositoryUrl
        })
      })
      if (res.ok) {
        const saved = await res.json()
        addLog(`Proyecto creado exitosamente en base de datos: ${saved.name}`, 'success')
        await loadProjectsList()
        await selectProject(saved)
      } else {
        addLog('Error al guardar el nuevo proyecto en el servidor.', 'error')
      }
    } catch (err) {
      addLog(`Error al conectar con la API: ${err.message}`, 'error')
    }
  } else {
    addLog(`[Simulación] Proyecto creado en memoria: ${project.name}`, 'success')
    project.id = 'sim-proj-' + Date.now()
    project.status = 'IDLE'
  }
}

// Orchestrate pipeline using async execution
const orchestrateApiPipeline = async () => {
  if (!project.id) {
    await createNewProject()
    if (!project.id) return
  }
  
  addLog(`Arrancando orquestación asíncrona para: ${project.name}`, 'primary')
  project.status = 'RUNNING'
  
  try {
    const res = await fetch(`${backendUrl.value}/api/v1/projects/${project.id}/orchestrate`, {
      method: 'POST'
    })
    
    if (res.ok) {
      addLog('Flujo de agentes iniciado en segundo plano.', 'success')
      await refreshProjectState()
      startPolling()
    } else {
      addLog('Fallo al arrancar la orquestación en el servidor.', 'error')
      project.status = 'FAILED'
    }
  } catch (err) {
    addLog(`Error al conectar con la API para orquestar: ${err.message}`, 'error')
    project.status = 'FAILED'
  }
}

// Send pause signal to flow
const pauseApiPipeline = async () => {
  if (!project.id) return
  addLog(`Enviando señal de pausa para: ${project.name}...`, 'warning')
  
  try {
    const res = await fetch(`${backendUrl.value}/api/v1/projects/${project.id}/pause`, {
      method: 'POST'
    })
    
    if (res.ok) {
      project.status = 'PAUSED'
      addLog('Señal de pausa registrada. El pipeline se detendrá al finalizar la tarea del agente actual.', 'warning')
      stopPolling()
      await refreshProjectState()
    }
  } catch (err) {
    addLog(`Error al pausar el flujo: ${err.message}`, 'error')
  }
}

// Reset flow and outputs
const resetApiPipeline = async () => {
  if (!project.id) {
    resetPipeline()
    return
  }
  
  if (!confirm('¿Seguro que deseas reiniciar el flujo de este proyecto? Se perderán todas las salidas generadas.')) {
    return
  }
  
  addLog(`Reiniciando flujo para: ${project.name}...`, 'info')
  stopPolling()
  
  try {
    const res = await fetch(`${backendUrl.value}/api/v1/projects/${project.id}/reset`, {
      method: 'POST'
    })
    
    if (res.ok) {
      resetPipeline()
      await refreshProjectState()
      addLog('Proyecto reiniciado con éxito. Listo para iniciar de nuevo.', 'success')
    }
  } catch (err) {
    addLog(`Error al reiniciar el flujo: ${err.message}`, 'error')
  }
}

// Dropdown selector change handler
const onProjectDropdownChange = (projectId) => {
  const proj = projectsList.value.find(p => p.id === projectId)
  if (proj) {
    selectProject(proj)
  }
}

// API Connection Action Trigger
const connectToBackendApi = async () => {
  connectionMode.value = 'api'
  resetPipeline()
  addLog(`Conectándose al backend Spring Boot en ${backendUrl.value}...`, 'info')
  
  try {
    const res = await fetch(`${backendUrl.value}/actuator/health`, { mode: 'cors' })
    if (res.ok) {
      addLog('Conexión con el servidor backend establecida correctamente (Actuator OK).', 'success')
      await loadProjectsList()
      if (projectsList.value.length > 0) {
        await selectProject(projectsList.value[0])
      }
    } else {
      addLog('No se pudo verificar el estado del servidor. Asegúrate de ejecutar el backend.', 'warning')
    }
  } catch (err) {
    addLog(`El backend local no está levantado en ${backendUrl.value}. Usando modo simulación.`, 'warning')
    connectionMode.value = 'simulation'
  }
}

onMounted(async () => {
  addLog('Panel de Supervisión Agéntica inicializado. Listo para operar.', 'success')
  await connectToBackendApi()
})
</script>

<template>
  <div class="app-container">
    <!-- HEADER -->
    <header class="app-header glass-panel">
      <div class="logo-area">
        <span class="pulse-bullet active animate-spin-slow"></span>
        <div class="titles">
          <h1>AI Software Factory</h1>
          <p class="subtitle">Multi-Agent Supervisor & HITL Console</p>
        </div>
      </div>
      
      <div class="connection-control">
        <label class="toggle-mode">
          <input type="radio" v-model="connectionMode" value="simulation" @change="resetPipeline" />
          <span>Simulador</span>
        </label>
        <label class="toggle-mode">
          <input type="radio" v-model="connectionMode" value="api" @change="connectToBackendApi" />
          <span>Conectar Java API</span>
        </label>
        <select 
          v-if="connectionMode === 'api' && projectsList.length > 0" 
          :value="project.id" 
          @change="e => onProjectDropdownChange(e.target.value)"
          class="api-input project-select-dropdown"
        >
          <option value="" disabled>-- Seleccionar Proyecto --</option>
          <option v-for="proj in projectsList" :key="proj.id" :value="proj.id">
            📁 {{ proj.name }} ({{ proj.currentPhase }})
          </option>
        </select>
        <input 
          v-if="connectionMode === 'api'" 
          type="text" 
          v-model="backendUrl" 
          placeholder="http://localhost:8080"
          class="api-input" 
        />
      </div>
    </header>

    <!-- CONTENT LAYOUT -->
    <main 
      class="dashboard-grid" 
      :class="{ 
        'left-collapsed': isLeftCollapsed, 
        'center-collapsed': isCenterCollapsed,
        'both-collapsed': isLeftCollapsed && isCenterCollapsed 
      }"
    >
      
      <!-- LEFT PANEL: Project & Phase State -->
      <section 
        class="grid-panel glass-panel project-panel"
        :class="{ 'collapsed': isLeftCollapsed }"
      >
        <!-- Toggle button -->
        <button 
          class="panel-toggle-btn" 
          @click="isLeftCollapsed = !isLeftCollapsed"
          :title="isLeftCollapsed ? 'Expandir Panel' : 'Colapsar Panel'"
        >
          <span class="arrow-icon">{{ isLeftCollapsed ? '▶' : '◀' }}</span>
        </button>

        <!-- Full content -->
        <div v-show="!isLeftCollapsed" class="panel-full-content">
          <div class="panel-header">
            <span class="icon">📁</span>
            <h2>Proyecto & Estado SDLC</h2>
          </div>
          
          <div class="project-details">
            <!-- Inputs editables en modo simulación o antes de iniciar -->
            <div class="form-group">
              <label>Nombre del Proyecto:</label>
              <input type="text" v-model="project.name" class="form-control" />
            </div>
            <div class="form-group">
              <label>Descripción de Requisitos / Feature:</label>
              <textarea v-model="project.description" rows="4" class="form-control"></textarea>
            </div>
            <div class="form-group">
              <label>URL de Repositorio:</label>
              <input type="text" v-model="project.repositoryUrl" class="form-control" />
            </div>
          </div>

          <hr class="divider" />

          <!-- Vertical Stepper -->
          <div class="stepper-list">
            <div 
              v-for="phase in phases" 
              :key="phase.key"
              class="step-item"
              :class="{ 
                active: project.currentPhase === phase.key,
                completed: phases.findIndex(p => p.key === project.currentPhase) > phases.findIndex(p => p.key === phase.key)
              }"
            >
              <div class="step-indicator">
                {{ phase.icon }}
              </div>
              <div class="step-label">
                <h4>{{ phase.name }}</h4>
                <span class="badge">{{ phase.key }}</span>
              </div>
            </div>
          </div>

          <!-- Controles de ejecución -->
          <div class="control-actions" style="display: flex; flex-direction: column; gap: 8px; margin-top: 15px;">
            <div v-if="connectionMode === 'api'" style="display: flex; gap: 8px; width: 100%;">
              <button 
                v-if="project.status !== 'RUNNING'" 
                @click="orchestrateApiPipeline" 
                class="btn btn-primary start-btn" 
                style="flex: 1;"
              >
                🚀 Iniciar / Reanudar
              </button>
              <button 
                v-else 
                @click="pauseApiPipeline" 
                class="btn btn-warning pause-btn" 
                style="flex: 1; background: #f39c12; color: white; border: none; border-radius: 6px; padding: 10px; cursor: pointer; font-weight: 600;"
              >
                ⏸️ Pausar Flujo
              </button>
              
              <button @click="resetApiPipeline" class="btn btn-outline reset-btn" style="flex: 1;">
                🔄 Reiniciar
              </button>
            </div>
            
            <div v-else style="display: flex; gap: 8px; width: 100%;">
              <button @click="triggerPipeline" class="btn btn-primary start-btn" style="flex: 1;">
                🚀 Iniciar Simulación
              </button>
              <button @click="resetPipeline" class="btn btn-outline reset-btn" style="flex: 1;">
                Reiniciar
              </button>
            </div>

            <!-- Botón para guardar cambios o crear nuevo proyecto -->
            <button 
              v-if="connectionMode === 'api'" 
              @click="createNewProject" 
              class="btn btn-outline save-btn"
              style="width: 100%;"
            >
              💾 Guardar como Nuevo Proyecto
            </button>
          </div>
        </div>

        <!-- Collapsed Content -->
        <div v-show="isLeftCollapsed" class="panel-collapsed-content">
          <div class="collapsed-icon">📁</div>
          <h3 class="collapsed-title">Proyecto & Estado</h3>
        </div>
      </section>

      <!-- CENTER PANEL: Subagents and Activity logs -->
      <section 
        class="grid-panel glass-panel agents-panel"
        :class="{ 'collapsed': isCenterCollapsed }"
      >
        <!-- Toggle button -->
        <button 
          class="panel-toggle-btn" 
          @click="isCenterCollapsed = !isCenterCollapsed"
          :title="isCenterCollapsed ? 'Expandir Panel' : 'Colapsar Panel'"
        >
          <span class="arrow-icon">{{ isCenterCollapsed ? '▶' : '◀' }}</span>
        </button>

        <!-- Full content -->
        <div v-show="!isCenterCollapsed" class="panel-full-content">
          <div class="panel-header">
            <span class="icon">🤖</span>
            <h2>Agentes en Acción</h2>
          </div>

          <!-- Agent Cards list -->
          <div class="agents-list">
            <div 
              v-for="agent in agents" 
              :key="agent.role"
              class="agent-card clickable"
              :class="[agent.status.toLowerCase(), { 'selected-audit': selectedAgentForAudit?.role === agent.role }]"
              @click="auditAgent(agent)"
            >
              <div class="agent-avatar">{{ agent.avatar }}</div>
              <div class="agent-info">
                <div class="agent-row">
                  <h3>{{ agent.name }}</h3>
                  <span class="status-badge" :class="agent.status.toLowerCase()">
                    {{ agent.status }}
                  </span>
                </div>
                <p class="role-desc">{{ agent.description }}</p>
                
                <!-- Loader for active agent -->
                <div v-if="agent.status === 'IN_PROGRESS'" class="progress-bar-container">
                  <div class="progress-bar-fill"></div>
                </div>
              </div>
            </div>
          </div>

          <hr class="divider" />

          <!-- Activity logs -->
          <div class="activity-logs">
            <h3>Registro de Actividad</h3>
            <div class="logs-container">
              <div 
                v-for="(log, idx) in logs" 
                :key="idx" 
                class="log-row"
                :class="log.type"
              >
                <span class="log-time">[{{ log.time }}]</span>
                <span class="log-msg">{{ log.message }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Collapsed Content -->
        <div v-show="isCenterCollapsed" class="panel-collapsed-content">
          <div class="collapsed-icon">🤖</div>
          <h3 class="collapsed-title">Agentes en Acción</h3>
        </div>
      </section>

      <!-- RIGHT PANEL: Human-in-the-Loop & Audit Pane -->
      <section 
        class="grid-panel glass-panel hitl-panel"
        :class="{ 'attention': activeTask && activeTab === 'hitl', 'maximized': isRightMaximized }"
      >
        <!-- Tab Headers -->
        <div class="tab-headers">
          <button 
            class="tab-btn" 
            :class="{ active: activeTab === 'hitl', 'has-task': activeTask }"
            @click="activeTab = 'hitl'"
          >
            👤 HITL <span v-if="activeTask" class="notification-dot"></span>
          </button>
          <button 
            class="tab-btn" 
            :class="{ active: activeTab === 'audit' }"
            @click="activeTab = 'audit'"
          >
            🔍 Auditoría
          </button>

          <!-- Maximize Button -->
          <button 
            class="tab-btn maximize-btn" 
            @click="toggleRightMaximize" 
            :title="isRightMaximized ? 'Restaurar Vista' : 'Maximizar Vista'"
          >
            <span class="maximize-icon">{{ isRightMaximized ? '⧉' : '⛶' }}</span>
          </button>
        </div>

        <div class="tab-content">
          <!-- HITL TAB -->
          <div v-if="activeTab === 'hitl'" class="tab-pane">
            <!-- No task waiting -->
            <div v-if="!activeTask" class="no-task-placeholder">
              <div class="relax-icon">🛡️</div>
              <h3>Todo en orden</h3>
              <p>No hay tareas pendientes en espera de validación. Los agentes están procesando o inactivos.</p>
            </div>

            <!-- Task waiting for approval -->
            <div v-else class="active-task-container">
              <div class="task-alert">
                <span class="pulse-bullet warning"></span>
                <h3>Revisión Requerida</h3>
              </div>

              <div class="task-info">
                <h4>{{ activeTask.title }}</h4>
                <p>{{ activeTask.description }}</p>
              </div>

              <div class="code-viewer">
                <div class="code-header">
                  <span>Artefacto de Salida (Markdown / Code)</span>
                </div>
                <pre><code>{{ activeTask.outputData }}</code></pre>
              </div>

              <!-- Actions -->
              <div class="hitl-actions">
                <button @click="approveTask" class="btn btn-success approve-btn">
                  ✅ Aprobar & Continuar
                </button>

                <div class="rejection-box">
                  <textarea 
                    v-model="reviewerFeedback" 
                    placeholder="Indica el motivo del rechazo para que el agente lo corrija..."
                    rows="3"
                    class="feedback-textarea"
                  ></textarea>
                  <button @click="rejectTask" class="btn btn-error reject-btn">
                    ❌ Rechazar Entrega
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- AUDIT TAB -->
          <div v-else-if="activeTab === 'audit'" class="tab-pane">
            <!-- No agent selected -->
            <div v-if="!selectedAgentForAudit" class="no-task-placeholder">
              <div class="relax-icon">🔍</div>
              <h3>Auditoría</h3>
              <p>Haz clic en cualquier tarjeta de agente en el panel central para inspeccionar sus tareas y logs.</p>
            </div>
            <!-- Agent Selected -->
            <div v-else class="audit-inspector-container">
              <div class="inspector-header">
                <span class="agent-avatar">{{ selectedAgentForAudit.avatar }}</span>
                <div>
                  <h4>{{ selectedAgentForAudit.name }}</h4>
                  <p class="role-desc">{{ selectedAgentForAudit.description }}</p>
                </div>
              </div>

              <!-- List of tasks -->
              <div class="audit-tasks-list">
                <div v-if="selectedAgentTasks.length === 0" class="no-tasks-msg">
                  No hay tareas registradas para este agente todavía.
                </div>
                <div 
                  v-for="task in selectedAgentTasks" 
                  :key="task.id" 
                  class="audit-task-card"
                  :class="task.status.toLowerCase()"
                >
                  <div class="task-card-header">
                    <h5>{{ task.title }}</h5>
                    <span class="status-badge" :class="task.status.toLowerCase()">{{ task.status }}</span>
                  </div>
                  <p class="task-desc">{{ task.description }}</p>

                  <div class="task-io-grid">
                    <div class="io-section" v-if="task.inputData">
                      <strong>Entrada (Input):</strong>
                      <pre><code>{{ task.inputData }}</code></pre>
                    </div>
                    <div class="io-section" v-if="task.outputData">
                      <strong>Salida (Output):</strong>
                      <pre><code>{{ task.outputData }}</code></pre>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

    </main>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  padding: 20px;
  gap: 20px;
  max-width: 1440px;
  margin: 0 auto;
  min-height: 100vh;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 30px;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 15px;
}

.titles h1 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.titles .subtitle {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.connection-control {
  display: flex;
  align-items: center;
  gap: 15px;
}

.toggle-mode {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.toggle-mode input {
  accent-color: var(--color-primary);
}

.api-input {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--panel-border);
  border-radius: 6px;
  color: #fff;
  padding: 4px 10px;
  font-size: 0.85rem;
  width: 180px;
}

/* Grid Layout */
.dashboard-grid {
  display: grid;
  grid-template-columns: 3fr 4fr 3fr;
  gap: 20px;
  transition: grid-template-columns 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.dashboard-grid.left-collapsed {
  grid-template-columns: 60px 4fr 3fr;
}

.dashboard-grid.center-collapsed {
  grid-template-columns: 3fr 60px 3fr;
}

.dashboard-grid.both-collapsed {
  grid-template-columns: 60px 60px 1fr;
}

.grid-panel {
  display: flex;
  flex-direction: column;
  padding: 24px;
  height: calc(100vh - 160px);
  min-height: 0;
  overflow: hidden;
  position: relative;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.panel-header h2 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
}

.divider {
  border: 0;
  height: 1px;
  background: var(--panel-border);
  margin: 20px 0;
}

/* Project panel styles */
.project-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-group label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.form-control {
  background: rgba(255,255,255,0.03);
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  color: #fff;
  padding: 8px 12px;
  font-size: 0.85rem;
}

.form-control:focus {
  border-color: var(--color-primary);
  outline: none;
}

/* Stepper styles */
.stepper-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  flex-grow: 1;
  overflow-y: auto;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 15px;
  opacity: 0.4;
  transition: opacity 0.3s ease;
}

.step-item.active {
  opacity: 1;
}

.step-item.completed {
  opacity: 0.8;
}

.step-indicator {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255,255,255,0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
}

.step-item.active .step-indicator {
  background: var(--color-primary-glow);
  border: 1px solid var(--color-primary);
}

.step-item.completed .step-indicator {
  background: var(--color-success-glow);
  border: 1px solid var(--color-success);
}

.step-label h4 {
  margin: 0;
  font-size: 0.9rem;
}

.step-label .badge {
  font-size: 0.7rem;
  color: var(--color-text-muted);
}

/* Button UI */
.btn {
  padding: 12px 20px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  border: 0;
  transition: all 0.2s ease;
}

.btn-primary {
  background: var(--color-primary);
  color: #0b0f19;
}

.btn-primary:hover {
  box-shadow: 0 0 15px var(--color-primary);
}

.btn-outline {
  background: transparent;
  border: 1px solid var(--panel-border);
  color: #fff;
}

.btn-outline:hover {
  background: rgba(255,255,255,0.05);
}

.start-btn {
  margin-top: 15px;
}

.reset-btn {
  margin-top: 8px;
}

/* Agents styles */
.agents-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  max-height: 55%;
}

.agent-card {
  display: flex;
  gap: 15px;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(255,255,255,0.02);
  border: 1px solid var(--panel-border);
  transition: all 0.25s ease;
}

.agent-card.in_progress {
  border-color: var(--color-primary);
  background: var(--color-primary-glow);
}

.agent-card.waiting_for_hitl {
  border-color: var(--color-warning);
  background: var(--color-warning-glow);
}

.agent-avatar {
  font-size: 1.5rem;
}

.agent-info {
  flex-grow: 1;
}

.agent-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.agent-row h3 {
  margin: 0;
  font-size: 0.95rem;
}

.status-badge {
  font-size: 0.7rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(255,255,255,0.05);
}

.status-badge.in_progress {
  background: var(--color-primary);
  color: #000;
}

.status-badge.completed {
  background: var(--color-success);
  color: #000;
}

.status-badge.waiting_for_hitl {
  background: var(--color-warning);
  color: #000;
}

.status-badge.rejected {
  background: var(--color-error);
  color: #000;
}

.role-desc {
  margin: 4px 0 0 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.progress-bar-container {
  height: 4px;
  background: rgba(255,255,255,0.1);
  border-radius: 2px;
  margin-top: 8px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  width: 40%;
  background: var(--color-primary);
  border-radius: 2px;
  animation: progress-slide 1.5s infinite linear;
}

@keyframes progress-slide {
  from { transform: translateX(-100%); }
  to { transform: translateX(250%); }
}

/* Log container */
.activity-logs {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  max-height: 40%;
}

.activity-logs h3 {
  margin: 0 0 10px 0;
  font-size: 0.95rem;
}

.logs-container {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  padding: 12px;
  font-family: monospace;
  font-size: 0.8rem;
  overflow-y: auto;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.log-row.success { color: var(--color-success); }
.log-row.warning { color: var(--color-warning); }
.log-row.error { color: var(--color-error); }
.log-row.primary { color: var(--color-primary); }

.log-time {
  color: var(--color-text-muted);
  margin-right: 6px;
}

/* HITL Panel */
.hitl-panel {
  transition: all 0.3s ease;
}

.hitl-panel.attention {
  border-color: var(--color-warning);
  box-shadow: 0 0 25px rgba(251, 191, 36, 0.1);
}

.no-task-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-grow: 1;
  text-align: center;
  color: var(--color-text-muted);
}

.relax-icon {
  font-size: 3rem;
  margin-bottom: 15px;
}

.no-task-placeholder h3 {
  color: #fff;
  margin: 0 0 8px 0;
  font-size: 1.1rem;
}

.no-task-placeholder p {
  font-size: 0.85rem;
  max-width: 250px;
}

.active-task-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
  flex-grow: 1;
  min-height: 0;
  overflow: hidden;
}

.task-alert {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-warning);
}

.task-alert h3 {
  margin: 0;
  font-size: 1.1rem;
}

.task-info h4 {
  margin: 0 0 4px 0;
  font-size: 1rem;
}

.task-info p {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.code-viewer {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: rgba(0,0,0,0.4);
  flex-grow: 1;
  min-height: 0;
  overflow: hidden;
}

.code-header {
  padding: 8px 12px;
  background: rgba(255,255,255,0.03);
  border-bottom: 1px solid var(--panel-border);
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.code-viewer pre {
  margin: 0;
  padding: 12px;
  overflow: auto;
  font-family: monospace;
  font-size: 0.8rem;
  color: #cbd5e1;
  flex-grow: 1;
  white-space: pre-wrap;
  word-break: break-word;
}

.hitl-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.approve-btn {
  background: var(--color-success);
  color: #0b0f19;
  width: 100%;
}

.approve-btn:hover {
  box-shadow: 0 0 15px var(--color-success);
}

.rejection-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-top: 1px solid var(--panel-border);
  padding-top: 10px;
}

.feedback-textarea {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  color: #fff;
  padding: 10px;
  font-size: 0.85rem;
  resize: vertical;
}

.reject-btn {
  background: var(--color-error);
  color: #fff;
  width: 100%;
}

.reject-btn:hover {
  box-shadow: 0 0 15px var(--color-error);
}

/* Auditor & Tabs Styles */
.agent-card.clickable {
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;
}

.agent-card.clickable:hover {
  transform: translateY(-2px);
  border-color: rgba(56, 189, 248, 0.4);
  background: rgba(255, 255, 255, 0.03);
}

.agent-card.selected-audit {
  border-color: var(--color-primary);
  background: rgba(56, 189, 248, 0.1) !important;
  box-shadow: 0 0 15px rgba(56, 189, 248, 0.15);
}

/* Tabs headers */
.tab-headers {
  display: flex;
  border-bottom: 1px solid var(--panel-border);
  margin-bottom: 20px;
  gap: 10px;
}

.tab-btn {
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--color-text-muted);
  padding: 8px 16px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  color: #fff;
}

.tab-btn.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.tab-btn.has-task .notification-dot {
  width: 8px;
  height: 8px;
  background-color: var(--color-warning);
  border-radius: 50%;
  display: inline-block;
  margin-left: 4px;
}

.tab-content {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.tab-pane {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

/* Auditor details layout */
.audit-inspector-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.inspector-header {
  display: flex;
  align-items: center;
  gap: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--panel-border);
  margin-bottom: 15px;
}

.inspector-header .agent-avatar {
  font-size: 2rem;
}

.inspector-header h4 {
  margin: 0;
  font-size: 1.1rem;
}

.audit-tasks-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  overflow-y: auto;
  flex-grow: 1;
  padding-right: 5px;
}

.no-tasks-msg {
  text-align: center;
  color: var(--color-text-muted);
  font-style: italic;
  padding-top: 40px;
  font-size: 0.85rem;
}

.audit-task-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.audit-task-card.failed {
  border-color: rgba(248, 113, 113, 0.3);
  background: rgba(248, 113, 113, 0.03);
}

.audit-task-card.completed, .audit-task-card.approved {
  border-color: rgba(52, 211, 153, 0.3);
  background: rgba(52, 211, 153, 0.03);
}

.task-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-card-header h5 {
  margin: 0;
  font-size: 0.95rem;
  color: #fff;
}

.task-desc {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.task-io-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 5px;
}

.io-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.io-section strong {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.io-section pre {
  margin: 0;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid var(--panel-border);
  border-radius: 6px;
  overflow: auto;
  font-family: monospace;
  font-size: 0.75rem;
  color: #e2e8f0;
  max-height: 180px;
  white-space: pre-wrap;
  word-break: break-word;
}

.project-select-dropdown {
  width: 220px !important;
  cursor: pointer;
  background-color: rgba(255, 255, 255, 0.08);
}

/* Panel full content to preserve nested flex behavior */
.panel-full-content {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  min-height: 0;
  width: 100%;
}

/* Toggle buttons styling */
.panel-toggle-btn {
  position: absolute;
  top: 15px;
  right: 15px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--panel-border);
  color: var(--color-text-muted);
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  z-index: 10;
}

.panel-toggle-btn:hover {
  background: var(--color-primary-glow);
  color: var(--color-primary);
  border-color: var(--color-primary);
  box-shadow: 0 0 10px rgba(56, 189, 248, 0.25);
}

.arrow-icon {
  font-size: 0.85rem;
  font-weight: bold;
}

/* Collapsed grid-panel overrides */
.grid-panel.collapsed {
  padding: 15px 10px;
  align-items: center;
}

/* Collapsed content container */
.panel-collapsed-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  height: 100%;
  width: 100%;
}

.collapsed-icon {
  font-size: 1.5rem;
  margin-top: 40px;
  opacity: 0.7;
}

.collapsed-title {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  transform: rotate(180deg);
  color: var(--color-text-muted);
  font-weight: 700;
  font-size: 0.8rem;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  margin: 0;
  white-space: nowrap;
  flex-grow: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.6;
}

/* Maximize Button in Tab headers */
.maximize-btn {
  margin-left: auto;
  border-bottom: 2px solid transparent !important;
  color: var(--color-text-muted);
  font-size: 1.15rem !important;
  padding: 8px !important;
}

.maximize-btn:hover {
  color: var(--color-primary) !important;
  background-color: transparent !important;
}

.maximize-icon {
  display: inline-block;
  transition: transform 0.2s ease;
}

.maximize-btn:hover .maximize-icon {
  transform: scale(1.15);
}
</style>

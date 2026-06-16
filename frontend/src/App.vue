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
  logs.value = []
  addLog('Sistema agéntico reiniciado. Listo para iniciar nueva iteración.', 'info')
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

    // Actualizar salidas en frontend con los artefactos de la API
    if (context.phaseOutputs.ANALYSIS) {
      project.phaseOutputs.ANALYSIS = context.phaseOutputs.ANALYSIS
      updateAgentStatus('PRODUCT_OWNER', 'COMPLETED', context.phaseOutputs.ANALYSIS)
      addLog('Requisitos generados por el Agente PO.', 'info')
    }

    // Comprobar si quedó en espera de revisión (Arquitectura usualmente requiere HITL)
    project.currentPhase = context.currentPhase
    if (context.currentPhase === 'ARCHITECTURE') {
      updateAgentStatus('ARCHITECT', 'WAITING_FOR_HITL', context.phaseOutputs.ARCHITECTURE || 'Generando...')
      addLog('Agente Arquitecto Técnico requiere aprobación manual (HITL).', 'warning')
      
      // Intentar obtener la tarea pendiente del backend (o simular la carga de tarea para aprobación)
      activeTask.value = {
        id: 'real-task-from-api-check', // En producción se consulta endpoint GET /api/v1/tasks/pending
        title: 'Aprobar especificación técnica',
        description: 'Valida la propuesta de Bounded Contexts y OpenAPI generada por el Arquitecto Técnico.',
        outputData: context.phaseOutputs.ARCHITECTURE || 'Generando borrador...',
        role: 'ARCHITECT'
      }
    }

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
      } else {
        addLog('Fallo al sincronizar aprobación con el servidor backend.', 'error')
      }
    } catch (err) {
      addLog(`Error de red con backend: ${err.message}`, 'error')
    }
  }

  updateAgentStatus(role, 'COMPLETED')
  activeTask.value = null
  
  if (role === 'ARCHITECT') {
    runDevelopmentPhase()
  } else if (role === 'SRE') {
    project.currentPhase = 'COMPLETED'
    addLog('¡Ciclo SDLC Multi-Agente finalizado con éxito!', 'success')
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
      }
    } catch (err) {
      addLog(`Error de red al enviar rechazo: ${err.message}`, 'error')
    }
  }

  updateAgentStatus(role, 'REJECTED')
  activeTask.value = null
  reviewerFeedback.value = ''
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

// API Connection Action Trigger
const connectToBackendApi = async () => {
  connectionMode.value = 'api'
  resetPipeline()
  addLog(`Conectándose al backend Spring Boot en ${backendUrl.value}...`, 'info')
  
  try {
    const res = await fetch(`${backendUrl.value}/actuator/health`, { mode: 'cors' })
    if (res.ok) {
      addLog('Conexión con el servidor backend establecida correctamente (Actuator OK).', 'success')
    } else {
      addLog('No se pudo verificar el estado del servidor. Asegúrate de ejecutar el backend.', 'warning')
    }
  } catch (err) {
    addLog(`El backend local no está levantado en ${backendUrl.value}. Usando modo simulación.`, 'warning')
    connectionMode.value = 'simulation'
  }
}

onMounted(() => {
  addLog('Panel de Supervisión Agéntica inicializado. Listo para operar.', 'success')
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
    <main class="dashboard-grid">
      
      <!-- LEFT PANEL: Project & Phase State -->
      <section class="grid-panel glass-panel project-panel">
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

        <button @click="triggerPipeline" class="btn btn-primary start-btn">
          🚀 Iniciar Flujo de Agentes
        </button>
        <button @click="resetPipeline" class="btn btn-outline reset-btn">
          Reiniciar Todo
        </button>
      </section>

      <!-- CENTER PANEL: Subagents and Activity logs -->
      <section class="grid-panel glass-panel agents-panel">
        <div class="panel-header">
          <span class="icon">🤖</span>
          <h2>Agentes en Acción</h2>
        </div>

        <!-- Agent Cards list -->
        <div class="agents-list">
          <div 
            v-for="agent in agents" 
            :key="agent.role"
            class="agent-card"
            :class="agent.status.toLowerCase()"
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
      </section>

      <!-- RIGHT PANEL: Human-in-the-Loop Review Pane -->
      <section 
        class="grid-panel glass-panel hitl-panel"
        :class="{ 'attention': activeTask }"
      >
        <div class="panel-header">
          <span class="icon">👤</span>
          <h2>Aprobación Humana (HITL)</h2>
        </div>

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
}

.grid-panel {
  display: flex;
  flex-direction: column;
  padding: 24px;
  height: calc(100vh - 160px);
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
  height: calc(100% - 40px);
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
</style>

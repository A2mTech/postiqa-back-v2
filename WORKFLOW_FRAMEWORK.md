# Workflow Orchestration Framework

Un framework générique et performant pour orchestrer des workflows complexes avec support complet de la résilience, compensation, et observabilité.

## 🎯 Vue d'ensemble

Ce framework implémente un moteur d'orchestration de workflows basé sur la **Clean Architecture** avec les fonctionnalités suivantes :

- ✅ **Exécution séquentielle et parallèle** - DAG resolution avec tri topologique
- ✅ **Retry automatique** - Exponential backoff configurable par step
- ✅ **Compensation/Rollback** - Saga pattern pour annuler des steps exécutés
- ✅ **Pause/Resume** - Workflows persistés en PostgreSQL avec reprise
- ✅ **Timeout configurables** - Par step avec gestion async
- ✅ **Observabilité complète** - Events Spring + Métriques Micrometer
- ✅ **Persistence PostgreSQL** - Context JSONB + Optimistic locking
- ✅ **State Management** - Tracking d'état avancé + health checks

## 📐 Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER                         │
│  (Pure Java - Zero dépendances)                         │
├─────────────────────────────────────────────────────────┤
│  • WorkflowDefinition, WorkflowInstance                 │
│  • WorkflowStep<I,O>, WorkflowContext                   │
│  • RetryPolicy, CompensationAction                      │
│  • Ports: WorkflowExecutionPort, PersistencePort, ...   │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   USE CASES LAYER                       │
├─────────────────────────────────────────────────────────┤
│  • StartWorkflowUseCase                                 │
│  • PauseWorkflowUseCase, ResumeWorkflowUseCase          │
│  • GetWorkflowStatusUseCase, CancelWorkflowUseCase      │
│  • CompensateWorkflowUseCase                            │
│  • ExecuteWorkflowStepUseCase                           │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  ADAPTERS LAYER                         │
├─────────────────────────────────────────────────────────┤
│  IN:  WorkflowOrchestrator (Facade publique)            │
│  OUT: JpaWorkflowPersistenceAdapter                     │
│       SpringEventWorkflowEventAdapter                   │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│               INFRASTRUCTURE LAYER                      │
├─────────────────────────────────────────────────────────┤
│  Engine:                                                │
│    • WorkflowEngine (orchestrateur central)             │
│    • StepExecutor (@Async execution)                    │
│    • DependencyResolver (topological sort)              │
│    • ParallelExecutionCoordinator                       │
│                                                         │
│  Resilience:                                            │
│    • RetryHandler (exponential backoff)                 │
│    • TimeoutHandler (CompletableFuture)                 │
│    • CompensationHandler (Saga pattern)                 │
│                                                         │
│  State Management:                                      │
│    • WorkflowStateManager (transitions)                 │
│    • StepStateTracker (analytics)                       │
│    • ContextSerializer (JSON)                           │
│                                                         │
│  Monitoring:                                            │
│    • WorkflowMetrics (Micrometer)                       │
│    • WorkflowEventPublisher (Spring Events)             │
└─────────────────────────────────────────────────────────┘
```

## 🚀 Utilisation rapide

### 1. Définir un workflow

```java
import fr.postiqa.core.domain.workflow.model.*;
import fr.postiqa.core.domain.workflow.enums.*;

WorkflowDefinition workflow = WorkflowDefinition.builder()
    .name("UserProfileAnalysis")
    .description("Analyse complète du profil utilisateur")
    .executionMode(ExecutionMode.PARALLEL)
    .compensationStrategy(CompensationStrategy.REVERSE_ORDER)
    .globalTimeout(Duration.ofHours(1))

    // Step 1: Scraper le site
    .addStep(new ScrapeSiteStep())

    // Step 2: Analyser (dépend du scraping)
    .addStep(new AnalyzeSiteStep(), "scrape-site")

    // Step 3: Générer le rapport
    .addStep(new GenerateReportStep(), "analyze-site")

    .build();
```

### 2. Créer un Step

```java
public class ScrapeSiteStep implements WorkflowStep<Void, Map<String, Object>> {

    @Override
    public String getStepId() {
        return "scrape-site";
    }

    @Override
    public Map<String, Object> execute(Void input, WorkflowContext context) throws Exception {
        // Logique de scraping
        return Map.of("site_data", scrapedContent);
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("scraped_data"); // Stocker dans le context
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(5);
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(3, Duration.ofSeconds(1));
    }

    @Override
    public Optional<CompensationAction<Map<String, Object>>> getCompensationAction() {
        return Optional.of((data, ctx) -> {
            // Logique de rollback
            log.info("Nettoyage des données scrapées");
        });
    }
}
```

### 3. Exécuter le workflow

```java
import fr.postiqa.core.adapter.in.workflow.WorkflowOrchestrator;

@Service
public class AnalysisService {

    private final WorkflowOrchestrator orchestrator;

    public void analyzeUser(String userId) {
        // Context initial
        WorkflowContext context = WorkflowContext.of(Map.of("user_id", userId));

        // Exécution synchrone
        WorkflowInstance result = orchestrator.startWorkflow(workflow, context);

        if (result.getStatus() == WorkflowStatus.COMPLETED) {
            String report = result.getContext()
                .get("final_report", String.class)
                .orElse("");
            log.info("Analyse terminée: {}", report);
        }
    }

    public String analyzeUserAsync(String userId) {
        WorkflowContext context = WorkflowContext.of(Map.of("user_id", userId));

        // Exécution async (fire-and-forget)
        String instanceId = orchestrator.startWorkflowAndGetId(workflow, context);

        return instanceId; // Pour tracking
    }
}
```

### 4. Gérer l'état du workflow

```java
// Récupérer le statut
WorkflowInstance instance = orchestrator.getWorkflow(instanceId).orElseThrow();
log.info("Status: {}", instance.getStatus());
log.info("Progress: {}%", instance.getProgress(totalSteps) * 100);

// Pause/Resume
orchestrator.pauseWorkflow(instanceId);
orchestrator.resumeWorkflow(instanceId, workflow);

// Cancel
orchestrator.cancelWorkflow(instanceId);

// Compensation manuelle
orchestrator.compensateWorkflow(instanceId, workflow);

// Health check
var health = orchestrator.checkHealth(instanceId);
if (!health.isHealthy()) {
    log.warn("Workflow unhealthy: {}", health.message());
}
```

### 5. Monitoring & Métriques

```java
// Statistiques d'exécution
var stats = orchestrator.getExecutionStats(instanceId);
log.info("Steps: {}/{} completed", stats.completedSteps(), stats.totalSteps());
log.info("Retry rate: {}%", stats.retryRate() * 100);

// Métriques globales
var metrics = orchestrator.getMetricsSnapshot();
log.info("Workflows: {} started, {} completed, {} failed",
    metrics.workflowsStarted(),
    metrics.workflowsCompleted(),
    metrics.workflowsFailed());
log.info("Success rate: {}%", metrics.getWorkflowSuccessRate() * 100);

// Résumé d'état
var summary = orchestrator.getStateSummary(instanceId);
log.info("Workflow: {} - Status: {} - Duration: {}",
    summary.workflowName(),
    summary.currentStatus(),
    summary.totalDuration());
```

## 📊 Exemple complet : Workflow d'analyse multi-source

```java
public class CompleteAnalysisWorkflow {

    public static WorkflowDefinition buildWorkflow() {
        return WorkflowDefinition.builder()
            .name("CompleteUserAnalysis")
            .executionMode(ExecutionMode.PARALLEL)
            .compensationStrategy(CompensationStrategy.REVERSE_ORDER)
            .globalTimeout(Duration.ofMinutes(30))

            // PHASE 1: Scraping en parallèle
            .addStep(new ScrapeSiteStep())
            .addStep(new ScrapeLinkedInStep())
            .addStep(new ScrapeTwitterStep())
            .addStep(new ScrapeInstagramStep())

            // PHASE 2: Analyses atomiques (dépendent du scraping)
            .addStep(new AnalyzeSiteStep(), "scrape-site")
            .addStep(new AnalyzeLinkedInProfileStep(), "scrape-linkedin")
            .addStep(new AnalyzeLinkedInPostsStep(), "scrape-linkedin")
            .addStep(new AnalyzeTwitterPostsStep(), "scrape-twitter")
            .addStep(new AnalyzeInstagramPostsStep(), "scrape-instagram")

            // PHASE 3: Cross-référencement (dépend de toutes les analyses)
            .addStep(new CrossReferenceAnalysisStep(),
                "analyze-site",
                "analyze-linkedin-profile",
                "analyze-linkedin-posts",
                "analyze-twitter-posts",
                "analyze-instagram-posts")

            // PHASE 4: Scoring
            .addStep(new CalculateScoringStep(), "cross-reference")

            // PHASE 5: Profil final
            .addStep(new GenerateFinalProfileStep(), "scoring")

            .build();
    }
}
```

## 🔧 Configuration

### application.properties

```properties
# Workflow executor thread pool
workflow.executor.core-pool-size=10
workflow.executor.max-pool-size=50
workflow.executor.queue-capacity=100

# Database (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/mydatabase
spring.datasource.username=myuser
spring.datasource.password=secret

# Liquibase
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml

# Metrics (Micrometer)
management.metrics.export.prometheus.enabled=true
management.endpoints.web.exposure.include=health,metrics,prometheus
```

### WorkflowConfig.java (déjà créé)

Thread pool configuré automatiquement avec :
- Core pool: 10 threads
- Max pool: 50 threads
- Queue capacity: 100 tasks
- Rejection policy: CallerRunsPolicy

## 📈 Métriques Micrometer

Le framework expose automatiquement les métriques suivantes :

### Compteurs
- `workflow.started` - Workflows démarrés
- `workflow.completed` - Workflows terminés avec succès
- `workflow.failed` - Workflows échoués
- `workflow.cancelled` - Workflows annulés
- `workflow.compensated` - Workflows compensés (rollback)
- `workflow.steps.executed` - Steps exécutés
- `workflow.steps.failed` - Steps échoués
- `workflow.steps.retried` - Steps retentés
- `workflow.steps.compensated` - Steps compensés

### Timers
- `workflow.duration` - Durée d'exécution des workflows
- `workflow.step.duration` - Durée d'exécution des steps

### Par workflow/step (avec tags)
- `workflow.executions` (tags: workflow, status)
- `workflow.step.executions` (tags: workflow, step, status)

## 🎭 Events Spring

Le framework publie 14 types d'events pour l'observabilité :

**Workflow events:**
- `WorkflowStartedEvent`
- `WorkflowCompletedEvent`
- `WorkflowFailedEvent`
- `WorkflowPausedEvent`
- `WorkflowResumedEvent`
- `WorkflowCancelledEvent`
- `CompensationStartedEvent`
- `CompensationCompletedEvent`

**Step events:**
- `StepStartedEvent`
- `StepCompletedEvent`
- `StepFailedEvent`
- `StepRetriedEvent`
- `StepSkippedEvent`
- `StepCompensatedEvent`

### Écouter les events

```java
@Component
public class WorkflowEventListener {

    @EventListener
    public void onWorkflowCompleted(WorkflowCompletedEvent event) {
        log.info("Workflow {} completed in {}",
            event.workflowName(), event.duration());
        // Notifier l'utilisateur, envoyer un email, etc.
    }

    @EventListener
    public void onWorkflowFailed(WorkflowFailedEvent event) {
        log.error("Workflow {} failed: {}",
            event.workflowName(), event.reason());
        // Alerting, notification d'erreur
    }
}
```

## 🗄️ Schéma de base de données

Le framework utilise 2 tables PostgreSQL :

### workflow_instances

```sql
CREATE TABLE workflow_instances (
    instance_id VARCHAR(255) PRIMARY KEY,
    workflow_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    context JSONB, -- Context partagé (JSONB pour queries efficaces)
    failure_reason TEXT,
    version BIGINT NOT NULL, -- Optimistic locking
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_workflow_name ON workflow_instances(workflow_name);
CREATE INDEX idx_workflow_status ON workflow_instances(status);
CREATE INDEX idx_workflow_name_status ON workflow_instances(workflow_name, status);
```

### workflow_step_executions

```sql
CREATE TABLE workflow_step_executions (
    execution_id UUID PRIMARY KEY,
    workflow_instance_id VARCHAR(255) NOT NULL,
    step_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    attempt_number INTEGER NOT NULL,
    error_message TEXT,
    output JSONB, -- Output du step (JSONB)
    metadata JSONB, -- Métadonnées additionnelles
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    FOREIGN KEY (workflow_instance_id)
        REFERENCES workflow_instances(instance_id) ON DELETE CASCADE
);

CREATE INDEX idx_step_workflow_instance ON workflow_step_executions(workflow_instance_id);
CREATE INDEX idx_step_status ON workflow_step_executions(status);
CREATE INDEX idx_workflow_step ON workflow_step_executions(workflow_instance_id, step_id);
```

## 🎨 Design Patterns

Le framework utilise **10 design patterns** :

1. **Command Pattern** - `WorkflowStep` encapsule une action exécutable
2. **Builder Pattern** - `WorkflowDefinition.builder()` pour construction fluide
3. **Strategy Pattern** - `RetryPolicy`, `CompensationStrategy`
4. **Template Method** - Hooks dans `WorkflowStep` (onBefore, onAfter, onError)
5. **Saga Pattern** - Compensation orchestrée en ordre inverse
6. **Repository Pattern** - `WorkflowPersistencePort` abstraction
7. **Observer Pattern** - Events Spring pour observabilité
8. **State Pattern** - `WorkflowStatus`, `StepStatus` state machines
9. **Chain of Responsibility** - Pipeline de steps
10. **Facade Pattern** - `WorkflowOrchestrator` API simplifiée

## 📦 Structure des fichiers (60+ fichiers)

```
core/
├── domain/workflow/                    # Domain layer (Pure Java)
│   ├── enums/
│   │   ├── WorkflowStatus.java
│   │   ├── StepStatus.java
│   │   ├── ExecutionMode.java
│   │   └── CompensationStrategy.java
│   ├── model/
│   │   ├── WorkflowDefinition.java
│   │   ├── WorkflowInstance.java
│   │   ├── WorkflowStep.java
│   │   ├── WorkflowContext.java
│   │   ├── StepResult.java
│   │   ├── RetryPolicy.java
│   │   ├── CompensationAction.java
│   │   └── StepDependency.java
│   └── port/
│       ├── WorkflowExecutionPort.java
│       ├── WorkflowPersistencePort.java
│       └── WorkflowEventPort.java
│
├── usecase/workflow/                   # Use cases
│   ├── StartWorkflowUseCase.java
│   ├── GetWorkflowStatusUseCase.java
│   ├── PauseWorkflowUseCase.java
│   ├── ResumeWorkflowUseCase.java
│   ├── CancelWorkflowUseCase.java
│   ├── CompensateWorkflowUseCase.java
│   └── ExecuteWorkflowStepUseCase.java
│
├── adapter/
│   ├── in/workflow/                    # Adapter IN (Facade)
│   │   ├── WorkflowOrchestrator.java
│   │   └── WorkflowOrchestratorImpl.java
│   └── out/workflow/                   # Adapters OUT
│       ├── JpaWorkflowPersistenceAdapter.java
│       └── SpringEventWorkflowEventAdapter.java
│
└── infrastructure/workflow/            # Infrastructure
    ├── engine/
    │   ├── WorkflowEngine.java
    │   ├── StepExecutor.java
    │   ├── DependencyResolver.java
    │   └── ParallelExecutionCoordinator.java
    ├── resilience/
    │   ├── RetryHandler.java
    │   ├── TimeoutHandler.java
    │   └── CompensationHandler.java
    ├── state/
    │   ├── WorkflowStateManager.java
    │   ├── StepStateTracker.java
    │   └── ContextSerializer.java
    ├── monitoring/
    │   ├── WorkflowMetrics.java
    │   └── WorkflowEventPublisher.java
    ├── config/
    │   └── WorkflowConfig.java
    └── example/
        └── SimpleAnalysisWorkflow.java

database/
├── entity/
│   ├── WorkflowInstanceEntity.java
│   └── WorkflowStepExecutionEntity.java
└── repository/
    ├── WorkflowInstanceRepository.java
    └── WorkflowStepExecutionRepository.java
```

## 🧪 Tests

```java
@SpringBootTest
class WorkflowIntegrationTest {

    @Autowired
    private WorkflowOrchestrator orchestrator;

    @Test
    void shouldExecuteWorkflowSuccessfully() {
        // Given
        WorkflowDefinition workflow = SimpleAnalysisWorkflow.buildWorkflow();
        WorkflowContext context = WorkflowContext.empty();

        // When
        WorkflowInstance result = orchestrator.startWorkflow(workflow, context);

        // Then
        assertThat(result.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(result.getContext().contains("final_report")).isTrue();
    }

    @Test
    void shouldRetryOnFailure() {
        // Test retry logic
    }

    @Test
    void shouldCompensateOnFailure() {
        // Test compensation
    }
}
```

## 🚀 Roadmap

- [ ] Support GraphQL API pour monitoring
- [ ] Dashboard UI temps réel
- [ ] Workflow versioning
- [ ] Distributed tracing (OpenTelemetry)
- [ ] Rate limiting par workflow
- [ ] Workflow templates library

## 📝 License

Internal - Postiqa © 2025

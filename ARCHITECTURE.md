# Architecture Documentation

## Overview

Postiqa Back v2 is a **package-based modular monolith** built with Spring Modulith. The architecture combines Clean Architecture for complex business logic with Spring Classic for simpler features, enforcing strict module boundaries through compile-time and runtime validation.

## Architectural Principles

### Modular Monolith (Spring Modulith)

- **Single Gradle module** with logical separation via packages under `fr.postiqa`
- **Module boundary enforcement** via Spring Modulith validation
- **Event-driven communication** between modules (no direct dependencies)
- **Shared modules** accessible by all: `shared`, `database`

### Clean Architecture

Applied to modules with complex business logic:
- **Domain layer**: Entities, value objects, domain ports (no framework dependencies)
- **Use case layer**: Business logic orchestration (framework-agnostic)
- **Adapter layer**: Inbound (controllers) and outbound (gateways) adapters
- **Infrastructure layer**: External clients, configurations, framework integrations

**Dependency Rule**: domain ← usecase ← adapter ← infrastructure

## Module Structure

### 1. Foundation Modules

#### `shared` - Shared Components
**Purpose**: Cross-cutting concerns accessible by all modules

**Contents**:
- **DTOs** (45 classes): LoginRequest/Response, PostDto, SocialAccountDto
- **Exceptions** (25 classes): UserNotFoundException, InvalidCredentialsException
- **Annotations**: `@UseCase`, `@LogActivity`, `@RequirePermission`, `@TenantScoped`
- **Enums**: SocialPlatform, PostStatus, PostType, MediaType
- **Use Case Infrastructure**: Base interfaces, AOP handler (UseCaseHandler)

**Key Components**:
```java
@UseCase(
    resourceType = "POST",
    description = "Creates social media post"
)
public class CreatePostUseCase {
    // Automatic activity logging via AOP
}
```

#### `database` - Persistence Layer
**Purpose**: JPA entities and Spring Data repositories (NO business logic)

**22 Entities**:
- Auth: UserEntity, RoleEntity, PermissionEntity, ApiKeyEntity
- Organization: OrganizationEntity, ClientEntity, OrganizationMemberEntity
- Social: SocialAccountEntity, PostEntity, MediaEntity
- Workflow: WorkflowInstanceEntity, WorkflowStepExecutionEntity

**Repository Pattern**: Spring Data JPA with derived queries and JPQL

**Design Rule**: Entities are pure data models. Business logic belongs in domain/usecase layers.

#### `gateway` - Security & Auth
**Purpose**: Authentication, authorization, organization management

**Architecture**: Use case-driven (17 use cases)

**Key Use Cases**:
- Auth: LoginUseCase, RefreshTokenUseCase, ValidateTokenUseCase
- Organization: InviteMemberUseCase, UpdateMemberRoleUseCase, GrantPermissionOverrideUseCase

**Authentication Mechanisms**:
1. **JWT**: Stateless tokens with custom claims (user_id, organization_id, client_id)
2. **API Key**: Header-based authentication (`X-API-Key`)
3. **OAuth2**: Social login (Google, GitHub, LinkedIn)

**Authorization Model**:
- RBAC with role-permission mapping
- Resource-action permissions (`POST:CREATE`, `POST:*`)
- Permission overrides per user
- Multi-tenant scope validation

**Filter Chain**: ApiKeyFilter → JwtFilter → TenantResolutionFilter

### 2. Core Module - Scraping & AI Orchestration

**Architecture**: Full Clean Architecture

**Responsibility**: Orchestrate external APIs for scraping and AI analysis, manage workflow execution

#### Domain Layer (`core/domain`)

**Models**:
- SocialPost, SocialProfile, WebsiteContent
- WritingStyleAnalysis, ContentThemesAnalysis
- WorkflowDefinition, WorkflowInstance, WorkflowStep

**Ports** (15 interfaces):
- ScrapingPort, WebScrapingPort, AnalysisPort
- WorkflowExecutionPort, WorkflowPersistencePort, WorkflowEventPort

**Enums**: SocialPlatform, ContentType, AnalysisType, WorkflowStatus, StepStatus, ExecutionMode

#### Use Case Layer (`core/usecase`)

**Scraping Use Cases** (5):
- `GetSocialPostsUseCase`: Fetch posts (sync/async/native modes)
- `GetSocialProfileUseCase`: Fetch user profiles
- `GetWebsiteDataUseCase`: Scrape website content
- `PollScrapingJobUseCase`: Monitor async job status
- `AnalyzeUserProfileUseCase`: Multi-platform scraping + AI analysis

**Workflow Use Cases** (7):
- `StartWorkflowUseCase`: Initialize workflow execution
- `ExecuteWorkflowStepUseCase`: Execute individual steps
- `GetWorkflowStatusUseCase`: Monitor progress
- `PauseWorkflowUseCase`, `ResumeWorkflowUseCase`, `CancelWorkflowUseCase`
- `CompensateWorkflowUseCase`: Rollback failed workflows

#### Adapter Layer (`core/adapter`)

**Inbound**: CoreFacade (public API)
**Outbound**: ApifyScrapingGateway, OpenAIAnalysisGateway, InMemoryCacheAdapter, JpaWorkflowPersistenceAdapter

#### Infrastructure Layer (`core/infrastructure`)

**Clients**:
- `ApifyClient`: REST client for Apify API
- `OpenAIAnalysisProvider`: OpenAI integration with strategy pattern

**Actor System**: Platform-specific scraping configurations
- ActorRegistry, ActorConfig, ActorInputBuilder, ActorOutputParser
- Implementations: LinkedInActor, InstagramActor, TikTokActor

**Analysis Framework**:
- AnalysisStrategy pattern (4 types: writing style, content themes, image, multimodal)
- PromptBuilder for context-aware templates
- AnalysisProviderRegistry for multi-provider support

**Workflow Engine**:
- `WorkflowEngine`: DAG-based orchestration
- `DependencyResolver`: Topological sort for execution order
- `ParallelExecutionCoordinator`: Concurrent step execution
- `RetryHandler`, `TimeoutHandler`, `CompensationHandler`

**Workflow Features**:
- Sequential/parallel execution modes
- Retry policies with exponential backoff
- Step-level timeouts
- Compensation strategies: ALL, CRITICAL_ONLY, BEST_EFFORT
- Context-based input/output chaining

### 3. Feature Modules

#### `features.postmanagement` - Post Lifecycle Management

**Architecture**: Full Clean Architecture

**Domain Models**:
- Post (aggregate root)
- Channel, Media (entities)
- PostId, ChannelId, Content (value objects)

**11 Use Cases**:
- CRUD: CreatePostUseCase, UpdatePostUseCase, DeletePostUseCase, GetPostUseCase, ListPostsUseCase
- Scheduling: SchedulePostUseCase, CancelScheduleUseCase
- Media: UploadMediaUseCase, DeleteMediaUseCase
- Channels: GetChannelUseCase, ListChannelsUseCase

**Events Published**:
- PostCreatedEvent, PostScheduledEvent, PostScheduleCancelledEvent
- PostPublishedEvent, PostPublishFailedEvent, PostDeletedEvent

**Adapter**: SpringPostEventAdapter (publishes events via ApplicationEventPublisher)

#### `features.socialaccounts` - Social Platform Connectivity

**Architecture**: Full Clean Architecture

**Domain Models**:
- SocialAccount (aggregate root)
- OAuth2Token (value object)
- Platform metadata (JSONB in database)

**7 Use Cases**:
- OAuth2: GenerateAuthorizationUrlUseCase, ConnectSocialAccountUseCase
- Management: ListSocialAccountsUseCase, GetSocialAccountUseCase, DisconnectSocialAccountUseCase
- Token: RefreshTokenUseCase (hourly scheduler), TestConnectionUseCase

**Supported Platforms**: LinkedIn, Twitter, Instagram, YouTube, TikTok

**Token Refresh**: Automatic hourly check via @Scheduled job

#### Planned Features (Not Implemented)

**`features.contentgeneration`**: Directory structure exists, no code
**`features.editorialcalendar`**: Directory structure exists, no code
**`features.publishing`**: Empty directories only
**`features.weeklybrief`**: Empty directories only
**`features.analytics`**: Empty directories only

### 4. API REST Modules

#### `business` - Business API

**Endpoints**: `/api/business/*`

**Target Audience**: Businesses managing their own social media

**Architecture**: Thin orchestration layer (NO business logic)

**Controllers**:
- BusinessOrganizationController: Member/role/permission management
- BusinessSocialAccountController: OAuth2 flow orchestration
- PostController: Post CRUD and scheduling
- ChannelController: Read-only channel access
- MediaController: File upload handling

**Pattern**: Controllers inject feature use cases directly, convert DTOs, delegate execution

**Security**: Single organization scope extracted from `@AuthenticationPrincipal CustomUserDetails`

#### `agency` - Agency API

**Endpoints**: `/api/agency/clients/{clientId}/*`

**Target Audience**: Agencies managing multiple client accounts

**Architecture**: Thin orchestration with multi-tenant isolation

**Key Differences vs Business**:
- Client ID required in all endpoints
- Client-scoped operations with scope validation
- Cross-client views (agency-wide social accounts)
- Client access delegation for agency users

**Multi-Tenancy**:
- User scopes include `clientId` for agency users
- Scope validation in every controller method
- Activity logs filterable by client
- Member invitations support client-scoped roles

## Module Communication

### Event-Driven Architecture

**Spring Modulith Events** via `ApplicationEventPublisher`:

```java
// Feature publishes event
events.publishEvent(new PostCreatedEvent(postId, organizationId));

// Other module listens
@ApplicationModuleListener
void on(PostCreatedEvent event) {
    // React to event
}
```

**Benefits**:
- No direct dependencies between features
- Loose coupling
- Temporal decoupling (async event processing)
- Event persistence support

### Shared Data Access

Features access `database` module directly:
- Import repositories from `database.repository`
- NO direct access to other feature internals
- Shared DTOs via `shared.dto`

## Cross-Cutting Concerns

### Activity Logging

**@UseCase Annotation**: Automatic AOP-based logging

```java
@UseCase(
    resourceType = "POST",
    description = "Creates post",
    logActivity = true
)
```

**Logged Information**:
- User ID, organization ID, client ID
- Resource type and ID
- Action timestamp
- HTTP request context (IP, user agent)
- Performance metrics

### Multi-Tenancy

**Tenant Context**: Thread-local storage via TenantContextHolder

**Scope Validation**: Gateway validates JWT claims against requested resources

**Database Isolation**: Queries filtered by organization_id/client_id

### Error Handling

**Custom Exceptions** (shared module):
- Auth: UserNotFoundException, InvalidCredentialsException, TokenExpiredException
- Social: SocialAccountNotFoundException, OAuth2AuthenticationException
- Resources: ResourceNotFoundException, InsufficientPermissionsException

**Global Exception Handler**: Converts exceptions to standardized API responses

## Design Patterns

### Domain-Driven Design
- Aggregates: Post, SocialAccount, WorkflowInstance
- Value Objects: PostId, Content, OAuth2Token
- Domain Events: PostCreatedEvent, UserProfileAnalyzedEvent

### Ports & Adapters (Hexagonal)
- 15 port interfaces in domain layers
- Infrastructure adapters implement ports
- Dependency inversion (domain owns interfaces)

### Strategy Pattern
- Analysis strategies (writing style, content themes, image)
- Compensation strategies (ALL, CRITICAL_ONLY, BEST_EFFORT)
- Execution modes (sync, async thread, async native)

### Repository Pattern
- Spring Data repositories abstract persistence
- Domain uses repository interfaces
- Infrastructure provides JPA implementations

### Facade Pattern
- CoreFacade exposes public API of core module
- Hides internal complexity from other modules

## Module Dependency Graph

```
┌─────────────────────────────────────────────┐
│           business / agency                 │ API REST Modules
│        (Thin Orchestration)                 │
└──────────────┬──────────────────────────────┘
               │ calls use cases
               ▼
┌──────────────────────────────────────────────┐
│  features.postmanagement                     │
│  features.socialaccounts                     │ Feature Modules
│  (+ planned: contentgen, editorial, etc.)    │
└──────────────┬───────────────────────────────┘
               │ may use core for scraping
               ▼
┌──────────────────────────────────────────────┐
│            core                              │ Core Module
│  (Scraping, AI, Workflow Engine)             │
└──────────────┬───────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────┐
│          database                            │ Persistence
│    (JPA Entities, Repositories)              │
└──────────────────────────────────────────────┘

Cross-cutting: shared (accessible by all)
              gateway (security filter chain)
```

## Module Boundary Validation

**Spring Modulith Validation**: Enforced at compile-time and runtime

**ModularityTests** (MUST pass before commit):
- `verifyModularity()`: Validates no unauthorized cross-module imports
- `verifyNoCycles()`: Detects circular dependencies
- `generateModularityDocumentation()`: Creates PlantUML diagrams

**Shared Modules Declaration**:
```java
@Modulith(sharedModules = {"shared", "database"})
```

## Architectural Decisions

### Why Clean Architecture for Some Features?
- **postmanagement**: Complex domain logic, cross-platform publishing
- **socialaccounts**: OAuth2 flows, token lifecycle management
- **core**: External API orchestration with resilience patterns

### Why Spring Classic for Others?
- Simple CRUD operations without complex business rules
- Scheduler-based tasks (publishing, analytics)
- Planned but not yet implemented (publishing, weeklybrief, analytics)

### Why Modular Monolith?
- Team velocity: Deploy as single artifact
- Module boundaries prevent big ball of mud
- Easy refactoring: Move to microservices if needed
- Spring Modulith provides governance without distribution costs

### Why Event-Driven Communication?
- Loose coupling between features
- Prevents feature-to-feature dependencies
- Supports future async processing
- Audit trail of domain events

## Future Architectural Considerations

### Migration Path to Microservices
If scaling requires distribution:
1. Each feature is already a bounded context
2. Spring Modulith events → message broker (Kafka, RabbitMQ)
3. Extract modules to separate deployables
4. API gateway routes to services

### Planned Features Implementation
When implementing `contentgeneration`, `editorialcalendar`:
- Follow Clean Architecture pattern (already scaffolded)
- Define domain models and ports
- Implement use cases with `@UseCase` annotation
- Publish domain events for inter-feature communication
- NO direct dependencies on other features

### Performance Optimization
- Implement caching at use case level
- Optimize N+1 queries with JOIN FETCH
- Consider read models for complex queries (CQRS)
- Async processing for long-running workflows

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.7 application built with Java 21 using Gradle. The project name is `postiqa-back-v2` and is in the `fr.postiqa` package namespace. It's a feature-rich backend application leveraging Spring ecosystem technologies including Spring AI, Spring Modulith, Spring Batch, and Spring Security.

## Core Technology Stack

- **Java 21** - Language version (managed via Gradle toolchain)
- **Spring Boot 3.5.7** - Core framework
- **Spring Modulith** - Modular monolith architecture support
- **Spring AI 1.0.3** - AI integration (OpenAI, vector stores, document readers)
- **PostgreSQL with pgvector** - Primary database with vector search capabilities
- **Redis** - Caching and data structure store
- **Liquibase** - Database migrations
- **Lombok** - Reduces boilerplate code
- **GraalVM Native Image** - Optional native compilation support

## Build Commands

All commands use the Gradle wrapper (`./gradlew`):

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Run tests in native mode (requires GraalVM)
./gradlew nativeTest

# Clean build artifacts
./gradlew clean
```

## Docker and Infrastructure

The project includes `compose.yaml` with development services:
- **PostgreSQL with pgvector** (port 5432) - credentials: myuser/secret, database: mydatabase
- **Redis** (port 6379)

Spring Boot's Docker Compose support automatically starts these services during development.

```bash
# Build Docker image using Cloud Native Buildpacks
./gradlew bootBuildImage

# Run the containerized application
docker run --rm -p 8080:8080 postiqa-back-v2:0.0.1-SNAPSHOT
```

## GraalVM Native Image

For native compilation:

```bash
# Compile to native executable (requires GraalVM 22.3+)
./gradlew nativeCompile

# Run the native executable
./build/native/nativeCompile/postiqa-back-v2
```

## Architecture

### Modular Monolith with Spring Modulith

The application is a **package-based modular monolith** (NOT multi-module Gradle). Spring Modulith enforces module boundaries, provides observability, and validates architectural rules at compile-time and runtime.

**Important**: This is a single Gradle module with packages organized as Spring Modulith modules under `fr.postiqa`.

### Module Structure

The application is organized into **8 Spring Modulith modules**:

#### 1. Foundation Modules (Shared Across All)

- **`shared`** - DTOs (45), exceptions (25), custom annotations (@UseCase, @LogActivity)
  - Custom `@UseCase` annotation combines @Component with activity logging and tenant tracking
  - Enums: SocialPlatform, PostStatus, PostType, MediaType

- **`database`** - 22 JPA entities, Spring Data repositories
  - Auth entities: UserEntity, RoleEntity, PermissionEntity, ApiKeyEntity, RefreshTokenEntity
  - Organization entities: OrganizationEntity, ClientEntity, OrganizationMemberEntity
  - Social entities: SocialAccountEntity, PostEntity, PostChannelEntity, MediaEntity
  - Workflow entities: WorkflowInstanceEntity, WorkflowStepExecutionEntity
  - **NO BUSINESS LOGIC** - Pure persistence layer

- **`gateway`** - Authentication, authorization, organization management
  - **17 use cases** for auth and org management (InviteMemberUseCase, UpdateMemberRoleUseCase, etc.)
  - Triple auth: JWT (stateless), API Key, OAuth2 (Google, GitHub, LinkedIn)
  - Multi-tenant security with scope validation
  - Custom permission evaluator with resource-action pattern

#### 2. Core Module (Clean Architecture)

- **`core`** - Scraping orchestration + AI analysis + workflow engine
  - **12 use cases**: 5 scraping (GetSocialPostsUseCase, AnalyzeUserProfileUseCase), 7 workflow
  - **DAG-based workflow engine**: Sequential/parallel execution, retry, timeout, compensation
  - **External integrations**: Apify (scraping), OpenAI (AI analysis)
  - Supports sync/async execution modes with caching (30min TTL)

#### 3. Features Modules

**Implemented Features (Clean Architecture)**:

- **`features.postmanagement`** - Post lifecycle management
  - **11 use cases**: CRUD, scheduling, media upload, channel management
  - Domain models: Post, Channel (value objects: PostId, Content, Media)
  - Event publication: PostCreatedEvent, PostScheduledEvent, PostPublishedEvent

- **`features.socialaccounts`** - Social platform connectivity
  - **7 use cases**: OAuth2 flow, token management, connection testing
  - Platforms: LinkedIn, Twitter, Instagram, YouTube, TikTok
  - Automatic token refresh scheduler (hourly)

**Planned Features (Directories only, no implementation)**:

- **`features.contentgeneration`** - AI-powered content generation (NOT IMPLEMENTED)
- **`features.editorialcalendar`** - Editorial strategy (NOT IMPLEMENTED)
- **`features.publishing`** - Post publication orchestration (EMPTY)
- **`features.weeklybrief`** - Audio transcription workflow (EMPTY)
- **`features.analytics`** - Performance metrics (EMPTY)

#### 4. API REST Modules

- **`business`** - API pour entreprises (single organization)
  - **Endpoints**: `/api/business/posts`, `/social-accounts`, `/organization`
  - Thin orchestration layer calling feature use cases directly

- **`agency`** - API pour agences (multi-client)
  - **Endpoints**: `/api/agency/clients/{clientId}/posts`, `/clients/{clientId}/social-accounts`
  - Client-scoped operations with multi-tenant isolation
  - Client access delegation for agency users

### Module Dependencies

```
business/agency → features/* → core → database → shared
                              ↓
                           gateway
```

**Shared modules** (accessibles par tous):
- `shared`
- `database`

**Rules**:
- Features ne doivent PAS se dépendre entre elles
- API modules (business/agency) orchestrent les features, AUCUNE logique métier
- Core orchestre uniquement des APIs externes (pas de scraping/IA custom)

### Architectural Patterns

- **Clean Architecture**: `core`, `features.postmanagement`, `features.socialaccounts` (domain/port/usecase/adapter/infrastructure)
- **Ports & Adapters**: 15 port interfaces for external boundaries
- **Event-Driven**: Spring Modulith ApplicationEventPublisher for inter-module communication
- **Multi-Tenancy**: Organization/Client scoping with JWT claims, scope validation in gateway
- **External API Orchestration**: Apify (scraping), OpenAI (AI), OAuth2 providers (no custom scraping/AI)
- **Workflow Engine**: DAG-based execution with resilience patterns (retry, timeout, compensation)
- **Security**: Stateless JWT + API Key + OAuth2, RBAC with permission overrides
- **Database**: PostgreSQL with pgvector, JSONB for flexible metadata, UUID primary keys

## Important Configuration

**Build System**: Uses Gradle with **Kotlin DSL** (`build.gradle.kts`, `settings.gradle.kts`). All configuration should use Kotlin syntax.

**Database Migration**: Uses Liquibase for schema management. Changelog files should be placed in `src/main/resources/db/changelog/`.

**Application Properties**: Located at `src/main/resources/application.properties`.

Current configuration:
```properties
spring.application.name=postiqa-back-v2

# Spring Modulith
spring.modulith.validation.enabled=true
spring.modulith.events.enabled=true
spring.modulith.observability.enabled=true
```

Test configuration in `src/test/resources/application-test.properties`:
- H2 in-memory database
- Liquibase disabled (uses JPA DDL auto)
- Redis disabled
- Security auto-configuration disabled

**Lombok**: The project uses Lombok for reducing boilerplate. Annotations are processed at compile time via `annotationProcessor`.

**Hibernate**: Association management is enabled, which allows bidirectional relationships to be automatically managed.

**Spring Modulith**:
- Module validation at runtime enabled
- Event publication enabled for inter-module communication
- Observability enabled for monitoring module interactions
- Documentation generation available (commented out)

## Development

**DevTools**: Spring Boot DevTools is included for automatic restart during development.

**Hot Reload**: Docker Compose services are automatically managed during development, no manual start/stop needed.

**Actuator**: Spring Boot Actuator endpoints are available for monitoring and management. Spring Modulith adds observability features.

## Development Rules & Conventions

### Module Boundaries (CRITICAL)

1. **NO direct dependencies between features**
   - ❌ `features.contentgeneration` cannot import from `features.editorialcalendar`
   - ✅ Use Spring Modulith events for inter-feature communication
   - ✅ Share data via `shared` module only

2. **API modules are orchestration only**
   - ❌ NO business logic in `business` or `agency` controllers
   - ✅ Controllers call feature adapters/services only
   - ✅ Multi-tenant logic in `agency` is acceptable (infrastructure concern)

3. **Database module isolation**
   - ❌ NO business logic in entities or repositories
   - ✅ Pure JPA mapping only
   - ✅ Use repository methods for data access

4. **Clean Architecture layers (core, postmanagement, socialaccounts)**
   - ❌ `domain` NEVER depends on `infrastructure`
   - ❌ `usecase` NEVER depends on `adapter` or `infrastructure`
   - ✅ Dependency flow: infrastructure → adapter → usecase → domain
   - ✅ All use cases annotated with `@UseCase` for automatic activity logging

### Naming Conventions

- **Packages**: lowercase, no separators (`contentgeneration` not `content-generation`)
- **Classes**: PascalCase
- **Ports (interfaces)**: Suffix with `Port` (e.g., `ScrapingPort`, `AnalysisPort`)
- **Use Cases**: Suffix with `UseCase` (e.g., `AnalyzeUserProfileUseCase`)
- **Adapters IN**: Suffix with `Adapter` (e.g., `AnalysisAdapter`)
- **Adapters OUT**: Suffix with `Gateway` (e.g., `OpenAIGateway`, `ApifyGateway`)

### API External Integration

**IMPORTANT**: This project does NOT implement custom scraping or AI.

- ✅ Use **Apify** or **Bright Data** for scraping (via HTTP clients)
- ✅ Use **OpenAI** or **Anthropic** for AI analysis
- ✅ Use **Whisper API** for transcription
- ❌ NEVER implement custom scraping logic
- ❌ NEVER implement custom AI/ML models

Clients go in `*/infrastructure/client/` packages.

### Spring Modulith Events

For inter-module communication:

```java
// Publishing event
events.publishEvent(new UserProfileAnalyzedEvent(userId, profile));

// Listening in another module
@ApplicationModuleListener
void on(UserProfileAnalyzedEvent event) { ... }
```

## Testing

Test location: `src/test/java/fr/postiqa/`

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "fr.postiqa.ClassName"

# Run tests with specific pattern
./gradlew test --tests "*TestPattern*"

# Verify module boundaries (CRITICAL - run before every commit)
./gradlew test --tests "fr.postiqa.ModularityTests.verifyModularity"
```

### Testing Strategy

**Current State**: Minimal test coverage (2 test classes only)

- **`ModularityTests`**: Validates module boundaries, detects cycles, generates docs (CRITICAL - run before commit)
- **`PostiqaBackV2ApplicationTests`**: Basic smoke test

**Test Configuration** (application-test.properties):
- H2 in-memory database with JPA `create-drop` (Liquibase disabled)
- Redis, Security, Spring AI disabled for isolated testing

**TODO**: Implement unit/integration tests for use cases, adapters, and controllers.

## Spring AI Features

The application integrates Spring AI with:
- **OpenAI** integration for chat/completion
- **PGvector** for vector storage and similarity search
- **Document readers** for PDF and Markdown processing
- **MCP Server** support via WebMVC
- Vector store advisors for RAG patterns

When working with AI features, ensure proper configuration of OpenAI API keys and vector store connections.

## Security

**Authentication** (3 mechanisms):
- **JWT**: Stateless Bearer tokens (15min access, 30d refresh) with HMAC-SHA, custom claims (user_id, organization_id, client_id)
- **API Key**: Header-based `X-API-Key` for machine-to-machine
- **OAuth2**: Social login (Google, GitHub, LinkedIn) with custom success handler

**Authorization**:
- **RBAC**: Role-permission mapping with `ROLE_` prefix
- **Resource-Action Permissions**: `POST:CREATE`, `POST:*` with wildcard support
- **Permission Overrides**: Database-driven custom grants/revocations per user
- **Scope Validation**: Multi-tenant isolation via organization_id/client_id in JWT
- **Method Security**: `@PreAuthorize` with CustomPermissionEvaluator

**Filter Chain**: ApiKeyFilter → JwtFilter → TenantResolutionFilter → Spring Security

See `gateway/config/SecurityConfig.java` for configuration details.

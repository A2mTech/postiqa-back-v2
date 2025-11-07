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

The application is organized into 11 Spring Modulith modules:

#### 1. Foundation Modules (Shared Across All)

- **`shared`** - DTOs, exceptions, utils, annotations partagés
  - `shared/dto/` - Data Transfer Objects
  - `shared/exception/` - Custom exceptions
  - `shared/util/` - Utility classes
  - `shared/annotation/` - Custom annotations

- **`database`** - Entities JPA et repositories Spring Data uniquement
  - `database/entity/` - JPA entities
  - `database/repository/` - Spring Data repositories
  - **NO BUSINESS LOGIC** - Pure persistence layer

- **`gateway`** - Authentification et sécurité
  - `gateway/config/` - Spring Security configuration
  - `gateway/filter/` - Security filters (JWT, CORS)
  - `gateway/auth/` - Authentication services

#### 2. Core Module (Clean Architecture)

- **`core`** - Orchestration scraping + analyse IA
  - **Architecture**: Clean Architecture complète
  - **Responsabilité**: Scraper posts sociaux, analyser style d'écriture
  - **Structure**:
    - `core/domain/model/` - Entities métier, value objects
    - `core/domain/port/` - Interfaces (ScrapingPort, AnalysisPort)
    - `core/usecase/` - Business logic (AnalyzeUserProfileUseCase)
    - `core/adapter/in/` - Adapters entrants
    - `core/adapter/out/` - Adapters sortants
    - `core/infrastructure/client/` - Clients API externes (Apify, Bright Data, OpenAI)
    - `core/infrastructure/config/` - Configurations

#### 3. Features Modules

**Features Clean Architecture** (logique métier complexe):

- **`features.contentgeneration`** - Génération de posts avec learning
  - **Architecture**: Clean Architecture (domain/usecase/adapter/infrastructure)
  - **Responsabilité**: Générer posts dans le style utilisateur, apprendre des modifications

- **`features.editorialcalendar`** - Stratégie éditoriale dynamique
  - **Architecture**: Clean Architecture (domain/usecase/adapter/infrastructure)
  - **Responsabilité**: Créer stratégie hebdomadaire basée sur profil + business + objectifs

**Features Spring Classique**:

- **`features.publishing`** - Programmation et publication
  - `publishing/service/` - Services de publication
  - `publishing/scheduler/` - Schedulers automatiques

- **`features.weeklybrief`** - Brief vocal hebdomadaire
  - **Responsabilité**: Transcription audio → extraction événements → génération posts
  - `weeklybrief/service/` - Orchestration du workflow
  - `weeklybrief/transcription/` - Whisper API integration
  - `weeklybrief/extraction/` - Extraction d'événements via IA

- **`features.analytics`** - Analyse de performance
  - `analytics/service/` - Services d'analyse
  - `analytics/metrics/` - Calcul de métriques

#### 4. API REST Modules

- **`business`** - API REST pour entreprises
  - **Endpoints**: `/api/business/*`
  - **Responsabilité**: Orchestrer features pour entreprises gérant leur propre compte
  - `business/controller/` - REST controllers
  - `business/config/` - Configurations

- **`agency`** - API REST pour agences
  - **Endpoints**: `/api/agency/*`
  - **Responsabilité**: Orchestrer features en mode multi-clients
  - `agency/controller/` - REST controllers
  - `agency/config/` - Configurations
  - `agency/tenant/` - Gestion multi-tenant

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

- **Clean Architecture**: Appliquée sur `core`, `features.contentgeneration`, `features.editorialcalendar`
- **Spring Classique**: Pour features simples et API REST modules
- **Ports & Adapters**: Interfaces domain, implémentations infrastructure
- **API Orchestration**: Core et features orchestrent des APIs externes uniquement (Apify, OpenAI, Whisper, etc.)
- **JPA Entities**: Hibernate enhancements avec association management activé
- **Spring Security**: Couche sécurité centralisée dans gateway
- **Event-Driven**: Spring Modulith events pour communication inter-modules

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

4. **Clean Architecture layers (core, contentgeneration, editorialcalendar)**
   - ❌ `domain` NEVER depends on `infrastructure`
   - ❌ `usecase` NEVER depends on `adapter` or `infrastructure`
   - ✅ Dependency flow: infrastructure → adapter → usecase → domain

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

### Package-info.java

Every Spring Modulith module MUST have:

```java
@org.springframework.modulith.ApplicationModule
package fr.postiqa.modulename;
```

Mark shared modules in main class:
```java
@Modulith(sharedModules = {"shared", "database"})
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

- **`ModularityTests`**: Validates module boundaries (MUST pass)
- **Unit tests**: In same package as code under test
- **Integration tests**: Use `@SpringBootTest` with `@ActiveProfiles("test")`
- **H2 in-memory DB**: For tests (configured in `application-test.properties`)

Spring Modulith test support is included for module boundary testing.

## Spring AI Features

The application integrates Spring AI with:
- **OpenAI** integration for chat/completion
- **PGvector** for vector storage and similarity search
- **Document readers** for PDF and Markdown processing
- **MCP Server** support via WebMVC
- Vector store advisors for RAG patterns

When working with AI features, ensure proper configuration of OpenAI API keys and vector store connections.

## Security

Spring Security is configured. Authentication and authorization should be implemented according to the application's security requirements. Spring Security test support is available for testing secured endpoints.

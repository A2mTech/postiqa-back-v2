# Infrastructure Documentation

## Overview

Postiqa Back v2 uses PostgreSQL with pgvector, Redis for caching, and external APIs for scraping and AI. All infrastructure is containerized via Docker Compose for local development.

## Technology Stack

### Core Framework
- **Spring Boot**: 3.5.7
- **Java**: 21 (Gradle toolchain)
- **Spring Modulith**: 1.4.4
- **Spring AI**: 1.0.3

### Data Storage
- **PostgreSQL**: 16 with pgvector extension
- **Redis**: Latest (caching layer)
- **Liquibase**: Schema migrations

### External Services
- **Apify**: Web scraping (LinkedIn, Instagram, TikTok actors)
- **OpenAI**: AI analysis (GPT-4-turbo-preview)
- **OAuth2 Providers**: Google, GitHub, LinkedIn

### Build & Deploy
- **Gradle**: Kotlin DSL with GraalVM native support
- **Docker**: Cloud Native Buildpacks via Spring Boot
- **AWS S3 SDK**: 2.20.56 for Cloudflare R2 storage

## Local Development Setup

### Docker Compose Services

The `compose.yaml` defines two services:

```yaml
PostgreSQL:
  - Image: pgvector/pgvector:16
  - Port: 5432
  - Database: mydatabase
  - User: myuser
  - Password: secret

Redis:
  - Image: redis:latest
  - Port: 6379
```

**Auto-start**: Spring Boot Docker Compose support automatically manages these services during `./gradlew bootRun`.

### Database Configuration

**Production** (via Docker Compose):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mydatabase
spring.datasource.username=myuser
spring.datasource.password=secret
spring.jpa.hibernate.ddl-auto=validate
spring.liquibase.enabled=true
```

**Test** (H2 in-memory):
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
spring.liquibase.enabled=false
```

### Redis Configuration

**Caching Strategy**:
- Simple cache type with named caches
- Cache names: `socialPosts`, `socialProfiles`, `websiteContent`, `scrapingJobs`
- TTL: 30 minutes for scraping results

## External API Integrations

### Apify Configuration

```properties
apify.api.base-url=https://api.apify.com/v2
apify.api.key=${APIFY_API_KEY}
apify.timeout=300s
apify.polling-interval=5s
apify.max-retries=3
```

**Actor Registry**:
- LinkedIn: apify/linkedin-profile-scraper
- Instagram: apify/instagram-profile-scraper
- TikTok: apify/tiktok-scraper
- Twitter: apify/twitter-scraper
- YouTube: apify/youtube-scraper

**Execution Modes**:
- Sync: `/run-sync` endpoint (300s max)
- Async: `/runs` endpoint with polling

### OpenAI Configuration

```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.base-url=https://api.openai.com
spring.ai.openai.chat.options.model=gpt-4-turbo-preview
spring.ai.openai.chat.options.temperature=0.7
spring.ai.openai.chat.options.max-tokens=2000
```

**Analysis Strategies**:
- Writing style analysis
- Content themes extraction
- Image analysis (vision model)
- Multimodal analysis

### OAuth2 Providers

Configured in `application.properties`:
```properties
spring.security.oauth2.client.registration.google.*
spring.security.oauth2.client.registration.github.*
spring.security.oauth2.client.registration.linkedin.*
```

## Database Schema Management

### Liquibase Migrations

**Master Changelog**: `src/main/resources/db/changelog/db.changelog-master.xml`

**Current Migrations**:
- `001-post-management-tables.xml`: Posts, channels, media tables with JSONB metadata

**Migration Characteristics**:
- UUID primary keys
- JSONB columns for flexible metadata
- Comprehensive indexing (status, dates, foreign keys)
- CASCADE delete for parent-child relationships

**⚠️ Production Gap**: Only 4 changesets exist. 75% of entities (auth, organization, workflow) lack migrations.

### Database Schema

**22 JPA Entities**:

1. **Auth & Security** (8): UserEntity, RoleEntity, PermissionEntity, ApiKeyEntity, RefreshTokenEntity, OAuthConnectionEntity, PasswordResetTokenEntity, EmailVerificationTokenEntity

2. **Organization & Multi-tenancy** (6): OrganizationEntity, ClientEntity, OrganizationMemberEntity, OrganizationInvitationEntity, UserPermissionOverrideEntity, ActivityLogEntity

3. **Social Media** (4): SocialAccountEntity, PostEntity, PostChannelEntity, MediaEntity

4. **Workflow** (2): WorkflowInstanceEntity, WorkflowStepExecutionEntity

**PostgreSQL Features**:
- pgvector extension for vector similarity search
- JSONB for semi-structured data (platformMetadata, workflow context)
- UUID v4 for distributed-friendly IDs
- Optimistic locking with `@Version` for concurrency

## Build & Deployment

### Gradle Build

```bash
# Standard build
./gradlew build

# Docker image with Cloud Native Buildpacks
./gradlew bootBuildImage

# Native compilation (requires GraalVM)
./gradlew nativeCompile
```

**Build Plugins**:
- Spring Boot plugin with buildpack support
- GraalVM native buildtools (0.10.6)
- Hibernate enhancement plugin
- Lombok annotation processor

### Configuration Management

**Environment Variables Required**:
```bash
APIFY_API_KEY=your_apify_key
OPENAI_API_KEY=your_openai_key
JWT_SECRET=your_jwt_secret
OAUTH2_CLIENT_ID=your_oauth_client_id
OAUTH2_CLIENT_SECRET=your_oauth_secret
```

**Profiles**:
- `default`: Production configuration
- `test`: Test configuration with H2 database

## Async Execution

**Thread Pool Configuration**:
```properties
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=25
```

Used for:
- Async scraping jobs
- Workflow parallel execution
- Token refresh scheduler

## Monitoring & Observability

### Spring Boot Actuator

Endpoints available at `/actuator/*`:
- Health, metrics, info
- Spring Modulith observability (module interactions)

### Spring Modulith Observability

```properties
spring.modulith.observability.enabled=true
spring.modulith.events.enabled=true
```

Tracks:
- Inter-module event publication
- Module boundary violations
- Module dependency graph

## Security Infrastructure

### JWT Token Management

**Token Types**:
- Access Token: 15 minutes TTL
- Refresh Token: 30 days TTL

**JWT Claims**:
```json
{
  "user_id": "uuid",
  "organization_id": "uuid",
  "client_id": "uuid",
  "roles": ["ROLE_USER"],
  "permissions": ["POST:CREATE", "POST:UPDATE"]
}
```

**Signing**: HMAC-SHA with configurable secret

### Filter Chain Order

1. **ApiKeyAuthenticationFilter**: Intercepts `X-API-Key` header
2. **JwtAuthenticationFilter**: Validates Bearer tokens
3. **TenantResolutionFilter**: Extracts tenant context from JWT
4. **Spring Security FilterChain**: Standard authorization

## Performance Considerations

### Caching Strategy
- Social posts: 30min TTL (high churn rate)
- Scraping jobs: 30min TTL (poll completion)
- Token refresh: Automatic hourly check

### Database Optimization
- Indexed foreign keys for joins
- Indexed status columns for filtering
- Lazy loading for @ManyToOne relationships
- Batch operations support via Spring Batch

### Connection Pooling
- Default HikariCP configuration
- Tune based on concurrent user load

## Production Readiness Checklist

- [ ] Complete Liquibase migrations for all entities
- [ ] Configure production-grade JWT secret (256-bit minimum)
- [ ] Harden CORS configuration (wildcard currently allowed)
- [ ] Set up pgvector indexes for AI similarity search
- [ ] Configure Redis persistence (RDB/AOF)
- [ ] Implement comprehensive monitoring and alerting
- [ ] Load test async execution pools
- [ ] Secure external API keys (secret management system)
- [ ] Configure OAuth2 callback URLs for production domains
- [ ] Review and optimize database indexes based on query patterns

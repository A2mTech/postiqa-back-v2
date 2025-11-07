# Architecture Postiqa - Vue d'ensemble

## Structure Modulaire (Spring Modulith)

```
fr.postiqa/
│
├── 📦 SHARED MODULES (accessibles par tous)
│   ├── shared/                       # DTOs, exceptions, utils, annotations
│   └── database/                     # Entities JPA + Repositories Spring Data
│
├── 🔐 GATEWAY MODULE
│   └── gateway/                      # Spring Security, Auth, Filtres
│
├── 🎯 CORE MODULE (Clean Architecture)
│   └── core/
│       ├── domain/
│       │   ├── model/                # Entities métier, Value Objects
│       │   └── port/                 # ScrapingPort, AnalysisPort (interfaces)
│       ├── usecase/                  # AnalyzeUserProfileUseCase
│       ├── adapter/
│       │   ├── in/                   # Exposer use cases
│       │   └── out/                  # Implémenter ports
│       └── infrastructure/
│           ├── client/               # Apify, Bright Data, OpenAI clients
│           └── config/               # Configurations
│
├── 🎨 FEATURES MODULES
│   │
│   ├── features.contentgeneration/   # Clean Architecture
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   └── port/                 # GenerationPort, LearningPort
│   │   ├── usecase/                  # GeneratePostUseCase, LearnFromEditUseCase
│   │   ├── adapter/
│   │   │   ├── in/
│   │   │   └── out/
│   │   └── infrastructure/
│   │
│   ├── features.editorialcalendar/   # Clean Architecture
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   └── port/                 # StrategyPort
│   │   ├── usecase/                  # GenerateStrategyUseCase
│   │   ├── adapter/
│   │   │   ├── in/
│   │   │   └── out/
│   │   └── infrastructure/
│   │
│   ├── features.publishing/          # Spring Classique
│   │   ├── service/                  # PublishingService
│   │   └── scheduler/                # ScheduledPublisher
│   │
│   ├── features.weeklybrief/         # Spring Classique
│   │   ├── service/                  # BriefService
│   │   ├── transcription/            # WhisperClient
│   │   └── extraction/               # EventExtractor
│   │
│   └── features.analytics/           # Spring Classique
│       ├── service/                  # AnalyticsService
│       └── metrics/                  # MetricsCalculator
│
└── 🌐 API REST MODULES
    ├── business/                     # /api/business/*
    │   ├── controller/               # REST controllers (orchestration)
    │   └── config/
    │
    └── agency/                       # /api/agency/*
        ├── controller/               # REST controllers (orchestration)
        ├── config/
        └── tenant/                   # Multi-tenant logic
```

## Flux de Dépendances

```
┌─────────────────────────────────────────────────────────┐
│                    API REST LAYER                       │
│  ┌──────────────┐              ┌──────────────┐        │
│  │   business   │              │    agency    │        │
│  └──────┬───────┘              └──────┬───────┘        │
│         │                              │                │
└─────────┼──────────────────────────────┼────────────────┘
          │                              │
          ▼                              ▼
┌─────────────────────────────────────────────────────────┐
│                    FEATURES LAYER                       │
│  ┌────────────┐ ┌────────────┐ ┌──────────────────┐   │
│  │  content   │ │ editorial  │ │  publishing       │   │
│  │ generation │ │  calendar  │ │  weeklybrief      │   │
│  │            │ │            │ │  analytics        │   │
│  └─────┬──────┘ └─────┬──────┘ └────────┬─────────┘   │
└────────┼──────────────┼──────────────────┼─────────────┘
         │              │                  │
         ▼              ▼                  ▼
    ┌────────────────────────────────────────┐
    │          CORE MODULE                   │
    │    (Scraping + Analysis)               │
    └────────────────┬───────────────────────┘
                     │
                     ▼
    ┌────────────────────────────────────────┐
    │      SHARED MODULES LAYER              │
    │  ┌──────────┐        ┌──────────┐     │
    │  │ database │        │  shared  │     │
    │  └──────────┘        └──────────┘     │
    └────────────────────────────────────────┘
                     ▲
                     │
    ┌────────────────┴───────────────────────┐
    │         GATEWAY MODULE                 │
    │      (Security, Auth, Filters)         │
    └────────────────────────────────────────┘
```

## Règles Architecturales

### ✅ AUTORISÉ

1. **Features → Core** : Les features peuvent utiliser le core
2. **Tous → Shared/Database** : Tous les modules peuvent utiliser shared et database
3. **API → Features** : Les API REST orchestrent les features
4. **Events inter-modules** : Communication via Spring Modulith events

### ❌ INTERDIT

1. **Features → Features** : Aucune feature ne peut dépendre d'une autre feature
2. **Core → Features** : Le core ne connaît pas les features
3. **Database → Tout** : Le module database n'importe rien (sauf shared)
4. **Logique métier dans API** : Les modules business/agency sont purement orchestration

## Clean Architecture (core, contentgeneration, editorialcalendar)

```
┌────────────────────────────────────────────────────┐
│                  INFRASTRUCTURE                    │
│  ┌──────────────────────────────────────────────┐ │
│  │ Clients API (Apify, OpenAI, Whisper, etc.)  │ │
│  └──────────────────────────────────────────────┘ │
└────────────┬───────────────────────────────────────┘
             │ implements
             ▼
┌────────────────────────────────────────────────────┐
│                    ADAPTER                         │
│  ┌────────────┐              ┌────────────┐       │
│  │ Adapter IN │              │ Adapter OUT│       │
│  │ (expose    │              │ (implement │       │
│  │ use cases) │              │   ports)   │       │
│  └────────────┘              └────────────┘       │
└────────────┬───────────────────────────────────────┘
             │ uses
             ▼
┌────────────────────────────────────────────────────┐
│                    USE CASE                        │
│  ┌──────────────────────────────────────────────┐ │
│  │ Business Logic (orchestration)               │ │
│  └──────────────────────────────────────────────┘ │
└────────────┬───────────────────────────────────────┘
             │ uses
             ▼
┌────────────────────────────────────────────────────┐
│                     DOMAIN                         │
│  ┌────────────┐              ┌────────────┐       │
│  │   Model    │              │   Ports    │       │
│  │ (entities, │              │(interfaces)│       │
│  │   VOs)     │              │            │       │
│  └────────────┘              └────────────┘       │
│                                                    │
│  ⚠️  NO DEPENDENCIES - Pure Java                  │
└────────────────────────────────────────────────────┘
```

## Orchestration d'APIs Externes

**PRINCIPE CLÉ** : Ce projet n'implémente PAS de scraping custom ni d'IA custom.

### APIs Externes Utilisées

| Fonctionnalité | Service Externe | Package |
|----------------|-----------------|---------|
| Scraping LinkedIn/Twitter/Instagram | **Apify** ou **Bright Data** | `core/infrastructure/client/` |
| Analyse IA / Génération | **OpenAI** ou **Anthropic** | `*/infrastructure/client/` |
| Transcription audio | **Whisper API** | `weeklybrief/transcription/` |
| Extraction PDF carrousels | Bibliothèque PDF | `core/infrastructure/` |

### Exemple d'Implémentation

```java
// ❌ INTERDIT - Scraping custom
public class CustomLinkedInScraper { ... }

// ✅ AUTORISÉ - Client API Apify
@Component
public class ApifyScrapingGateway implements ScrapingPort {
    private final RestTemplate apifyClient;

    @Override
    public List<ScrapedPost> scrapeUserPosts(Platform platform, String userId) {
        // Appel API Apify
        return apifyClient.postForObject(...);
    }
}
```

## Communication Inter-Modules (Events)

```java
// Module A publie un événement
@Service
public class SomeService {
    private final ApplicationEventPublisher events;

    public void doSomething() {
        events.publishEvent(new UserProfileAnalyzedEvent(userId, profile));
    }
}

// Module B écoute l'événement
@Component
public class SomeOtherService {

    @ApplicationModuleListener
    void on(UserProfileAnalyzedEvent event) {
        // Réagir à l'événement
    }
}
```

## Validation Architecturale

Le projet inclut `ModularityTests` qui vérifie automatiquement :

- ✅ Respect des frontières entre modules
- ✅ Pas de cycles de dépendances
- ✅ Dépendances conformes au graphe

**À exécuter AVANT chaque commit** :
```bash
./gradlew test --tests "fr.postiqa.ModularityTests.verifyModularity"
```

---

**Date de création** : 2025-11-06
**Architecture** : Spring Modulith - Monolithe Modulaire Package-Based
**Style** : Clean Architecture (modules complexes) + Spring Classique (modules simples)

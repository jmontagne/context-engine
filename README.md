# Context Engine — Cross-Model Context Compaction POC

> **Concept:** *Context Engineering* replaces simple Prompt Engineering.
> Instead of sending the full conversation history to an expensive LLM, a cheaper model first **distills** the context — reducing tokens, cost, and noise.

## The Problem

As agentic systems grow, so does the conversation history. Sending 10k+ tokens of chat history to a frontier model on every turn is:
- **Expensive** — input tokens are billed, and history grows linearly
- **Noisy** — greetings, filler, and repetition dilute signal
- **Slow** — larger prompts mean higher Time-To-First-Token

## The Solution

A two-stage inference pipeline:

```
User Query + History
        │
        ▼
┌─────────────────────┐
│  Context Compactor   │  ← Gemini 2.0 Flash (cheap, fast)
│  Summarize history   │     Strips noise, preserves decisions
└────────┬────────────┘
         │ compressed context
         ▼
┌─────────────────────┐
│  Inference Model     │  ← Gemini 2.5 Pro (smart, expensive)
│  Answer the query    │     Receives only essential context
└─────────────────────┘
```

The Compactor fires **only when history exceeds a configurable token threshold** — short conversations go straight to Pro with zero overhead.

## Results

Tested with a multi-turn career advice conversation (8 messages):

| Metric | Without Compaction | With Compaction | Delta |
|--------|-------------------|-----------------|-------|
| History tokens sent to Pro | 272 | 121 | **-55%** |
| Compactor cost | — | ~0.002 cents | negligible |
| Answer quality | Baseline | Equivalent | same |

The compactor (Flash) cost is ~20x cheaper per token than the inference model (Pro), making the two-stage approach economically rational for any conversation beyond a few turns.

## Architecture

```
context-engine/
├── app/                              # Spring Boot 3.4 + LangChain4j
│   ├── src/main/java/.../
│   │   ├── config/
│   │   │   └── VertexAiConfig.java       # Two beans: inferenceModel + compactorModel
│   │   ├── controller/
│   │   │   └── ChatController.java       # /api/chat, /api/chat-raw, /api/health
│   │   ├── service/
│   │   │   ├── ChatService.java          # Orchestration: compact-if-needed → infer
│   │   │   └── CompactorService.java     # Token estimation + Flash summarization
│   │   └── model/
│   │       ├── ChatRequest.java          # message + history[]
│   │       └── ChatResponse.java         # answer + tokenUsage + compacted flag
│   ├── Dockerfile
│   └── pom.xml
└── terraform/                        # GCP infrastructure
    ├── main.tf                           # Cloud Run, Artifact Registry, IAM, APIs
    ├── variables.tf
    ├── outputs.tf
    └── terraform.tfvars.example
```

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Gemini 2.0 Flash** as compactor | Cheapest Gemini model, fast enough for summarization |
| **Gemini 2.5 Pro** for inference | Best reasoning quality for the final answer |
| **LangChain4j** abstraction | Swap models/providers by changing one config line |
| **Token estimation via char/4** | Simple heuristic, avoids tokenizer dependency |
| **Configurable threshold** | `compactor.token-threshold` in application.yml |
| **A/B endpoints** | `/chat` (with compaction) vs `/chat-raw` (without) for benchmarking |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.4 + LangChain4j |
| LLM Provider | Google Vertex AI (Gemini 2.5 Pro + 2.0 Flash) |
| Deployment | Cloud Run (scale-to-zero, pay-per-request) |
| Infrastructure | Terraform (GCP provider) |
| Container | Docker (Eclipse Temurin 21 Alpine) |

## API Reference

### `GET /api/health`
```json
{"status": "UP", "service": "context-engine"}
```

### `POST /api/chat` — With context compaction
### `POST /api/chat-raw` — Without compaction (baseline for A/B comparison)

**Request:**
```json
{
  "message": "What should I focus on?",
  "history": [
    {"role": "user", "content": "I want to learn backend development"},
    {"role": "assistant", "content": "Java with Spring Boot is a great choice..."}
  ]
}
```

**Response:**
```json
{
  "answer": "Based on our conversation, start with...",
  "usage": {
    "inputTokens": 144,
    "outputTokens": 418,
    "compactorInputTokens": 272,
    "compactorOutputTokens": 121
  },
  "compacted": true
}
```

When `compacted: true`, the response includes compactor token counts — enabling cost comparison between the two endpoints.

## How to Run

### Prerequisites
- Java 21 + Maven
- GCP project with billing enabled
- `gcloud` CLI authenticated (`gcloud auth application-default login`)
- Terraform >= 1.5

### 1. Deploy infrastructure
```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars — set your project_id

terraform init
terraform apply
```

### 2. Build and deploy application
```bash
cd app
mvn clean package -DskipTests

# Build and push Docker image via Cloud Build
gcloud builds submit . \
  --tag us-central1-docker.pkg.dev/YOUR_PROJECT/context-engine/app:latest \
  --region us-central1

# Update Cloud Run
gcloud run services update context-engine \
  --region us-central1 \
  --image us-central1-docker.pkg.dev/YOUR_PROJECT/context-engine/app:latest
```

### 3. Test
```bash
# Health check
curl https://YOUR_CLOUD_RUN_URL/api/health

# Chat with compaction
curl -X POST https://YOUR_CLOUD_RUN_URL/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message": "Hello", "history": []}'
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `vertex-ai.project-id` | `$GCP_PROJECT_ID` | GCP project ID |
| `vertex-ai.location` | `$GCP_LOCATION` | Region (must support Gemini) |
| `compactor.token-threshold` | `200` | Token count above which compaction triggers |

## What This POC Demonstrates

1. **Context Engineering > Prompt Engineering** — managing *what* goes into the context window matters more than *how* you phrase the prompt
2. **Economic two-tier inference** — cheap model for preprocessing, expensive model for reasoning
3. **Measurable ROI** — token usage exposed in every response for A/B comparison
4. **Infrastructure as Code** — full GCP stack reproducible via `terraform apply`
5. **LangChain4j on GCP** — Java-first LLM integration with Vertex AI (not just Python)

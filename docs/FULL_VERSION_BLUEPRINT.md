# PromptKu Full Version — Product & Engineering Blueprint

Status: Foundation specification  
Baseline: PromptKu Android v1.4.4  
Target: Android-first AI Cinematic Production Operating System

## 1. Product direction

PromptKu evolves from an offline-first prompt builder into an end-to-end cinematic pre-production and prompt-production workspace. The product must convert a creative brief into a structured, reviewable production package: story, screenplay, character bible, location/world bible, scene plan, shot design, platform-specific prompts, continuity constraints, and exportable production documents.

The existing v1.4.4 application remains the compatibility baseline. Existing local projects, scripts, scenes, DNA records, history, templates, prompt engines, scoring, and export behavior must not be removed or silently rewritten.

## 2. Product principles

1. Offline-first: core authoring, local prompt generation, saved projects, and exports work without a network connection.
2. AI-assisted, not AI-dependent: AI improves ideation and drafting; deterministic local engines remain available as fallback.
3. Human approval gates: generated story/scene content is a draft until the user approves it.
4. Continuity by construction: canonical character, location, wardrobe, prop, visual-style, timeline, and screen-direction data are referenced by scenes rather than re-invented in each prompt.
5. Provider-neutral: model providers are adapters behind a backend gateway; provider credentials never ship in the APK.
6. Traceable outputs: retain source brief, generation settings, model/provider metadata when available, revisions, and approval state.
7. Incremental migration: add new modules and schema migrations without destructive changes to existing Room data.

## 3. Full Version capability map

### A. Existing capabilities to preserve
- Quick Mode and detailed prompt wizard
- Template gallery
- Projects and multi-scene management
- Script Engine and flat scene model
- Character DNA and World DNA
- Effects library
- Prompt Engine for Flow/Veo, Wan, Kling, and Seedance
- Prompt Score / slot meter / warnings
- Local history, Room persistence, and text/Fountain export

### B. New production workspace
1. Project Brief: premise, genre, audience, runtime, aspect ratio, visual treatment, language, constraints, references.
2. Story Development: logline, synopsis, theme, character arcs, conflict, stakes, ending, beat sheet, screenplay draft.
3. Master Character Bible: identity anchors, face/body, age range, wardrobe, accessories, voice, behavior, invariants, prohibited drift.
4. Master Location Bible: geography, architecture, layout, materials, weather, time-of-day rules, lighting baseline, persistent props.
5. Scene Blueprint: slugline, dramatic purpose, objective, obstacle, action, dialogue, emotional beat, continuity inputs, transition.
6. Shot & Prompt Studio: shot size, angle, lens, camera motion, blocking, action timing, lighting, sound, dialogue timing, engine-specific constraints.
7. Continuity Validator: detect character/wardrobe/location/time/prop/axis/pose mismatches across adjacent scenes; explain severity and offer fixes.
8. Prompt Compiler: compile canonical scene data into engine-specific prompts, enforce word/token budgets, validate required fields, preserve protected identity and continuity anchors.
9. Production Review: approve/reject/revise per scene; lock approved bibles; track version history.
10. Export Center: scene prompt pack, character/location bibles, screenplay, shot list, production breakdown, JSON/CSV and copy-ready text.
11. AI Workspace: structured generation actions, regeneration scopes, user-editable provider/model settings, cost/usage visibility where provider data permits.
12. Sync & Accounts: optional authentication and encrypted transport; conflict-aware cloud sync while local data remains usable.

## 4. Proposed architecture

Android:
- Compose UI / navigation
- Feature modules: brief, story, bible, scenes, prompt studio, continuity, review, export, settings
- Domain use cases and repository interfaces
- Existing deterministic PromptEngine and ScriptEngine retained as local engines
- Room as local source of truth and offline cache
- WorkManager for retryable sync and queued AI jobs

Backend:
- Supabase Auth for optional identity
- PostgreSQL for synchronized projects and version metadata
- Row Level Security on every user-owned table
- Edge Functions as the only client-facing AI gateway
- Provider adapters for OpenAI, Gemini, and other providers after explicit configuration
- Usage ledger, request IDs, rate limits, idempotency keys, and structured error handling
- No provider secret or service-role key in Android resources, BuildConfig, or client storage

AI orchestration:
- Task-specific structured requests, not one unrestricted mega-prompt
- JSON schema validation and bounded retries
- Separate generation tasks for story, character bible, location bible, scene breakdown, and prompt compilation
- Deterministic post-processing and continuity validation after model output
- Human review before committing generated content to canonical project state

## 5. Data model direction

New normalized entities should be introduced through additive Room migrations:
- ProjectBrief
- StoryVersion
- CharacterBible / CharacterVersion
- LocationBible / LocationVersion
- SceneBlueprint / SceneVersion
- ShotPlan
- ContinuityIssue
- GenerationJob
- GenerationTrace
- ApprovalRecord
- SyncOutbox / SyncConflict

Existing ProjectItem, SceneItem, ScriptItem, ScriptSceneItem, DnaItem, and HistoryItem remain supported. A mapping layer will bridge legacy records into the richer production model. No destructive migration or automatic deletion of user data.

## 6. Security and privacy requirements

- Never embed OpenAI/Gemini/provider API keys in the APK.
- Supabase anon key may be client-visible only with strict RLS and least-privilege policies; service-role keys remain server-side.
- Validate ownership in every Edge Function and database policy.
- Apply per-user quotas, request-size limits, timeouts, abuse controls, and idempotency.
- Store the minimum personal data needed.
- Make cloud sync and AI processing explicit, configurable features.
- Do not transmit project content to a provider without a user-triggered action and clear disclosure.
- Keep secrets out of logs and crash reports.

## 7. Delivery phases and exit criteria

### Phase 0 — Baseline audit and protection
- Inventory all source files, dependencies, licenses, tests, Room schema, workflows, signing process, and release artifacts.
- Establish regression tests for current v1.4.4 behavior.
- Document legacy-to-new model mappings.
Exit: baseline build and tests reproducibly pass; no user-data migration risk is unresolved.

### Phase 1 — Production domain foundation
- Add domain models, repositories, additive Room migrations, and project workspace navigation.
- Import existing projects/scripts/DNA into the new workspace without data loss.
Exit: legacy workflows remain functional and new projects persist offline.

### Phase 2 — Cinematic production workflow
- Build Brief, Story, Character Bible, Location Bible, Scene Blueprint, and approval/versioning.
- Add continuity rules and deterministic validation.
Exit: a complete multi-scene project can be authored and reviewed offline.

### Phase 3 — AI gateway
- Add backend schema, RLS, Edge Functions, provider adapter interface, quotas, usage ledger, and structured output validation.
- Add opt-in AI actions and safe fallback behavior.
Exit: no provider secret in APK; ownership, rate-limit, failure, and retry tests pass.

### Phase 4 — Prompt compiler and platform adapters
- Connect approved scene data to existing platform-specific PromptEngine.
- Add platform capability profiles, length budgets, warnings, and regression tests.
Exit: same canonical scene produces valid, distinct outputs for supported target engines.

### Phase 5 — Sync, export, QA and release
- Add conflict-aware sync, export packs, instrumentation, accessibility, performance and security checks.
- Run migration tests from real v1.4.4 database fixtures.
Exit: release candidate passes unit, integration, migration, security, and device QA.

## 8. Quality gates

Every feature must include:
- Unit tests for domain rules and deterministic engines
- Room migration tests and backward-compatibility tests
- Repository and backend contract tests
- RLS / ownership tests for cloud features
- Failure-path tests: offline, timeout, invalid model output, quota exceeded, conflict, retry
- Regression tests for v1.4.4 prompt output and existing user workflows

A green GitHub Actions build is necessary but not sufficient for release approval. APK installation, runtime flows, data migration, and signing identity must also be verified.

## 9. Non-goals for the first Full Version milestone

- Automatic video rendering or generation inside PromptKu
- Unreviewed autonomous publishing to external platforms
- Hard dependency on a single AI vendor
- Destructive rewrite of the current app
- Storing provider API keys on-device

## 10. Immediate engineering sequence

1. Complete source/dependency/license/security audit.
2. Extract current behavior into regression tests and fixtures.
3. Inspect all Room entities, DAOs, migrations, and export paths.
4. Design additive domain schema and repository boundaries.
5. Implement one vertical slice at a time, beginning with Project Brief → Character/Location Bible → Scene Blueprint → existing PromptEngine.
6. Validate each slice in CI and on-device before proceeding.

# Personal OS — Product & Architecture Specification

## Purpose

This document is the implementation specification for a unified **Personal Operating System (Personal OS)**.

The application is **not** only a fitness app, only a to-do list, a Notion clone, or an RPG game. It is a unified system for managing:

- Tasks and execution
- Projects and goals
- Notes and knowledge
- Learning
- Habits and routines
- Health and fitness
- Personal metrics
- Progress and analytics

The system should reduce fragmentation between separate tools such as to-do apps, note-taking tools, habit trackers, fitness trackers, study trackers, and personal dashboards.

# 1. Product Vision

## Core idea

Build a **Personal Operating System** that represents important entities in a user's life and connects them.

The application should answer:

- What should I do today?
- What am I working toward?
- Which tasks contribute to which goals?
- Which projects are active?
- What knowledge supports a project?
- What should I learn next?
- How consistent have I been?
- How is my health progressing?
- Which areas are being neglected?

The application must be a **connected system**, not a collection of isolated modules.

## Product philosophy

### System over isolated apps

Do not build independent clones of Todoist, Notion, MyFitnessPal, or Hevy.

Build shared primitives.

```text
Goal: Build Cognitive OS
│
├── Project: Ken
│   ├── Tasks
│   ├── Notes
│   ├── Resources
│   └── Milestones
│
├── Learning
│   ├── Embeddings
│   ├── Vector Databases
│   └── RAG
│
└── Metrics
    └── Hours invested
```

### Fitness is a domain, not the whole application

```text
PERSONAL OS
│
├── Execution
├── Projects
├── Knowledge
├── Learning
├── Health
├── Habits
├── Goals
└── Analytics
```

Health includes workouts, nutrition, calories, macros, weight, body measurements, steps, water, sleep, and progress.

### Remove RPG semantics but preserve motivation

Avoid:

- Classes
- Mages/warriors
- Ability points
- Magic damage
- Boss fights
- Dungeons
- Fantasy ranks

Keep:

- Streaks
- Consistency
- Progress
- Milestones
- Achievements
- Trends
- Completion feedback

Example:

```text
OLD
+50 XP
LEVEL UP

NEW
Weekly Consistency: 82%
5-day streak
3 / 4 planned workouts completed
Strength trend: Improving
```

# 2. Design Direction

## Visual identity

Use:

- Dark environment
- Deep blue-black surfaces
- Electric/neon blue accents
- Premium aesthetic
- Strong geometric layouts
- Subtle illuminated borders
- Modern analytics
- High information density without clutter

Suggested starting palette:

```text
Background: #0B0F17
Surface:    #151A26
Primary:    #3B82F6
Success:    Green
Warning:    Amber
Error:      Red
```

## Execution UX

Tasks must remain extremely fast to interact with.

```text
[  ] Design database schema
[✓] Finish workout
[  ] Read virtual memory chapter
```

Support nesting:

```text
[  ] Build Personal OS
    [  ] Define architecture
    [  ] Create database schema
    [  ] Implement task engine
```

# 3. Conceptual Domain Architecture

```text
PERSONAL OS
│
├── LIFE
│   ├── Life Areas
│   ├── Goals
│   └── Metrics
│
├── EXECUTION
│   ├── Tasks
│   ├── Habits
│   ├── Routines
│   └── Events
│
├── WORK
│   ├── Projects
│   ├── Milestones
│   └── Actions
│
├── KNOWLEDGE
│   ├── Notes
│   ├── Documents
│   ├── Resources
│   └── Concepts
│
├── LEARNING
│   ├── Domains
│   ├── Subjects
│   ├── Topics
│   ├── Concepts
│   └── Progress
│
├── HEALTH
│   ├── Fitness
│   ├── Nutrition
│   ├── Body Metrics
│   └── Recovery
│
└── INTELLIGENCE
    ├── Analytics
    ├── Insights
    ├── Relationships
    └── Recommendations
```

These are conceptual domains and should not automatically become bottom-navigation tabs.

# 4. Core Behavioral Model

## Capture → Organize → Execute

### Layer 1 — Capture

Fast creation of:

- Task
- Note
- Idea
- Goal
- Habit
- Metric entry
- Workout
- Resource

Example:

> Research vector databases tomorrow

Initially:

```text
What is this?

○ Task
○ Note
○ Idea
○ Goal
○ Habit
```

Future versions may classify input automatically.

### Layer 2 — Organize

```text
Task
↓
Project
↓
Goal
↓
Life Area
```

or:

```text
Note
↓
Concept
↓
Learning Topic
↓
Domain
```

### Layer 3 — Execute

The primary question:

> What should I do now?

```text
TODAY

Priority
[  ] Complete assignment

Projects
[  ] Design architecture

Health
[  ] Workout
[  ] Hit protein target

Learning
[  ] Study virtual memory
```

The user should spend more time doing than organizing.

# 5. Core Entity Ontology

Do not design database tables from screens.

Correct process:

```text
What exists in the user's life?
↓
Define entities
↓
Define relationships
↓
Define lifecycle/state
↓
Design storage
↓
Build features
↓
Build UI
```

## 5.1 User

```typescript
interface User {
  id: string;
  email?: string;
  createdAt: Date;
  updatedAt: Date;
}
```

## 5.2 Profile

```typescript
interface Profile {
  userId: string;
  displayName?: string;
  avatarUrl?: string;
  timezone?: string;
  locale?: string;
  theme?: "dark" | "light" | "system";
  createdAt: Date;
  updatedAt: Date;
}
```

## 5.3 Life Area

Examples:

- Health
- Education
- Career
- Projects
- Finance
- Personal Development
- Relationships

```typescript
interface LifeArea {
  id: string;
  userId: string;
  name: string;
  description?: string;
  icon?: string;
  color?: string;
  status: "active" | "archived";
  createdAt: Date;
  updatedAt: Date;
}
```

## 5.4 Goal

Examples:

- Learn Operating Systems
- Lose 10 kg
- Build Personal OS
- Read 20 books

```typescript
interface Goal {
  id: string;
  userId: string;
  title: string;
  description?: string;
  status: "active" | "paused" | "completed" | "cancelled";
  targetValue?: number;
  currentValue?: number;
  unit?: string;
  startDate?: Date;
  targetDate?: Date;
  createdAt: Date;
  updatedAt: Date;
}
```

Goals can connect to projects, tasks, habits, metrics, learning items, and notes.

## 5.5 Project

A bounded initiative with an intended outcome.

```typescript
interface Project {
  id: string;
  userId: string;
  title: string;
  description?: string;
  status: "planning" | "active" | "paused" | "completed" | "archived";
  startDate?: Date;
  targetDate?: Date;
  createdAt: Date;
  updatedAt: Date;
}
```

Projects can relate to tasks, notes, resources, milestones, goals, learning items, and metrics.

## 5.6 Milestone

```typescript
interface Milestone {
  id: string;
  title: string;
  description?: string;
  status: "pending" | "in_progress" | "completed";
  targetDate?: Date;
  createdAt: Date;
  updatedAt: Date;
}
```

## 5.7 Task

The fundamental unit of execution. Usually completed once.

```typescript
interface Task {
  id: string;
  userId: string;
  title: string;
  description?: string;
  status: "inbox" | "todo" | "in_progress" | "completed" | "cancelled";
  priority: "none" | "low" | "medium" | "high" | "urgent";
  dueDate?: Date;
  completedAt?: Date;
  parentTaskId?: string;
  position?: number;
  createdAt: Date;
  updatedAt: Date;
}
```

Support:

- Inbox capture
- Today
- Upcoming
- Projects
- Nested subtasks
- Completion
- Rescheduling
- Priorities
- Tags
- Relationships

A task may exist independently or within:

```text
Goal
└── Project
    └── Task
        └── Subtask
```

## 5.8 Habit

A repeated behavior.

```text
Task  = complete once
Habit = repeated behavior
```

```typescript
interface Habit {
  id: string;
  userId: string;
  title: string;
  description?: string;
  frequencyType: "daily" | "weekly" | "custom";
  targetCount?: number;
  active: boolean;
  createdAt: Date;
  updatedAt: Date;
}
```

Habit history:

```typescript
interface HabitLog {
  id: string;
  habitId: string;
  completedAt: Date;
  value?: number;
  notes?: string;
}
```

## 5.9 Routine

A structured sequence of recurring actions.

Example:

```text
Morning Routine
[ ] Drink water
[ ] Exercise
[ ] Review daily plan
```

## 5.10 Note

Primary flexible knowledge object.

```typescript
interface Note {
  id: string;
  userId: string;
  title: string;
  content?: string;
  format: "markdown" | "blocks";
  status: "active" | "archived";
  createdAt: Date;
  updatedAt: Date;
}
```

Notes should eventually support:

- Markdown
- Block editing
- Tags
- Attachments
- References
- Backlinks
- Related entities

Example:

```text
Note: RAG Architecture

Related:
├── Project: Ken
├── Learning Topic: Vector Databases
├── Task: Implement Retrieval Pipeline
└── Resource: Research Paper
```

## 5.11 Note Block

Optional future primitive. Do not over-engineer in MVP.

```typescript
interface NoteBlock {
  id: string;
  noteId: string;
  type: string;
  content: Record<string, unknown>;
  position: number;
}
```

Potential types:

- paragraph
- heading
- checklist
- quote
- code
- image
- embed
- entity reference

## 5.12 Resource

Types:

- Book
- Article
- Video
- Course
- Research Paper
- Website
- Document

```typescript
interface Resource {
  id: string;
  userId: string;
  title: string;
  type: "book" | "article" | "video" | "course" | "paper" | "website" | "document" | "other";
  url?: string;
  author?: string;
  metadata?: Record<string, unknown>;
  createdAt: Date;
  updatedAt: Date;
}
```

## 5.13 Learning Item

Use a recursive hierarchy:

```text
Domain
↓
Subject
↓
Topic
↓
Concept
```

Example:

```text
Computer Science
│
└── Operating Systems
    ├── Processes
    ├── Threads
    ├── Scheduling
    └── Memory Management
```

```typescript
interface LearningItem {
  id: string;
  userId: string;
  parentId?: string;
  title: string;
  description?: string;
  type: "domain" | "subject" | "topic" | "concept";
  status: "not_started" | "learning" | "reviewing" | "mastered";
  mastery?: number;
  position?: number;
  createdAt: Date;
  updatedAt: Date;
}
```

Learning items connect to notes, resources, tasks, projects, and goals.

## 5.14 Metric

A measurable quantity.

Examples:

- Weight
- Calories
- Sleep
- Study Hours
- Steps
- Workout Volume
- Books Read
- Money Saved

```typescript
interface Metric {
  id: string;
  userId: string;
  name: string;
  unit: string;
  valueType: "number" | "duration" | "boolean" | "text";
  category?: string;
  createdAt: Date;
  updatedAt: Date;
}
```

Time-series values:

```typescript
interface MetricEntry {
  id: string;
  metricId: string;
  value: number;
  recordedAt: Date;
  notes?: string;
}
```

# 6. Health Domain

Health is a specialized domain built on core entities and metrics.

## Workouts

```typescript
interface Workout {
  id: string;
  userId: string;
  title: string;
  startedAt?: Date;
  completedAt?: Date;
  notes?: string;
}
```

## Exercises

```typescript
interface Exercise {
  id: string;
  name: string;
  muscleGroup?: string;
  instructions?: string;
  imageUrl?: string;
  metadata?: Record<string, unknown>;
}
```

## Workout Sets

```typescript
interface WorkoutSet {
  id: string;
  workoutId: string;
  exerciseId: string;
  setNumber: number;
  weight?: number;
  reps?: number;
  duration?: number;
  completed: boolean;
}
```

## Nutrition

Support:

- Meals
- Calories
- Protein
- Carbohydrates
- Fat
- Water

Do not make the initial architecture dependent on external nutrition APIs.

## Body measurements

Track:

- Weight
- Body fat
- Chest
- Waist
- Neck
- Shoulders
- Arms
- Forearms
- Thighs
- Calves

Prefer the general Metric system where possible.

# 7. Universal Relationship System

This is a critical architectural component.

Do not hard-code every possible relationship.

```typescript
interface EntityRelation {
  id: string;
  sourceEntityType: EntityType;
  sourceEntityId: string;
  targetEntityType: EntityType;
  targetEntityId: string;
  relationshipType: RelationshipType;
  metadata?: Record<string, unknown>;
  createdAt: Date;
}
```

```typescript
type EntityType =
  | "life_area"
  | "goal"
  | "project"
  | "milestone"
  | "task"
  | "habit"
  | "routine"
  | "note"
  | "resource"
  | "learning_item"
  | "metric"
  | "workout";
```

```typescript
type RelationshipType =
  | "belongs_to"
  | "contributes_to"
  | "supports"
  | "requires"
  | "related_to"
  | "depends_on"
  | "references"
  | "blocks"
  | "tracks"
  | "part_of";
```

Example:

```text
Task
"Implement Vector Database"
│ belongs_to
▼
Project
"Ken"
│ contributes_to
▼
Goal
"Build Cognitive OS"
│ requires
▼
Learning Item
"Vector Embeddings"
```

This enables graph-like behavior without requiring a graph database initially.

# 8. Unified Entity Strategy

Conceptually, entities share:

```text
Entity
├── id
├── type
├── title
├── description
├── createdAt
├── updatedAt
├── tags
└── relationships
```

Do not force all entities into one giant table.

Use:

- Domain-specific relational tables
- Shared IDs
- Generic relationship system
- Generic tagging system

# 9. Tags

Examples:

```text
#urgent
#research
#university
#personal
#health
```

Suggested tables:

```text
tags
├── id
├── user_id
├── name
├── color
```

```text
entity_tags
├── id
├── entity_type
├── entity_id
├── tag_id
```

# 10. Unified Activity/Event System

Record meaningful actions:

- Task completed
- Habit completed
- Workout completed
- Goal created
- Note created
- Project completed
- Metric recorded

```typescript
interface ActivityEvent {
  id: string;
  userId: string;
  entityType: string;
  entityId: string;
  action: string;
  metadata?: Record<string, unknown>;
  occurredAt: Date;
}
```

Enables:

- Activity feed
- Analytics
- History
- Insights
- Future AI context

# 11. Database Architecture

## Recommended approach

Use one primary relational database with flexible JSON fields and a first-class relationship layer.

Do not initially operate separate relational, document, and graph databases.

```text
PRIMARY DATABASE
│
├── Structured relational tables
│   ├── tasks
│   ├── projects
│   ├── goals
│   ├── habits
│   └── metrics
│
├── Flexible content
│   ├── notes
│   ├── note blocks
│   └── JSON metadata
│
└── Relationship layer
    └── entity_relations
```

## Recommended production database

PostgreSQL.

Reasons:

- Strong relational modeling
- Transactions
- JSONB support
- Full-text search
- Mature indexing
- Excellent ecosystem
- Scalability

For offline-first support:

```text
Local SQLite
↓
Sync Engine
↓
Remote PostgreSQL
```

# 12. Offline-First Architecture

```text
UI
│
▼
Application State
│
▼
Local Database
│
├── Immediate read/write
│
▼
Sync Engine
│
▼
Remote Backend
```

Rules:

- UI must not depend directly on network availability.
- Local writes should be immediate.
- Sync should happen asynchronously.
- Syncable entities need timestamps and version metadata.
- Conflicts require explicit resolution strategy.

Common fields:

```text
created_at
updated_at
deleted_at
version
```

Soft deletion is recommended.

# 13. Application Modules

## Today

The command center.

Show:

- Priority tasks
- Today's tasks
- Habits
- Calendar/events
- Health summary
- Learning
- Active projects
- Important metrics

Question answered:

> What matters now?

## Plan

Contains:

- Inbox
- Today
- Upcoming
- Tasks
- Projects
- Goals
- Habits
- Routines

## Knowledge

Contains:

- Notes
- Resources
- Documents
- Concepts
- Linked information

Initial priority:

1. Markdown notes
2. Entity linking
3. Tags
4. Search

Block editing can come later.

## Learning

Contains:

- Domains
- Subjects
- Topics
- Concepts
- Checklists
- Resources
- Progress

Example:

```text
Operating Systems

[✓] Processes
[✓] Threads
[✓] Scheduling
[ ] Virtual Memory
[ ] File Systems
```

## Health

Contains:

- Workouts
- Exercise library
- Nutrition
- Weight
- Measurements
- Sleep
- Water
- Steps
- Progress

## Analytics

Do not only display graphs.

Derive insights:

- Task completion trends
- Habit consistency
- Project activity
- Learning progress
- Workout frequency
- Weight trends
- Time allocation

Future cross-domain insights:

```text
Study consistency increased 18% this month.

Project X has not received activity for 12 days.

Workout frequency is stable, but sleep duration is declining.
```

## Profile

Contains:

- User information
- Preferences
- Theme
- Units
- Privacy
- Data export
- Sync settings

# 14. Navigation

Avoid 8–10 bottom navigation tabs.

Recommended primary navigation:

```text
HOME | PLAN | + | KNOWLEDGE | PROFILE
```

- HOME: Today dashboard
- PLAN: Tasks, projects, goals, habits
- +: Universal capture
- KNOWLEDGE: Notes, resources, learning
- PROFILE: Personal data, analytics, settings

Domain-specific navigation may exist inside modules.

# 15. Universal Capture

The central action:

```text
+
├── Task
├── Note
├── Project
├── Goal
├── Habit
├── Metric
├── Workout
└── Resource
```

Future natural-language example:

> Research vector databases tomorrow

Potential interpretation:

```text
Type: Task
Title: Research vector databases
Due: Tomorrow
```

AI classification is not required for MVP.

# 16. State Machines

## Task

```text
INBOX
↓
TODO
↓
IN_PROGRESS
↓
COMPLETED

Alternative:
CANCELLED
```

## Project

```text
PLANNING
↓
ACTIVE
↓
PAUSED
↓
COMPLETED

Alternative:
ARCHIVED
```

## Goal

```text
ACTIVE
↓
PAUSED
↓
COMPLETED

Alternative:
CANCELLED
```

## Learning Item

```text
NOT_STARTED
↓
LEARNING
↓
REVIEWING
↓
MASTERED
```

# 17. Recommended Code Architecture

```text
src/
│
├── app/
│   ├── navigation/
│   ├── providers/
│   └── configuration/
│
├── features/
│   ├── tasks/
│   ├── projects/
│   ├── goals/
│   ├── habits/
│   ├── notes/
│   ├── resources/
│   ├── learning/
│   ├── metrics/
│   ├── health/
│   └── analytics/
│
├── entities/
│   ├── models/
│   └── relationships/
│
├── database/
│   ├── schema/
│   ├── repositories/
│   ├── migrations/
│   └── sync/
│
├── services/
├── components/
│   ├── ui/
│   ├── cards/
│   ├── charts/
│   └── forms/
│
├── theme/
├── hooks/
├── utils/
└── types/
```

Avoid giant files containing unrelated application logic.

# 18. Repository Pattern

UI components must not directly query the database.

Preferred flow:

```text
Screen
↓
Feature Hook / Controller
↓
Use Case
↓
Repository
↓
Local / Remote Data Source
```

Example:

```text
TodayScreen
↓
useTodayDashboard()
↓
GetTodayOverviewUseCase
↓
TaskRepository
HabitRepository
MetricRepository
↓
Database
```

Benefits:

- Testability
- Maintainability
- Easier sync
- Easier backend migration
- Clear separation of concerns

# 19. Initial Database Tables

```text
users
profiles

life_areas
goals
projects
milestones

tasks
habits
habit_logs
routines
routine_steps

notes
note_blocks

resources
learning_items

metrics
metric_entries

workouts
exercises
workout_sets

entity_relations

tags
entity_tags

activity_events
```

Implement incrementally through migrations.

# 20. MVP Roadmap

## Phase 1 — Foundation

[  ] Project setup
[  ] Authentication architecture
[  ] Database abstraction
[  ] Local storage
[  ] Core entity IDs
[  ] Timestamps
[  ] Soft deletion
[  ] Repository layer
[  ] Relationship system
[  ] Tag system
[  ] Activity event system

## Phase 2 — Execution

[  ] Inbox
[  ] Tasks
[  ] Nested tasks
[  ] Today view
[  ] Due dates
[  ] Priorities
[  ] Task completion
[  ] Projects
[  ] Goals
[  ] Habits

## Phase 3 — Knowledge

[  ] Markdown notes
[  ] Tags
[  ] Search
[  ] Resources
[  ] Entity relationships
[  ] Backlinks

## Phase 4 — Learning

[  ] Learning hierarchy
[  ] Concepts
[  ] Learning checklists
[  ] Mastery state
[  ] Resources
[  ] Progress

## Phase 5 — Health

[  ] Workouts
[  ] Exercise library
[  ] Weight tracking
[  ] Body measurements
[  ] Calories
[  ] Macros
[  ] Water
[  ] Analytics

## Phase 6 — Intelligence

[  ] Unified activity timeline
[  ] Analytics
[  ] Cross-domain relationships
[  ] Insights
[  ] Recommendation engine

## Phase 7 — Advanced

[  ] Offline synchronization
[  ] Collaboration if required
[  ] AI capture
[  ] AI search
[  ] AI insights
[  ] Knowledge graph visualization

# 21. Non-Functional Requirements

## Performance

- Fast initial load
- Immediate task completion feedback
- Local-first reads
- Avoid unnecessary full-table queries
- Paginate large collections
- Index frequently queried fields

Likely indexes:

```text
user_id
status
due_date
updated_at
parent_id
entity_type
entity_id
source_entity_id
target_entity_id
```

## Scalability

Design for:

- Thousands of tasks
- Thousands of notes
- Large metric histories
- Many relationships
- Multiple devices

Do not load all user data into memory.

## Maintainability

Require:

- Strict TypeScript
- Clear module boundaries
- No giant components
- No direct database calls from UI
- Reusable domain models
- Schema migrations
- Automated tests for business logic

## Security

Require:

- User data isolation
- Authentication
- Authorization
- Ownership checks
- Encrypted transport
- Secure token storage
- Data export

# 22. Relationship Query Examples

## Goal context

```text
Goal
↓
Related Projects
↓
Related Tasks
↓
Related Habits
↓
Supporting Learning Items
↓
Supporting Notes
```

## Project context

```text
Project
├── Tasks
├── Milestones
├── Notes
├── Resources
├── Learning Requirements
└── Activity
```

## Learning context

```text
Concept
├── Parent Topic
├── Child Concepts
├── Notes
├── Resources
├── Related Tasks
└── Projects requiring it
```

## Health context

```text
Health Goal
├── Weight Metrics
├── Workouts
├── Nutrition
├── Habits
└── Progress Trends
```

# 23. Architectural Constraints

## Do not

- Build disconnected mini-apps
- Create database tables based purely on screens
- Couple UI directly to storage
- Make the application dependent on network availability
- Over-engineer the MVP with multiple databases
- Introduce a graph database initially
- Implement AI before deterministic features work
- Turn the application into an RPG
- Clone Notion's full block editor prematurely

## Do

- Build reusable domain primitives
- Use explicit relationships
- Use modular architecture
- Support offline-first evolution
- Preserve extensibility
- Prefer simple schemas over premature abstraction
- Implement incrementally
- Write migrations
- Maintain strong typing
- Keep business logic testable

# 24. Implementation Order

Codex should implement in this order.

## Step 1 — Project foundation

Deliver:

```text
Project structure
TypeScript configuration
Linting
Formatting
Environment configuration
```

## Step 2 — Core database abstractions

Deliver:

```text
Entity IDs
Base timestamps
Soft deletion
Repository interfaces
Database adapter
Migration system
```

## Step 3 — Core entities

Start with:

```text
Life Areas
Goals
Projects
Tasks
Habits
Notes
Learning Items
Metrics
```

Do not start with every advanced health feature.

## Step 4 — Relationships

Deliver:

```text
Create relationship
Delete relationship
Get outgoing relationships
Get incoming relationships
Get related entities
```

## Step 5 — Execution engine

Deliver:

```text
Inbox
Today
Tasks
Nested tasks
Projects
Goals
Habits
```

## Step 6 — Today dashboard

Aggregate:

```text
Tasks
Habits
Projects
Metrics
Learning
Health summaries
```

## Step 7 — Knowledge and Learning

## Step 8 — Health

## Step 9 — Analytics

## Step 10 — Sync and advanced intelligence

# 25. Final Product Definition

The final application is:

> A unified personal operating system that combines execution, projects, goals, knowledge, learning, habits, health, metrics, and analytics into one interconnected data model.

The core differentiator is not an individual feature.

It is:

```text
UNIFIED ENTITIES
+
EXPLICIT RELATIONSHIPS
+
CROSS-DOMAIN CONTEXT
+
FAST DAILY EXECUTION
```

Target model:

```text
                 PERSONAL OS
                      │
        ┌─────────────┼─────────────┐
        │             │             │
    EXECUTION     KNOWLEDGE       TRACKING
        │             │             │
     Tasks         Notes          Health
     Projects      Learning       Metrics
     Goals         Resources      Habits
        │             │             │
        └─────────────┼─────────────┘
                      │
                  INSIGHTS
```

The system should eventually understand the connections between what the user:

- Wants
- Plans
- Does
- Learns
- Tracks
- Achieves

This document is the architectural foundation. Implementation should prioritize correctness of the core entity model and relationships over rapid accumulation of isolated features.

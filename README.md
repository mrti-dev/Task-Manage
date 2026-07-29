# Task Manager

Full-stack task management application with real-time collaboration, workspace-based project organization, and granular role-based access control.

## Architecture

```
Task Manager/
├── task-management-api/         # Backend — Spring Boot REST API
└── TaskManagement-ui/
    └── task-management-ui/      # Frontend — React SPA
```

---

## Backend — `task-management-api`

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 3.5.6 |
| Build | Maven |
| Database | MySQL 8 (Hibernate `ddl-auto=update`) |
| Auth | JWT (access token 24h + refresh token 7d, httpOnly cookie) |
| Security | Spring Security + JWT filter |
| Real-time | STOMP over WebSocket (SockJS fallback) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |

### Dependencies

- `spring-boot-starter-web` — REST APIs
- `spring-boot-starter-data-jpa` — ORM
- `spring-boot-starter-security` — Auth
- `spring-boot-starter-validation` — Request validation
- `spring-boot-starter-websocket` — Real-time
- `mysql-connector-j` — MySQL driver
- `jjwt` 0.12.6 — JWT generation & validation
- `lombok` — Boilerplate reduction
- `springdoc-openapi` 2.8.14 — Swagger UI

### Database

**Schema:** `taskmanagement` (auto-generated via `ddl-auto=update`)

| Table | Description |
|-------|-------------|
| `users` | Platform users (ADMIN/USER roles) |
| `workspaces` | Top-level organizational unit (soft-delete) |
| `workspace_members` | User-workspace membership with OWNER/MEMBER roles |
| `projects` | Projects within a workspace (soft-delete) |
| `tasks` | Tasks within a project (soft-delete) |
| `comments` | Comments on tasks |
| `notifications` | User notifications with type + link |
| `refresh_tokens` | JWT refresh token store |
| `activity_logs` | Audit trail for entities |

### API Endpoints

All endpoints are proxied via Vite in development (`localhost:5173` → `localhost:8080`).

#### Auth (`/api/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login, returns access token + httpOnly cookie |
| POST | `/api/auth/register` | Register, returns access token + httpOnly cookie |
| POST | `/api/auth/refresh` | Refresh access token via cookie |
| POST | `/api/auth/logout` | Revoke refresh token, clear cookie |

#### Users (`/api/users`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/me` | Current user profile |
| PUT | `/api/users/me` | Update current user |

#### Workspaces (`/api/workspaces`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/workspaces` | Create workspace (auto-add creator as OWNER) |
| GET | `/api/workspaces` | List user's workspaces |
| GET | `/api/workspaces/{id}` | Get workspace by ID |
| PUT | `/api/workspaces/{id}` | Update workspace (OWNER) |
| DELETE | `/api/workspaces/{id}` | Soft-delete workspace (OWNER) |

#### Workspace Members (`/api/workspace-members`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/workspace-members` | Add member |
| GET | `/api/workspace-members` | List members |
| PUT | `/api/workspace-members/{id}` | Update role (OWNER) |
| DELETE | `/api/workspace-members/{id}` | Remove member (OWNER) |

#### Projects (`/api/projects`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/projects` | Create project |
| GET | `/api/projects` | List (paginated, filterable) |
| GET | `/api/projects/{id}` | Get by ID |
| PUT | `/api/projects/{id}` | Update |
| DELETE | `/api/projects/{id}` | Soft-delete |

#### Tasks (`/api/tasks`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tasks` | Create task |
| GET | `/api/tasks` | List (paginated, filterable) |
| GET | `/api/tasks/{id}` | Get by ID |
| PUT | `/api/tasks/{id}` | Update |
| DELETE | `/api/tasks/{id}` | Soft-delete |

#### Comments (`/api/comments`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/comments` | Create comment |
| GET | `/api/comments/task/{taskId}` | Get by task |
| PUT | `/api/comments/{id}` | Update (author only) |
| DELETE | `/api/comments/{id}` | Delete (author only) |

#### Notifications (`/api/notifications/user/{userId}`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `...` | List (paginated) |
| GET | `.../unread` | Unread notifications |
| GET | `.../unread-count` | Unread count |
| PUT | `.../read/{id}` | Mark as read |
| PUT | `.../read-all` | Mark all as read |

#### Activity Logs (`/api/activities`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/activities/entity/{entityType}/{entityId}` | Logs by entity |
| GET | `/api/activities/workspace/{workspaceId}` | Logs by workspace |

### Enums

| Enum | Values |
|------|--------|
| `Role` | `ADMIN`, `USER` |
| `WorkspaceRole` | `OWNER`, `MEMBER` |
| `TaskStatus` | `TODO`, `IN_PROGRESS`, `DONE` |
| `TaskPriority` | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `ProjectStatus` | `PLANNING`, `ACTIVE`, `COMPLETED`, `ARCHIVED` |
| `NotificationType` | 13 types (WORKSPACE_CREATED, PROJECT_*, TASK_*, COMMENT_CREATED, WORKSPACE_MEMBER_*) |

---

## Frontend — `TaskManagement-ui`

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | React 19 |
| Build | Vite 8 |
| Language | JavaScript (JSX) |
| UI Library | MUI 9 (Material UI) + Emotion |
| Routing | React Router DOM 7 |
| HTTP Client | Axios 1 (JWT interceptor, auto-refresh) |
| Real-time | STOMP.js + SockJS |
| Date | dayjs |
| Linter | oxlint (React + Oxc rules) |

### Project Structure

```
task-management-ui/
├── src/
│   ├── api/            # Axios client + API functions
│   │   ├── axiosClient.js    # Configured Axios with JWT interceptor
│   │   ├── authApi.js
│   │   ├── workspaceApi.js
│   │   ├── workspaceMemberApi.js
│   │   ├── projectApi.js
│   │   ├── taskApi.js
│   │   ├── commentApi.js
│   │   └── notificationApi.js
│   ├── components/
│   │   ├── common/           # ProtectedRoute, ConfirmDialog, EmptyState
│   │   ├── header/           # Top app bar
│   │   ├── layouts/          # AuthLayout, MainLayout
│   │   ├── notification/     # NotificationMenu, NotificationToast
│   │   └── sidebar/          # Navigation sidebar
│   ├── contexts/
│   │   ├── AuthContext.jsx         # Auth state + token management
│   │   └── NotificationContext.jsx # WebSocket + notification state
│   ├── pages/
│   │   ├── auth/                  # LoginPage, RegisterPage
│   │   ├── notifications/         # NotificationPage
│   │   ├── projects/              # ProjectBoardPage
│   │   ├── tasks/                 # TaskDetailModal
│   │   └── workspaces/            # WorkspaceListPage, WorkspaceDetailPage
│   ├── utils/
│   │   ├── constants.js           # Status color mappings
│   │   └── eventBus.js            # Pub/sub event bus
│   ├── theme.js                   # MUI theme customization
│   ├── App.jsx                    # Route definitions
│   └── main.jsx                   # Entry point
├── index.html
├── vite.config.js
└── package.json
```

### Frontend Features

- **Auth:** JWT-based login/register, automatic token refresh with request queuing during refresh
- **Workspaces:** CRUD, member management with OWNER/MEMBER roles, settings
- **Projects:** Kanban-style board, task table with priority/status/assignee, real-time updates
- **Tasks:** Detail modal with comments, claim, assign, status change, STOMP-driven real-time comments
- **Notifications:** Real-time push via WebSocket, unread badge, paginated history, mark read
- **Routing:** Protected routes (redirect to login), nested layouts, URL-driven task detail modal
- **UI:** Fully responsive, custom MUI theme with 25+ component overrides, loading skeletons, empty states

### Real-time Architecture

```
Backend (STOMP broker)
  │
  ├── /topic/workspace/{id}/tasks/{taskId}/comments  → Comment updates
  └── /user/{id}/queue/notifications                  → Per-user notifications
```

Backend pushes via `SimpMessagingTemplate`, frontend connects via STOMP over SockJS.

---

## Getting Started

### Prerequisites

- **Java 25** + Maven
- **Node.js 20+** + npm
- **MySQL 8**

### Backend Setup

```bash
cd task-management-api

# Database (MySQL)
# Create database: taskmanagement

# Configure connection (default: root/123456@localhost:3306/taskmanagement)
# Environment variables: MYSQL_HOST, MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD

# Run
mvn spring-boot:run
```

The API starts at `http://localhost:8080`. Swagger UI at `http://localhost:8080/swagger-ui.html`.

### Frontend Setup

```bash
cd TaskManagement-ui/task-management-ui
npm install
npm run dev
```

The app starts at `http://localhost:5173`. Vite proxies `/api` and `/ws` to `localhost:8080`.

---

## Environment Variables

### Backend
| Variable | Default | Description |
|----------|---------|-------------|
| `MYSQL_HOST` | `localhost` | Database host |
| `MYSQL_DATABASE` | `taskmanagement` | Database name |
| `MYSQL_USER` | `root` | Database user |
| `MYSQL_PASSWORD` | `123456` | Database password |

JWT configuration in `application.properties` (not env vars currently):
- `jwt.secret` — HMAC-SHA signing key
- `jwt.access-token-expiration` — 86400000ms (24h)
- `jwt.refresh-token-expiration` — 604800000ms (7d)

### Frontend
All API calls are proxied via Vite in development. In production, configure `BASE_URL` in `src/api/axiosClient.js`.

---

## Development

### Backend
- **Lombok** must be enabled in IDE
- API docs auto-generated at `/v3/api-docs` and `/swagger-ui.html`
- Test: `mvn test` (basic context load test)

### Frontend
- Lint: `npm run lint` (oxlint)
- Build: `npm run build` → output to `dist/`
- Preview production build: `npm run preview`

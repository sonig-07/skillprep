# SkillPrep

Interview practice grounded in your actual resume and a target job description —
persistent question bank, per-answer AI feedback, and a reflection dashboard.

## Stack

- **Backend:** Spring Boot 3.3 (Java 17), MongoDB, JWT auth (jjwt)
- **AI:** Groq chat completions API, JSON-mode structured output, all prompts centralized in
  `com.skillprep.ai.PromptFactory`
- **Frontend:** vanilla HTML/CSS/JS single-page app (no build step), dark/light theme
- **Resume parsing:** Apache PDFBox (PDF) / plain text (.txt)
- **Export:** OpenCSV (CSV), iText 7 (PDF)

## Prerequisites

- Java 17+
- Maven 3.8+
- A running MongoDB instance (local `mongodb://localhost:27017` or Atlas)
- A Groq API key — https://console.groq.com

## Configuration

All config is in `src/main/resources/application.yml`, overridable via environment variables:

| Variable | Default | Notes |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/skillprep` | |
| `JWT_SECRET` | placeholder — **change this** | any string ≥32 chars is padded/used as HMAC-SHA256 key |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | |
| `GROQ_API_KEY` | *(empty — required)* | requests fail clearly if unset |
| `GROQ_BASE_URL` | `https://api.groq.com/openai/v1/chat/completions` | |
| `GROQ_MODEL` | `llama-3.3-70b-versatile` | any Groq JSON-mode-capable chat model |

Example:

```bash
export MONGODB_URI="mongodb://localhost:27017/skillprep"
export JWT_SECRET="a-long-random-string-at-least-32-characters"
export GROQ_API_KEY="gsk_..."
```

## Run

```bash
mvn spring-boot:run
```

Then open **http://localhost:8080** — the frontend is served as static content from the same app.

## Project layout

```
src/main/java/com/skillprep/
  ai/            PromptFactory (all prompts, centralized) + GroqService (JSON-mode API client)
  config/        Spring Security + CORS, RestTemplate bean
  controller/    REST endpoints, one per module
  dto/           Request/response records
  exception/     Global exception -> HTTP mapping
  model/         MongoDB documents (User, Resume, JobDescription, SkillMatch, Question, PracticeSession)
  repository/    Spring Data MongoDB repositories
  security/      JWT util + filter
  service/       Business logic, one per module (Auth, Resume, Jd, Match, Practice, QuestionBank, Export, Dashboard)
src/main/resources/
  application.yml
  static/        index.html, css/style.css, js/api.js, js/app.js
```

## Module notes

- **Auth** — signup captures experience level (0–5, 5 = "5+"); JWT returned on signup/signin,
  sent as `Authorization: Bearer <token>` on every subsequent call.
- **Resume & JD** — uploading a new resume/JD deactivates the previous one but keeps it in history
  (`GET /api/resumes`, `GET /api/jd`); either can be reactivated. Resume text is also sent through
  a best-effort AI extraction pass to pull skills/projects for the practice-session focus dropdowns —
  if that call fails, the upload itself still succeeds.
- **Skill matching** — `POST /api/match` compares the active (or specified) resume/JD pair.
- **Practice** — `POST /api/practice/sessions` generates a fresh AI session; dedup is enforced via a
  normalized-text hash checked per user+skill/topic before a generated question is saved, so repeated
  generations on the same topic won't flood the bank with near-duplicates. Answering a global seed
  question clones it into the user's own bank on first attempt so per-user history stays clean.
- **Question Bank** — `GET /api/bank` supports `skillTopic`, `questionType`, `difficulty`,
  `experienceLevel`, `answered`, `flagged`, `keyword`, `sort` (`recent` | `lowest_score` | `most_attempted`)
  query params. `POST /api/practice/sessions/from-bank` assembles a session from chosen question IDs.
  Export via `GET /api/bank/export?format=csv|pdf`.
- **Dashboard** — `GET /api/dashboard` aggregates everything; a skill/topic is flagged as a weak area
  when its average sits more than 10 points below the overall average.

## Known limitations / next steps

- This was built and reviewed without a live compiler in the build environment (Maven Central wasn't
  reachable from the sandbox), so run `mvn compile` locally first and ping me with any errors —
  I'll fix them immediately.
- No refresh-token flow — sessions just expire after `JWT_EXPIRATION_MS` and require re-login.
- Resume skill/project extraction is best-effort AI output, not guaranteed 100% structured — it only
  feeds the optional "focus" dropdowns in Practice, nothing else depends on it.

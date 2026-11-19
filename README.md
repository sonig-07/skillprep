# SkillPrep

**SkillPrep** is an interview practice app. You add your resume and a job description, and it finds the skill gaps and generates interview questions for you to practice. It scores your answers and tracks your progress.

It includes a personal question bank, answer feedback, practice sessions, and a performance dashboard.

## Tech Stack

* **Backend:** Spring Boot, Java 17
* **Database:** MongoDB
* **Frontend:** HTML, CSS, JavaScript
* **Authentication:** JWT, with BCrypt
* **AI:** Ollama (runs locally). other can be used instead.
* **Libraries:** Apache PDFBox (read PDF resumes), iText (PDF export), Lombok

## How It Works

* **Skill matching uses no AI.** A skill dictionary finds skills in the resume and the job description. Matched skills are in both, missing skills are only in the job description, and the score is matched skills divided by job description skills.
* **AI is used for three things:** reading skills and projects from the resume, writing interview questions, and scoring answers.
* The missing skills found by the matching code are passed to the AI so the skill-gap questions are relevant.

## Main Features

### User Authentication
* Sign up and sign in
* JWT checked on every API request by an interceptor
* Dark and light theme saved to the user's profile

### Resume and Job Description
* Upload a resume (PDF or text file)
* Paste a job description
* Keep previous resumes and job descriptions, and choose which one is active

### Skill Matching
* Compare the active resume with the active job description
* Show matched skills, missing skills, a match score, and a short summary

### Interview Practice
* Generate questions from the resume, job description, and missing skills
* Choose difficulty, number of questions, experience level, and an optional focus skill or project
* Answer by typing or by voice (uses the browser's speech recognition)
* Get a score, strengths, improvements, and notes on a strong answer
* Questions with the same text as one you already have are skipped

### Question Bank
* Every generated question is saved, with all your attempts
* Search by keyword, filter by type, difficulty, answered, and flagged, and sort
* Star questions and start a practice session from selected ones

### Dashboard
* Average score and number of answers
* Scores by question type, skill, and experience level
* Weak skills, score trend, and recent sessions

### Export
* Download the question bank as CSV or PDF

## Project Structure

```text
src/main/java/com/skillprep/
├── ai            AiService (talks to the AI)
├── config        AppConfig
├── controller    REST endpoints
├── dto           Request and response shapes
├── exception     GlobalExceptionHandler
├── model         MongoDB documents
├── repository    MongoDB access
├── security      JwtUtil, JwtInterceptor
└── service       Business logic, including SkillMatcher

src/main/resources/
├── application.yml
└── static        index.html, css, js (the frontend)
```

## Requirements

* Java 17+
* Maven
* MongoDB (running locally)
* [Ollama](https://ollama.com) with a model downloaded: `ollama pull llama3.2`

## Run the Project

1. Start MongoDB and make sure Ollama is running.
2. Start the app:

```bash
mvn spring-boot:run
```

3. Open:

```text
http://localhost:8080
```

No API key is needed with Ollama.

## Settings (environment variables, all optional)

```text
MONGODB_URI          default: mongodb://localhost:27017/skillprep
JWT_SECRET           at least 32 characters
JWT_EXPIRATION_MS    default: 86400000 (24 hours)
AI_BASE_URL          default: Ollama on localhost:11434
AI_MODEL             default: llama3.2
AI_API_KEY           only needed for online services such as Groq
```

To use other AI instead of Ollama, set `AI_BASE_URL`, `AI_MODEL` to a AI model name, and `AI_API_KEY` to your key.


## Future Improvements

* Real AI model integration
* Large sets of QnA
* Automated tests
* Deployment
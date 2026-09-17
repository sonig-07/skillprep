# SkillPrep

**SkillPrep** is an interview preparation platform that helps users practice interview questions based on their **resume and target job description**.

It includes a personal question bank, answer feedback, practice sessions, and a performance dashboard.

## Tech Stack

* **Backend:** Spring Boot, Java 17+
* **Database:** MongoDB
* **Frontend:** HTML, CSS, JavaScript
* **Authentication:** JWT
* **AI:** Groq API
* **Resume Parsing:** Apache PDFBox, OpenCSV


## Main Features

### 🔐 User Authentication

* User signup and login
* JWT-based authentication
* Experience level selection

### 📄 Resume & Job Description

* Upload resume
* Upload job description
* Store previous resumes and JDs
* Activate previous documents when needed

### 🎯 Skill Matching

* Compare resume with job description
* Identify matching skills
* Identify missing skills

### 💻 Interview Practice

* Generate interview questions
* Practice questions based on skills and topics
* Answer questions and receive feedback
* Avoid duplicate questions

### 📚 Question Bank

* Save and manage interview questions
* Filter by skill, difficulty, type, and experience level
* Track answered and flagged questions
* Create practice sessions from saved questions

### 📊 Dashboard

* View practice performance
* Track scores
* Identify weak topics
* View practice history

### 📥 Export

Export your question bank as:

* CSV
* PDF

## Project Structure

```text
src/main/java/com/skillprep/

├── ai
├── config
├── controller
├── dto
├── exception
├── model
├── repository
├── security
└── service

src/main/resources/

├── application.yml
└── static
    ├── index.html
    ├── css
    └── js
```

## Requirements

* Java 17+
* MongoDB
* Groq API Key

## Run the Project

```bash
mvn spring-boot:run
```

Then open:

```text
http://localhost:8080
```

## Environment Variables

```text
MONGODB_URI
JWT_SECRET
GROQ_API_KEY
JWT_EXPIRATION_MS
GROQ_BASE_URL
GROQ_MODEL
```

## Future Improvements

* Refresh token authentication
* Improved resume parsing
* More interview question types
* Better performance analytics
* Deployment to cloud

package com.skillprep.ai;

import org.springframework.stereotype.Component;

/**
 * Single source of truth for every prompt sent to the AI provider.
 * Keeping all prompt construction here means prompt tuning never
 * requires hunting through service classes.
 *
 * Every prompt instructs the model to return ONLY strict JSON (no prose,
 * no markdown fences) matching the documented shape, so callers never
 * need to regex-scrape a free-text reply.
 */
@Component
public class PromptFactory {

    private String experienceLabel(Integer level) {
        if (level == null) return "unspecified experience level";
        return switch (level) {
            case 0 -> "0 years experience (fresher / student, no professional experience yet)";
            case 1 -> "1 year of professional experience";
            case 2 -> "2 years of professional experience";
            case 3 -> "3 years of professional experience";
            case 4 -> "4 years of professional experience";
            default -> "5+ years of professional experience (senior)";
        };
    }

    private String calibrationNote(Integer level) {
        if (level == null) return "";
        if (level <= 1) {
            return "Calibration: this candidate is early-career. Even at 'Hard' difficulty, lean toward strong "
                 + "fundamentals and clear reasoning rather than obscure trivia or deep production-scale trade-offs. "
                 + "Do not expect them to have led systems at scale.";
        } else if (level <= 3) {
            return "Calibration: this candidate has mid-level experience. Expect solid fundamentals plus some "
                 + "practical judgment and awareness of trade-offs, but not necessarily deep architectural ownership.";
        } else {
            return "Calibration: this candidate is senior. Even at 'Easy' difficulty, lean toward depth, trade-offs, "
                 + "and real-world judgment rather than pure definitions. They should be pushed on 'why', not just 'what'.";
        }
    }

    /* ---------------------------------------------------------------- */
    /* Skill matching                                                    */
    /* ---------------------------------------------------------------- */

    public String skillMatchPrompt(String resumeText, String jdText) {
        return """
            You are an expert technical recruiter. Compare the RESUME against the JOB DESCRIPTION below \
            and produce an honest, specific skill-match analysis. Do not be generically positive — be accurate.

            RESUME:
            ---
            %s
            ---

            JOB DESCRIPTION:
            ---
            %s
            ---

            Return ONLY strict JSON, no markdown fences, no commentary, in exactly this shape:
            {
              "overallScore": <integer 0-100>,
              "matchedSkills": [ { "skill": "<string>", "confidence": <integer 0-100> } ],
              "missingSkills": [ { "skill": "<string>", "importance": <integer 0-100> } ],
              "fitSummary": "<2-4 sentence honest summary of fit, mentioning real strengths and real gaps>"
            }
            "missingSkills" must be sorted by importance to the JD, descending. Only include skills genuinely \
            implied by the JD text. Do not invent skills not present in either document's context.
            """.formatted(resumeText, jdText);
    }

    /* ---------------------------------------------------------------- */
    /* Question generation                                               */
    /* ---------------------------------------------------------------- */

    public String questionGenerationPrompt(String resumeText, String jdText, int numQuestions,
                                            String difficulty, Integer experienceLevel,
                                            String focusSkill, String focusProject) {
        StringBuilder focus = new StringBuilder();
        if (focusSkill != null && !focusSkill.isBlank()) {
            focus.append("Focus ALL questions on the skill/topic: \"").append(focusSkill).append("\". ");
        }
        if (focusProject != null && !focusProject.isBlank()) {
            focus.append("Focus ALL project-specific questions on the project named \"").append(focusProject)
                 .append("\" as it appears on the resume. ");
        }

        return """
            You are an expert technical interviewer. Generate %d interview practice questions grounded strictly \
            in the RESUME and JOB DESCRIPTION below. Do not use generic templated interview-bank phrasing — every \
            question must clearly connect to specific content in the resume or JD.

            Candidate experience level: %s
            %s

            Target difficulty: %s
            %s
            %s

            Generate a MIX of these three question types (unless a single skill/project focus above overrides this):
            - GENERAL: topic-level conceptual questions about a skill that appears on the resume or JD. Tests \
              fundamentals, not memory of one specific project.
            - PROJECT_SPECIFIC: deep-dive questions tied to ONE named project actually listed on the resume — \
              must reference that project by name and ask about real architecture decisions, trade-offs, or a \
              challenge faced. Never generic ("tell me about a project you worked on") — name the actual project.
            - SKILL_GAP: questions probing a skill the JD requires but the resume does not clearly evidence, \
              testing whether the candidate can still reason about it despite no listed experience with it.

            Return ONLY strict JSON, no markdown fences, no commentary, in exactly this shape:
            {
              "questions": [
                {
                  "questionText": "<string>",
                  "questionType": "GENERAL" | "PROJECT_SPECIFIC" | "SKILL_GAP",
                  "skillTopic": "<short skill/topic label, e.g. 'React', 'System Design', 'PostgreSQL'>",
                  "relatedProject": "<project name if PROJECT_SPECIFIC, else null>"
                }
              ]
            }
            The "questions" array must contain exactly %d items.
            """.formatted(
                numQuestions,
                experienceLabel(experienceLevel),
                calibrationNote(experienceLevel),
                difficulty,
                focus.isEmpty() ? "" : focus.toString(),
                """
                RESUME:
                ---
                %s
                ---

                JOB DESCRIPTION:
                ---
                %s
                ---
                """.formatted(resumeText, jdText),
                numQuestions
        );
    }

    /* ---------------------------------------------------------------- */
    /* Answer feedback                                                    */
    /* ---------------------------------------------------------------- */

    public String answerFeedbackPrompt(String questionText, String questionType, String skillTopic,
                                        String relatedProject, String answerText,
                                        String difficulty, Integer experienceLevel) {
        return """
            You are an expert technical interviewer evaluating a candidate's spoken/typed answer during practice.

            QUESTION (%s, topic: %s%s, difficulty: %s):
            "%s"

            CANDIDATE'S ANSWER:
            "%s"

            Candidate experience level: %s
            %s

            Evaluate the answer strictly against what THIS question asked, calibrated to the stated experience \
            level: do not penalize a fresher for not knowing something only a senior would know, and do not let a \
            senior get away with a shallow/fresher-level answer.

            Return ONLY strict JSON, no markdown fences, no commentary, in exactly this shape:
            {
              "score": <integer 0-100>,
              "strengths": ["<specific strength>", "..."],
              "improvements": ["<specific, actionable improvement>", "..."],
              "idealAnswerNotes": "<2-4 sentences on what a strong answer would have included, specific to this question>"
            }
            """.formatted(
                questionType, skillTopic,
                (relatedProject != null && !relatedProject.isBlank()) ? ", project: " + relatedProject : "",
                difficulty, questionText, answerText,
                experienceLabel(experienceLevel), calibrationNote(experienceLevel)
        );
    }

    /* ---------------------------------------------------------------- */
    /* Resume structuring (best-effort skill/project extraction)         */
    /* ---------------------------------------------------------------- */

    public String resumeExtractionPrompt(String resumeText) {
        return """
            Extract structured data from the RESUME below.

            RESUME:
            ---
            %s
            ---

            Return ONLY strict JSON, no markdown fences, no commentary, in exactly this shape:
            {
              "skills": ["<skill>", "..."],
              "projects": [ { "name": "<project name>", "description": "<1-2 sentence summary of what it involved>" } ]
            }
            Only include projects that are clearly named/distinguishable entities in the resume, not vague \
            catch-all bullet points.
            """.formatted(resumeText);
    }
}

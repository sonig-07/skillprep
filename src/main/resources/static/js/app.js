// app.js — SPA state, routing, and view rendering for SkillPrep.

const state = {
  user: null,
  currentSession: null,   // { sessionId, questions[], difficulty, experienceLevel, focus }
  currentQuestionIdx: 0,
  bankSelection: new Set(),
  bankItemsCache: [],
  recognizing: false,
};

/* ============================== Toast ============================== */

let toastTimer = null;
function toast(message, isError = false) {
  const el = document.getElementById('toast');
  el.textContent = message;
  el.classList.toggle('error', isError);
  el.classList.remove('hidden');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.add('hidden'), 3800);
}

function setError(scope, message) {
  const el = document.querySelector(`[data-error-for="${scope}"]`);
  if (el) el.textContent = message || '';
}

/* ============================== Theme ============================== */

function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  document.querySelector('.theme-toggle-label').textContent = theme === 'dark' ? 'Dark mode' : 'Light mode';
}

document.getElementById('theme-toggle').addEventListener('click', async () => {
  const current = document.documentElement.getAttribute('data-theme');
  const next = current === 'dark' ? 'light' : 'dark';
  applyTheme(next);
  if (state.user) {
    try { await Api.updateProfile({ themePreference: next }); } catch (_) {}
  }
});

/* ============================== Auth ============================== */

function showAuthTab(tab) {
  document.querySelectorAll('.auth-tab').forEach(b => b.classList.toggle('active', b.dataset.authTab === tab));
  document.getElementById('form-signin').classList.toggle('hidden', tab !== 'signin');
  document.getElementById('form-signup').classList.toggle('hidden', tab !== 'signup');
}
document.querySelectorAll('.auth-tab').forEach(btn => {
  btn.addEventListener('click', () => showAuthTab(btn.dataset.authTab));
});

document.getElementById('form-signin').addEventListener('submit', async (e) => {
  e.preventDefault();
  setError('signin', '');
  const fd = new FormData(e.target);
  try {
    const res = await Api.signIn({ email: fd.get('email'), password: fd.get('password') });
    onAuthed(res);
  } catch (err) {
    setError('signin', err.message);
  }
});

document.getElementById('form-signup').addEventListener('submit', async (e) => {
  e.preventDefault();
  setError('signup', '');
  const fd = new FormData(e.target);
  try {
    const res = await Api.signUp({
      name: fd.get('name'),
      email: fd.get('email'),
      password: fd.get('password'),
      experienceLevel: parseInt(fd.get('experienceLevel'), 10),
    });
    onAuthed(res);
  } catch (err) {
    setError('signup', err.message);
  }
});

document.getElementById('btn-logout').addEventListener('click', () => {
  Api.setToken(null);
  state.user = null;
  document.getElementById('view-app').classList.add('hidden');
  document.getElementById('view-auth').classList.remove('hidden');
});

function onAuthed(authResponse) {
  Api.setToken(authResponse.token);
  state.user = authResponse;
  applyTheme(authResponse.themePreference || 'dark');
  document.getElementById('view-auth').classList.add('hidden');
  document.getElementById('view-app').classList.remove('hidden');

  const initial = (authResponse.name || '?').trim().charAt(0).toUpperCase();
  document.getElementById('user-avatar-initial').textContent = initial || '?';
  document.getElementById('user-chip-name').textContent = authResponse.name;
  document.getElementById('user-chip-level').textContent = experienceLabel(authResponse.experienceLevel);

  navigate('dashboard');
}

function experienceLabel(level) {
  if (level === null || level === undefined) return '—';
  return level >= 5 ? '5+ yrs experience' : `${level} yr${level === 1 ? '' : 's'} experience`;
}

/* ============================== Routing ============================== */

function navigate(route) {
  document.querySelectorAll('.nav-item').forEach(b => b.classList.toggle('active', b.dataset.route === route));
  document.querySelectorAll('.route').forEach(r => r.classList.add('hidden'));
  document.getElementById(`route-${route}`).classList.remove('hidden');

  if (route === 'dashboard') loadDashboard();
  if (route === 'resume-jd') loadResumeJd();
  if (route === 'practice') loadPracticeSetup();
  if (route === 'bank') loadBank();
}
document.querySelectorAll('.nav-item').forEach(btn => btn.addEventListener('click', () => navigate(btn.dataset.route)));
document.querySelectorAll('[data-route-to]').forEach(btn => btn.addEventListener('click', () => navigate(btn.dataset.routeTo)));

/* ============================== Score ring helper ============================== */

function setRing(circleEl, valueEl, score) {
  const circumference = 327; // 2 * pi * 52, matches the SVG r=52 in index.html
  if (score === null || score === undefined) {
    circleEl.style.strokeDashoffset = circumference;
    valueEl.textContent = '—';
    return;
  }
  const pct = Math.max(0, Math.min(100, score)) / 100;
  circleEl.style.strokeDashoffset = String(circumference * (1 - pct));
  valueEl.textContent = Math.round(score);
}

/* ============================== Dashboard ============================== */

async function loadDashboard() {
  let data;
  try {
    data = await Api.dashboard();
  } catch (err) {
    toast(err.message, true);
    return;
  }

  const empty = document.getElementById('dashboard-empty');
  const content = document.getElementById('dashboard-content');

  if (!data.totalAnswered) {
    empty.classList.remove('hidden');
    content.classList.add('hidden');
    return;
  }
  empty.classList.add('hidden');
  content.classList.remove('hidden');

  setRing(document.getElementById('ring-overall'), document.getElementById('stat-overall-score'), data.overallAverageScore);
  document.getElementById('stat-total-answered').textContent = data.totalAnswered;

  renderSparkline(data.recentScoreTrend || []);
  renderBarList('breakdown-type', data.scoreByQuestionType, false, s => titleCase(s.replace('_', ' ')));
  renderBarList('breakdown-level', data.scoreByExperienceLevel, false, s => s === '5' ? '5+ yrs' : `${s} yr${s === '1' ? '' : 's'}`);
  renderSkillBreakdown(data.scoreBySkill || []);
  renderRecentSessions(data.recentSessions || []);
}

function titleCase(s) {
  return s.replace(/\w\S*/g, t => t.charAt(0).toUpperCase() + t.substring(1).toLowerCase());
}

function renderSparkline(values) {
  const svg = document.getElementById('trend-sparkline');
  svg.innerHTML = '';
  if (!values.length) return;
  const w = 200, h = 60, pad = 4;
  const min = Math.min(...values), max = Math.max(...values);
  const range = max - min || 1;
  const pts = values.map((v, i) => {
    const x = values.length === 1 ? w / 2 : pad + (i / (values.length - 1)) * (w - pad * 2);
    const y = h - pad - ((v - min) / range) * (h - pad * 2);
    return `${x},${y}`;
  });
  const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  path.setAttribute('d', 'M' + pts.join(' L'));
  svg.appendChild(path);
}

function renderBarList(containerId, dataMap, isWide, labelFn) {
  const el = document.getElementById(containerId);
  el.innerHTML = '';
  const entries = Object.entries(dataMap || {});
  if (!entries.length) {
    el.innerHTML = '<p style="color:var(--text-dim);font-size:0.85rem;">Not enough data yet.</p>';
    return;
  }
  entries.forEach(([key, score]) => {
    const row = document.createElement('div');
    row.className = 'bar-row';
    row.innerHTML = `
      <span class="bar-label">${labelFn(key)}</span>
      <span class="bar-track"><span class="bar-fill" style="width:${Math.max(2, score)}%"></span></span>
      <span class="bar-score">${score}</span>
    `;
    el.appendChild(row);
  });
}

function renderSkillBreakdown(list) {
  const el = document.getElementById('breakdown-skill');
  el.innerHTML = '';
  if (!list.length) {
    el.innerHTML = '<p style="color:var(--text-dim);font-size:0.85rem;">Not enough data yet.</p>';
    return;
  }
  list.forEach(item => {
    const row = document.createElement('div');
    row.className = 'bar-row';
    row.innerHTML = `
      <span class="bar-label">${item.weakArea ? '<span class="weak-dot" title="Weak area"></span>' : ''}${escapeHtml(item.skillTopic)}</span>
      <span class="bar-track"><span class="bar-fill ${item.weakArea ? 'weak' : ''}" style="width:${Math.max(2, item.averageScore)}%"></span></span>
      <span class="bar-score">${item.averageScore}</span>
    `;
    el.appendChild(row);
  });
}

function renderRecentSessions(sessions) {
  const el = document.getElementById('recent-sessions');
  el.innerHTML = '';
  if (!sessions.length) {
    el.innerHTML = '<p style="color:var(--text-dim);font-size:0.85rem;">No sessions yet.</p>';
    return;
  }
  sessions.forEach(s => {
    const row = document.createElement('div');
    row.className = 'session-row';
    const date = new Date(s.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    row.innerHTML = `
      <span>${date} · ${s.questionCount} question${s.questionCount === 1 ? '' : 's'} · ${titleCase(s.difficulty || '')}</span>
      <span class="session-row-meta">${s.averageScore !== null && s.averageScore !== undefined ? 'avg ' + s.averageScore : 'in progress'}</span>
    `;
    row.addEventListener('click', () => openSessionInPractice(s.sessionId));
    el.appendChild(row);
  });
}

document.querySelector('[data-action="download-bank"]').addEventListener('click', () => Api.exportBank('csv'));
document.querySelector('[data-action="practice-weakest"]').addEventListener('click', async () => {
  try {
    const { skillTopic } = await Api.weakestSkill();
    navigate('practice');
    if (skillTopic) {
      pendingFocusSkill = skillTopic;
      await loadPracticeSetup();
    }
  } catch (err) { toast(err.message, true); }
});
document.querySelector('[data-action="view-flagged"]').addEventListener('click', () => {
  navigate('bank');
  setTimeout(() => { document.getElementById('bank-filter-flagged').value = 'true'; applyBankFilters(); }, 0);
});

/* ============================== Resume & JD ============================== */

document.getElementById('btn-choose-resume').addEventListener('click', () => document.getElementById('resume-file-input').click());
document.getElementById('resume-file-input').addEventListener('change', async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  setError('resume', '');
  try {
    await Api.uploadResume(file);
    toast('Resume uploaded');
    await loadResumeJd();
  } catch (err) {
    setError('resume', err.message);
  }
  e.target.value = '';
});

document.getElementById('form-jd').addEventListener('submit', async (e) => {
  e.preventDefault();
  setError('jd', '');
  const fd = new FormData(e.target);
  try {
    await Api.saveJd({ title: fd.get('title'), rawText: fd.get('rawText') });
    toast('Job description saved');
    e.target.reset();
    await loadResumeJd();
  } catch (err) {
    setError('jd', err.message);
  }
});

document.getElementById('btn-run-match').addEventListener('click', async () => {
  const btn = document.getElementById('btn-run-match');
  btn.disabled = true;
  btn.textContent = 'Analyzing…';
  try {
    const match = await Api.runMatch({});
    renderMatch(match);
  } catch (err) {
    toast(err.message, true);
  } finally {
    btn.disabled = false;
    btn.textContent = 'Run match analysis';
  }
});

async function loadResumeJd() {
  try {
    const [resumes, jds] = await Promise.all([Api.listResumes(), Api.listJds()]);
    renderDocList('resume-list', resumes, 'fileName', Api.setActiveResume.bind(Api), loadResumeJd);
    renderDocList('jd-list', jds, 'title', Api.setActiveJd.bind(Api), loadResumeJd);

    const latestMatch = (await Api.matchHistory())[0];
    if (latestMatch) renderMatch(latestMatch);
  } catch (err) {
    toast(err.message, true);
  }
}

function renderDocList(containerId, items, nameField, activateFn, onChange) {
  const el = document.getElementById(containerId);
  el.innerHTML = '';
  if (!items.length) {
    el.innerHTML = '<p style="color:var(--text-dim);font-size:0.85rem;">None yet.</p>';
    return;
  }
  items.forEach(item => {
    const row = document.createElement('div');
    row.className = 'doc-row' + (item.active ? ' is-active' : '');
    row.innerHTML = `
      <span class="doc-row-name">${escapeHtml(item[nameField] || 'Untitled')}</span>
      ${item.active ? '<span class="doc-row-tag">Active</span>' : '<button class="btn btn-ghost small">Set active</button>'}
    `;
    if (!item.active) {
      row.querySelector('button').addEventListener('click', async () => {
        try { await activateFn(item.id); await onChange(); } catch (err) { toast(err.message, true); }
      });
    }
    el.appendChild(row);
  });
}

function renderMatch(match) {
  const wrap = document.getElementById('match-result');
  wrap.classList.remove('hidden');
  setRing(document.getElementById('ring-match'), document.getElementById('match-score-value'), match.overallScore);
  document.getElementById('match-summary').textContent = match.fitSummary || '';

  const matchedEl = document.getElementById('match-matched');
  matchedEl.innerHTML = (match.matchedSkills || []).map(s =>
    `<span class="chip chip-matched">${escapeHtml(s.skill)} · ${s.confidence}%</span>`).join('') || '<span style="color:var(--text-dim);font-size:0.82rem;">None found</span>';

  const missingEl = document.getElementById('match-missing');
  missingEl.innerHTML = (match.missingSkills || []).map(s =>
    `<span class="chip chip-missing">${escapeHtml(s.skill)} · ${s.importance}%</span>`).join('') || '<span style="color:var(--text-dim);font-size:0.82rem;">None found</span>';
}

/* ============================== Practice ============================== */

let pendingFocusSkill = null;

async function loadPracticeSetup() {
  document.getElementById('practice-session').classList.add('hidden');
  document.getElementById('practice-setup').classList.remove('hidden');

  const focusSkillSelect = document.querySelector('select[name="focusSkill"]');
  const focusProjectSelect = document.querySelector('select[name="focusProject"]');
  focusSkillSelect.innerHTML = '<option value="">Mixed</option>';
  focusProjectSelect.innerHTML = '<option value="">Any</option>';

  try {
    const resumes = await Api.listResumes();
    const active = resumes.find(r => r.active);
    (active?.extractedSkills || []).forEach(s => {
      const opt = document.createElement('option');
      opt.value = s; opt.textContent = s;
      focusSkillSelect.appendChild(opt);
    });
    (active?.extractedProjects || []).forEach(p => {
      const opt = document.createElement('option');
      opt.value = p.name; opt.textContent = p.name;
      focusProjectSelect.appendChild(opt);
    });
  } catch (_) { /* optional enhancement — form still works without it */ }

  const expSelect = document.querySelector('select[name="experienceLevel"]');
  if (state.user?.experienceLevel !== undefined && state.user?.experienceLevel !== null) {
    expSelect.value = String(state.user.experienceLevel);
  }

  if (pendingFocusSkill) {
    focusSkillSelect.value = pendingFocusSkill;
    pendingFocusSkill = null;
  }
}

document.getElementById('form-practice-setup').addEventListener('submit', async (e) => {
  e.preventDefault();
  setError('practice', '');
  const fd = new FormData(e.target);
  const btn = e.target.querySelector('button[type="submit"]');
  btn.disabled = true;
  btn.textContent = 'Generating…';
  try {
    const session = await Api.generateSession({
      numQuestions: parseInt(fd.get('numQuestions'), 10),
      difficulty: fd.get('difficulty'),
      experienceLevel: parseInt(fd.get('experienceLevel'), 10),
      focusSkill: fd.get('focusSkill') || null,
      focusProject: fd.get('focusProject') || null,
    });
    startSession(session);
  } catch (err) {
    setError('practice', err.message);
  } finally {
    btn.disabled = false;
    btn.textContent = 'Generate session';
  }
});

async function openSessionInPractice(sessionId) {
  try {
    const session = await Api.getSession(sessionId);
    navigate('practice');
    startSession(session, true);
  } catch (err) { toast(err.message, true); }
}

function startSession(session, resumeAtFirstUnanswered = false) {
  state.currentSession = session;
  state.currentQuestionIdx = 0;
  if (resumeAtFirstUnanswered) {
    const idx = session.questions.findIndex(q => !q.answered);
    state.currentQuestionIdx = idx === -1 ? 0 : idx;
  }
  document.getElementById('practice-setup').classList.add('hidden');
  document.getElementById('practice-session').classList.remove('hidden');
  renderCurrentQuestion();
}

function renderCurrentQuestion() {
  const session = state.currentSession;
  const idx = state.currentQuestionIdx;
  const q = session.questions[idx];

  document.getElementById('session-progress-label').textContent = `Question ${idx + 1} of ${session.questions.length}`;
  document.getElementById('session-progress-fill').style.width = `${((idx) / session.questions.length) * 100}%`;

  document.getElementById('q-type-tag').textContent = titleCase(q.questionType.replace('_', ' '));
  document.getElementById('q-topic-tag').textContent = q.skillTopic + (q.relatedProject ? ` · ${q.relatedProject}` : '');
  document.getElementById('q-diff-tag').textContent = titleCase(q.difficulty);
  document.getElementById('question-text').textContent = q.questionText;

  document.getElementById('answer-input').value = '';
  document.getElementById('answer-zone').classList.remove('hidden');
  document.getElementById('feedback-zone').classList.add('hidden');
  document.getElementById('mic-status').textContent = '';
}

document.getElementById('btn-submit-answer').addEventListener('click', async () => {
  const answerText = document.getElementById('answer-input').value.trim();
  if (!answerText) { toast('Write or speak an answer first', true); return; }

  const session = state.currentSession;
  const q = session.questions[state.currentQuestionIdx];
  const btn = document.getElementById('btn-submit-answer');
  btn.disabled = true;
  btn.textContent = 'Scoring…';

  try {
    const feedback = await Api.submitAnswer({ questionId: q.questionId, answerText, sessionId: session.sessionId });
    renderFeedback(feedback);
  } catch (err) {
    toast(err.message, true);
  } finally {
    btn.disabled = false;
    btn.textContent = 'Submit answer';
  }
});

function renderFeedback(feedback) {
  document.getElementById('answer-zone').classList.add('hidden');
  document.getElementById('feedback-zone').classList.remove('hidden');
  setRing(document.getElementById('ring-feedback'), document.getElementById('feedback-score-value'), feedback.score);

  document.getElementById('feedback-strengths').innerHTML =
    (feedback.strengths || []).map(s => `<li>${escapeHtml(s)}</li>`).join('') || '<li>—</li>';
  document.getElementById('feedback-improvements').innerHTML =
    (feedback.improvements || []).map(s => `<li>${escapeHtml(s)}</li>`).join('') || '<li>—</li>';
  document.getElementById('feedback-ideal').textContent = feedback.idealAnswerNotes || '';

  const btn = document.getElementById('btn-next-question');
  const isLast = state.currentQuestionIdx >= state.currentSession.questions.length - 1;
  btn.textContent = isLast ? 'Finish session ✓' : 'Next question →';
}

document.getElementById('btn-next-question').addEventListener('click', () => {
  const isLast = state.currentQuestionIdx >= state.currentSession.questions.length - 1;
  if (isLast) {
    document.getElementById('session-progress-fill').style.width = '100%';
    toast('Session complete — nice work');
    navigate('dashboard');
    return;
  }
  state.currentQuestionIdx += 1;
  renderCurrentQuestion();
});

// Web Speech API — browser-native speech-to-text, no backend involvement
const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
let recognizer = null;
if (SpeechRec) {
  recognizer = new SpeechRec();
  recognizer.continuous = true;
  recognizer.interimResults = true;
  recognizer.onresult = (event) => {
    let finalText = '';
    for (let i = event.resultIndex; i < event.results.length; i++) {
      if (event.results[i].isFinal) finalText += event.results[i][0].transcript + ' ';
    }
    if (finalText) {
      const ta = document.getElementById('answer-input');
      ta.value = (ta.value + ' ' + finalText).trim();
    }
  };
  recognizer.onend = () => {
    state.recognizing = false;
    document.getElementById('mic-status').textContent = '';
    document.getElementById('mic-status').classList.remove('listening');
    document.getElementById('btn-mic').textContent = '🎤 Speak answer';
  };
}

document.getElementById('btn-mic').addEventListener('click', () => {
  if (!recognizer) { toast('Speech recognition is not supported in this browser', true); return; }
  if (state.recognizing) {
    recognizer.stop();
    return;
  }
  state.recognizing = true;
  document.getElementById('mic-status').textContent = 'Listening…';
  document.getElementById('mic-status').classList.add('listening');
  document.getElementById('btn-mic').textContent = '■ Stop';
  recognizer.start();
});

/* ============================== Question bank ============================== */

async function loadBank() {
  state.bankSelection.clear();
  updateBankSelectionCount();
  await applyBankFilters();
}

function currentBankFilters() {
  return {
    keyword: document.getElementById('bank-keyword').value,
    questionType: document.getElementById('bank-filter-type').value,
    difficulty: document.getElementById('bank-filter-difficulty').value,
    answered: document.getElementById('bank-filter-answered').value,
    flagged: document.getElementById('bank-filter-flagged').value,
    sort: document.getElementById('bank-sort').value,
  };
}

let bankFilterDebounce = null;
['bank-keyword'].forEach(id => document.getElementById(id).addEventListener('input', () => {
  clearTimeout(bankFilterDebounce);
  bankFilterDebounce = setTimeout(applyBankFilters, 300);
}));
['bank-filter-type', 'bank-filter-difficulty', 'bank-filter-answered', 'bank-filter-flagged', 'bank-sort']
  .forEach(id => document.getElementById(id).addEventListener('change', applyBankFilters));

async function applyBankFilters() {
  try {
    const items = await Api.browseBank(currentBankFilters());
    state.bankItemsCache = items;
    renderBankList(items);
  } catch (err) {
    toast(err.message, true);
  }
}

function renderBankList(items) {
  const el = document.getElementById('bank-list');
  const empty = document.getElementById('bank-empty');
  el.innerHTML = '';

  if (!items.length) {
    empty.classList.remove('hidden');
    return;
  }
  empty.classList.add('hidden');

  items.forEach(item => {
    const row = document.createElement('div');
    row.className = 'bank-item';
    row.innerHTML = `
      <input type="checkbox" class="bank-item-check" data-id="${item.questionId}" ${state.bankSelection.has(item.questionId) ? 'checked' : ''}>
      <div class="bank-item-body">
        <p class="bank-item-text">${escapeHtml(item.questionText)}</p>
        <div class="bank-item-tags">
          <span class="tag tag-muted">${titleCase(item.questionType.replace('_', ' '))}</span>
          <span class="tag tag-muted">${escapeHtml(item.skillTopic)}</span>
          <span class="tag tag-muted">${titleCase(item.difficulty)}</span>
          ${item.seed ? '<span class="tag tag-muted">Seed</span>' : ''}
        </div>
      </div>
      <span class="bank-item-score">${item.latestScore !== null && item.latestScore !== undefined ? Math.round(item.latestScore) : '—'}${item.attemptCount > 1 ? ` (${item.attemptCount}x)` : ''}</span>
      <button class="bank-item-flag ${item.flagged ? 'is-flagged' : ''}" title="Flag for review">★</button>
    `;

    row.querySelector('.bank-item-check').addEventListener('click', (e) => {
      e.stopPropagation();
      toggleBankSelection(item.questionId, e.target.checked);
    });
    row.querySelector('.bank-item-flag').addEventListener('click', async (e) => {
      e.stopPropagation();
      try {
        await Api.toggleFlag(item.questionId);
        applyBankFilters();
      } catch (err) { toast(err.message, true); }
    });
    row.addEventListener('click', () => openQuestionDetail(item));

    el.appendChild(row);
  });
}

function toggleBankSelection(id, checked) {
  if (checked) state.bankSelection.add(id); else state.bankSelection.delete(id);
  updateBankSelectionCount();
}

function updateBankSelectionCount() {
  const n = state.bankSelection.size;
  document.getElementById('bank-selection-count').textContent = n ? `${n} selected` : '';
}

document.getElementById('bank-select-all').addEventListener('change', (e) => {
  state.bankItemsCache.forEach(item => toggleBankSelection(item.questionId, e.target.checked));
  renderBankList(state.bankItemsCache);
});

document.getElementById('btn-build-from-selection').addEventListener('click', async () => {
  if (!state.bankSelection.size) { toast('Select at least one question first', true); return; }
  try {
    const session = await Api.buildFromBank(Array.from(state.bankSelection));
    navigate('practice');
    startSession(session);
  } catch (err) { toast(err.message, true); }
});

document.querySelector('[data-action="export-csv"]').addEventListener('click', () => Api.exportBank('csv'));
document.querySelector('[data-action="export-pdf"]').addEventListener('click', () => Api.exportBank('pdf'));

function openQuestionDetail(item) {
  const modal = document.getElementById('modal-question-detail');
  const content = document.getElementById('modal-question-content');

  const attemptsHtml = (item.attempts || []).map((a, i) => `
    <div class="attempt-row">
      <p style="font-size:0.8rem;color:var(--text-dim);margin:0 0 6px;">Attempt ${i + 1} · score ${a.score}</p>
      <p style="margin:0 0 8px;font-size:0.9rem;">${escapeHtml(a.answerText)}</p>
      <p style="margin:0;font-size:0.85rem;color:var(--text-muted);">${escapeHtml(a.idealAnswerNotes || '')}</p>
    </div>
  `).join('') || '<p style="color:var(--text-dim);font-size:0.85rem;">No attempts yet.</p>';

  content.innerHTML = `
    <div class="question-tags" style="margin-bottom:14px;">
      <span class="tag">${titleCase(item.questionType.replace('_', ' '))}</span>
      <span class="tag tag-muted">${escapeHtml(item.skillTopic)}</span>
      <span class="tag tag-muted">${titleCase(item.difficulty)}</span>
    </div>
    <p class="question-text" style="font-size:1.1rem;">${escapeHtml(item.questionText)}</p>
    <h4>Attempt history</h4>
    ${attemptsHtml}
    <div style="display:flex;gap:10px;margin-top:20px;">
      <button class="btn btn-primary" id="modal-answer-btn">${item.answered ? 'Re-attempt →' : 'Answer this →'}</button>
    </div>
  `;

  document.getElementById('modal-answer-btn').addEventListener('click', async () => {
    try {
      const session = await Api.buildFromBank([item.questionId]);
      closeModal();
      navigate('practice');
      startSession(session);
    } catch (err) { toast(err.message, true); }
  });

  modal.classList.remove('hidden');
}

function closeModal() { document.getElementById('modal-question-detail').classList.add('hidden'); }
document.querySelector('[data-action="close-modal"]').addEventListener('click', closeModal);
document.getElementById('modal-question-detail').addEventListener('click', (e) => {
  if (e.target.id === 'modal-question-detail') closeModal();
});

/* ============================== Utilities ============================== */

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}

/* ============================== Boot ============================== */

(async function boot() {
  if (!Api.token) { showAuthTab('signin'); return; }
  try {
    const me = await Api.me();
    onAuthed({ ...me, token: Api.token });
  } catch (_) {
    Api.setToken(null);
    showAuthTab('signin');
  }
})();

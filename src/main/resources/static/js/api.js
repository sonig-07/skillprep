// api.js — thin fetch wrapper. Attaches the JWT, centralizes error handling.

const API_BASE = 'http://localhost:8080/api';;

const Api = {
  token: localStorage.getItem('skillprep_token') || null,

  setToken(token) {
    this.token = token;
    if (token) localStorage.setItem('skillprep_token', token);
    else localStorage.removeItem('skillprep_token');
  },

  async request(path, { method = 'GET', body, isForm = false, rawResponse = false } = {}) {
    const headers = {};
    if (this.token) headers['Authorization'] = `Bearer ${this.token}`;
    if (!isForm && body !== undefined) headers['Content-Type'] = 'application/json';

    const res = await fetch(API_BASE + path, {
      method,
      headers,
      body: body === undefined ? undefined : (isForm ? body : JSON.stringify(body)),
    });

    if (rawResponse) return res;

    if (!res.ok) {
      let message = `Request failed (${res.status})`;
      try {
        const errBody = await res.json();
        if (errBody.error) message = errBody.error;
      } catch (_) { /* non-JSON error body */ }
      throw new Error(message);
    }

    if (res.status === 204) return null;
    const text = await res.text();
    return text ? JSON.parse(text) : null;
  },

  get(path) { return this.request(path); },
  post(path, body) { return this.request(path, { method: 'POST', body }); },
  put(path, body) { return this.request(path, { method: 'PUT', body }); },
  postForm(path, formData) { return this.request(path, { method: 'POST', body: formData, isForm: true }); },

  // Auth
  signUp(data) { return this.post('/auth/signup', data); },
  signIn(data) { return this.post('/auth/signin', data); },
  me() { return this.get('/auth/me'); },
  updateProfile(data) { return this.put('/auth/profile', data); },

  // Resume / JD
  uploadResume(file) {
    const fd = new FormData();
    fd.append('file', file);
    return this.postForm('/resumes', fd);
  },
  listResumes() { return this.get('/resumes'); },
  setActiveResume(id) { return this.put(`/resumes/${id}/active`); },
  saveJd(data) { return this.post('/jd', data); },
  listJds() { return this.get('/jd'); },
  setActiveJd(id) { return this.put(`/jd/${id}/active`); },

  // Match
  runMatch(data) { return this.post('/match', data || {}); },
  matchHistory() { return this.get('/match/history'); },

  // Practice
  generateSession(data) { return this.post('/practice/sessions', data); },
  buildFromBank(questionIds) { return this.post('/practice/sessions/from-bank', { questionIds }); },
  getSession(id) { return this.get(`/practice/sessions/${id}`); },
  sessionHistory() { return this.get('/practice/sessions'); },
  submitAnswer(data) { return this.post('/practice/answers', data); },

  // Question bank
  browseBank(params) {
    const qs = new URLSearchParams();
    Object.entries(params || {}).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') qs.set(k, v); });
    const s = qs.toString();
    return this.get('/bank' + (s ? `?${s}` : ''));
  },
  toggleFlag(id) { return this.post(`/bank/${id}/flag`); },
  flaggedQueue() { return this.get('/bank/flagged'); },
  async exportBank(format, params) {
    const qs = new URLSearchParams({ format, ...(params || {}) });
    const res = await this.request(`/bank/export?${qs.toString()}`, { rawResponse: true });
    if (!res.ok) throw new Error('Export failed');
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = format === 'csv' ? 'skillprep-question-bank.csv' : 'skillprep-question-bank.pdf';
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  },

  // Dashboard
  dashboard() { return this.get('/dashboard'); },
  weakestSkill() { return this.get('/dashboard/weakest-skill'); },
};

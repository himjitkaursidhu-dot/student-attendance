'use strict';

// A small, dependency-free frontend. The server is the source of truth.
const $ = (id) => document.getElementById(id);
const state = {
  students: [], subjects: [], records: [],
  filteredRecords: [], reports: [], reportLow: new Set(),
  loaded: false, reportsLoaded: false, busy: false
};

function localToday() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

// All API errors, including non-JSON server errors, become visible messages.
async function api(path, options = {}) {
  let response;
  try {
    response = await fetch(`/api${path}`, {
      ...options,
      headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers }
    });
  } catch (error) {
    throw new Error('Cannot reach the server. Check that the Spring Boot application is running, then refresh.');
  }
  const text = await response.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch (error) {
    if (response.ok) throw new Error('The server returned an unexpected response. Please refresh and try again.');
  }
  if (!response.ok) throw new Error(data && data.message ? data.message : `Request failed (${response.status}). Please try again.`);
  return data;
}

function feedback(message, type = 'success') {
  const box = $('feedback');
  box.textContent = message;
  box.className = `feedback ${type}`;
  box.setAttribute('role', type === 'error' ? 'alert' : 'status');
  box.hidden = false;
  if (type === 'error') box.scrollIntoView({ block: 'nearest' });
}

// All asynchronous event handlers pass through here: no unhandled rejections.
async function run(task) {
  if (state.busy) { feedback('An operation is in progress. Please wait a moment.', 'info'); return; }
  state.busy = true;
  $('main').setAttribute('aria-busy', 'true');
  const buttons = Array.from(document.querySelectorAll('button'));
  const previousDisabled = buttons.map((button) => button.disabled);
  buttons.forEach((button) => { button.disabled = true; });
  try { await task(); }
  catch (error) { feedback(error.message || 'Something went wrong. Please try again.', 'error'); }
  finally {
    state.busy = false;
    $('main').removeAttribute('aria-busy');
    buttons.forEach((button, index) => { if (button.isConnected) button.disabled = previousDisabled[index]; });
    updateMarkProgress();
  }
}

// DOM helpers always use textContent, including user-entered names and emails.
function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined && text !== null) node.textContent = String(text);
  if (className) node.className = className;
  return node;
}
function cell(row, text) { const node = element('td', text); row.append(node); return node; }
function personCell(row, name, detail) {
  const node = cell(row);
  node.append(element('span', name, 'cell-title'), element('span', detail, 'cell-subtitle'));
}
function empty(tbody, columns, message) {
  const row = element('tr');
  const node = cell(row, message);
  node.colSpan = columns;
  node.className = 'empty';
  tbody.append(row);
}
function badge(status, type) { return element('span', status, type); }
function action(label, handler, danger = false) {
  const button = element('button', label, `text-button${danger ? ' danger' : ''}`);
  button.type = 'button';
  button.addEventListener('click', handler);
  return button;
}
function reportKey(row) { return `${row.studentId}:${row.subjectId}`; }
function shortDate(value) {
  if (!value) return '—';
  const parts = String(value).split('-').map(Number);
  return new Date(parts[0], parts[1] - 1, parts[2]).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

function navigate() {
  const allowed = ['students', 'subjects', 'attendance', 'records', 'reports'];
  const requested = location.hash.slice(1);
  const page = allowed.includes(requested) ? requested : 'students';
  document.querySelectorAll('.page').forEach((section) => { section.hidden = section.id !== page; });
  document.querySelectorAll('[data-page]').forEach((link) => {
    if (link.dataset.page === page) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });
  document.title = `${$(page + '-title').textContent} | Student Attendance Management`;
}

// Refresh reference data first, then the current filtered views.
async function loadAll() {
  const [students, subjects, records] = await Promise.all([
    api('/students'), api('/subjects'), api('/attendance')
  ]);
  state.students = students;
  state.subjects = subjects;
  state.records = records;
  state.loaded = true;
  renderStudents();
  renderSubjects();
  populateSelects();
  renderMarkGrid();
  const views = await Promise.allSettled([loadRecords(), loadReports()]);
  const failures = views.filter((result) => result.status === 'rejected');
  if (failures.length) throw new Error(failures.map((result) => result.reason.message).join(' '));
}

function populateSelects() {
  document.querySelectorAll('[data-student-select]').forEach((select) => {
    const previous = select.value;
    select.replaceChildren(new Option('All students', ''));
    state.students.forEach((student) => select.add(new Option(`${student.name} · ID ${student.studentId}`, student.studentId)));
    select.value = Array.from(select.options).some((option) => option.value === previous) ? previous : '';
  });
  document.querySelectorAll('[data-subject-select]').forEach((select) => {
    const previous = select.value;
    select.replaceChildren(new Option(select.id === 'mark-subject' ? 'Choose a subject' : 'All subjects', ''));
    state.subjects.forEach((subject) => select.add(new Option(`${subject.subjectName} (${subject.subjectCode})`, subject.subjectId)));
    select.value = Array.from(select.options).some((option) => option.value === previous) ? previous : '';
  });
}

// Student directory and editable form.
function renderStudents() {
  const query = $('student-search').value.trim().toLowerCase();
  const students = state.students.filter((student) => [student.name, student.email, student.studentId].some((value) => String(value).toLowerCase().includes(query)));
  $('student-count').textContent = `${students.length} of ${state.students.length} students`;
  const tbody = $('student-rows');
  tbody.replaceChildren();
  students.forEach((student) => {
    const row = element('tr');
    cell(row, student.studentId);
    personCell(row, student.name, student.email);
    cell(row, student.department);
    cell(row, student.semester);
    const actions = element('div', null, 'actions');
    actions.append(action('Edit', () => editStudent(student)), action('Delete', () => run(async () => {
      if (!confirm(`Delete ${student.name} (ID ${student.studentId})? All of this student's attendance records will also be permanently deleted.`)) return;
      const result = await api(`/students/${student.studentId}`, { method: 'DELETE' });
      if ($('student-edit-id').value === String(student.studentId)) resetStudentForm();
      await refreshAfterChange(result.message || 'Student deleted.');
    }), true));
    cell(row).append(actions);
    tbody.append(row);
  });
  if (!students.length) empty(tbody, 5, query ? 'No students match that name, email, or ID.' : 'No students yet. Add your first student using the form.');
}
function editStudent(student) {
  $('student-edit-id').value = student.studentId;
  $('student-name').value = student.name;
  $('student-email').value = student.email;
  $('student-department').value = student.department;
  $('student-semester').value = student.semester;
  $('student-form-title').textContent = `Edit student #${student.studentId}`;
  $('student-save').textContent = 'Save changes';
  $('student-cancel').hidden = false;
  $('student-name').focus();
}
function resetStudentForm() {
  $('student-form').reset();
  $('student-edit-id').value = '';
  $('student-form-title').textContent = 'Add student';
  $('student-save').textContent = 'Add student';
  $('student-cancel').hidden = true;
}

function renderSubjects() {
  $('subject-count').textContent = `${state.subjects.length} subject${state.subjects.length === 1 ? '' : 's'}`;
  const tbody = $('subject-rows');
  tbody.replaceChildren();
  state.subjects.forEach((subject) => {
    const row = element('tr');
    cell(row, subject.subjectId);
    cell(row, subject.subjectName);
    cell(row, subject.subjectCode);
    const actions = element('div', null, 'actions');
    actions.append(action('Edit', () => {
      $('subject-edit-id').value = subject.subjectId;
      $('subject-name').value = subject.subjectName;
      $('subject-code').value = subject.subjectCode;
      $('subject-form-title').textContent = `Edit subject #${subject.subjectId}`;
      $('subject-save').textContent = 'Save changes';
      $('subject-cancel').hidden = false;
      $('subject-name').focus();
    }), action('Delete', () => run(async () => {
      if (!confirm(`Delete ${subject.subjectName} (${subject.subjectCode})? All attendance records for this subject will also be permanently deleted.`)) return;
      const result = await api(`/subjects/${subject.subjectId}`, { method: 'DELETE' });
      if ($('subject-edit-id').value === String(subject.subjectId)) resetSubjectForm();
      await refreshAfterChange(result.message || 'Subject deleted.');
    }), true));
    cell(row).append(actions);
    tbody.append(row);
  });
  if (!state.subjects.length) empty(tbody, 4, 'No subjects yet. Add a subject using the form.');
}
function resetSubjectForm() {
  $('subject-form').reset();
  $('subject-edit-id').value = '';
  $('subject-form-title').textContent = 'Add subject';
  $('subject-save').textContent = 'Add subject';
  $('subject-cancel').hidden = true;
}

// Keep a successful write distinguishable from a later refresh failure.
async function refreshAfterChange(message) {
  try { await loadAll(); feedback(message); }
  catch (error) { throw new Error(`${message} However, the latest data could not be refreshed: ${error.message} Refresh the page before making another change.`); }
}

// Every row starts with a blank choice, even when correcting a saved register.
function renderMarkGrid() {
  const tbody = $('mark-rows');
  tbody.replaceChildren();
  if (!$('mark-subject').value || !$('mark-date').value) {
    empty(tbody, 5, 'Choose a subject and class date to open the register.');
    updateMarkProgress();
    return;
  }
  const saved = new Map(state.records.filter((record) => String(record.subjectId) === $('mark-subject').value && record.attendanceDate === $('mark-date').value).map((record) => [String(record.studentId), record.status]));
  state.students.forEach((student) => {
    const row = element('tr');
    cell(row, student.studentId);
    personCell(row, student.name, student.email);
    cell(row, student.department);
    const existing = saved.get(String(student.studentId));
    cell(row).append(existing ? badge(existing === 'PRESENT' ? 'Present' : 'Absent', existing.toLowerCase()) : badge('Not recorded', 'none'));
    const select = element('select', null, 'status-choice');
    select.required = true;
    select.dataset.studentId = student.studentId;
    select.setAttribute('aria-label', `Attendance for ${student.name}, ID ${student.studentId}`);
    select.add(new Option('Choose status…', ''));
    select.add(new Option('Present', 'PRESENT'));
    select.add(new Option('Absent', 'ABSENT'));
    select.addEventListener('change', updateMarkProgress);
    cell(row).append(select);
    tbody.append(row);
  });
  if (!state.students.length) empty(tbody, 5, 'Add students before marking attendance.');
  updateMarkProgress();
}
function updateMarkProgress() {
  const choices = Array.from(document.querySelectorAll('.status-choice'));
  const chosen = choices.filter((select) => select.value).length;
  $('mark-progress').textContent = choices.length ? `${chosen} of ${choices.length} students marked. Every student needs an explicit choice.` : 'Choose a subject and date. At least one student is required.';
  // Leave enabled while incomplete so native required-field feedback can guide the user.
  $('mark-save').disabled = state.busy || !state.loaded || !choices.length || !$('mark-subject').value || !$('mark-date').value;
}

// Filters are sent to the server, using inclusive date bounds.
function filterQuery(prefix) {
  const from = $(`${prefix}-from`).value;
  const to = $(`${prefix}-to`).value;
  if (from && to && from > to) throw new Error('The From date must be on or before the To date.');
  const params = new URLSearchParams();
  [['studentId', 'student'], ['subjectId', 'subject'], ['from', 'from'], ['to', 'to']].forEach(([key, suffix]) => {
    const value = $(`${prefix}-${suffix}`).value;
    if (value) params.set(key, value);
  });
  return params.size ? `?${params.toString()}` : '';
}
async function loadRecords() {
  try {
    const rows = await api(`/attendance${filterQuery('records')}`);
    state.filteredRecords = rows.sort((a, b) => b.attendanceDate.localeCompare(a.attendanceDate) || b.attendanceId - a.attendanceId);
    renderRecords();
  } catch (error) {
    state.filteredRecords = [];
    $('record-rows').replaceChildren();
    empty($('record-rows'), 5, 'Records could not be loaded. Correct filters or refresh to retry.');
    $('records-summary').textContent = 'Results unavailable';
    throw error;
  }
}
function renderRecords() {
  const tbody = $('record-rows');
  tbody.replaceChildren();
  $('records-summary').textContent = `${state.filteredRecords.length} matching record${state.filteredRecords.length === 1 ? '' : 's'}`;
  state.filteredRecords.forEach((record) => {
    const row = element('tr');
    cell(row, shortDate(record.attendanceDate));
    personCell(row, record.studentName, `ID ${record.studentId}`);
    personCell(row, record.subjectName, record.subjectCode);
    cell(row).append(badge(record.status === 'PRESENT' ? 'Present' : 'Absent', record.status.toLowerCase()));
    cell(row).append(action('Delete', () => run(async () => {
      if (!confirm(`Delete the ${record.status.toLowerCase()} record for ${record.studentName}, ${record.subjectCode}, on ${record.attendanceDate}? This also changes their report. To change status instead, re-save that class in Mark attendance.`)) return;
      const result = await api(`/attendance/${record.attendanceId}`, { method: 'DELETE' });
      await refreshAfterChange(result.message || 'Attendance record deleted.');
    }), true));
    tbody.append(row);
  });
  if (!state.filteredRecords.length) empty(tbody, 5, 'No saved records match these filters.');
}
async function loadReports() {
  state.reportsLoaded = false;
  try {
    const query = filterQuery('reports');
    const [reports, low] = await Promise.all([api(`/attendance/reports${query}`), api(`/attendance/defaulters${query}`)]);
    state.reports = reports;
    state.reportLow = new Set(low.map(reportKey));
    state.reportsLoaded = true;
    renderReports();
  } catch (error) {
    state.reports = [];
    state.reportLow = new Set();
    $('report-rows').replaceChildren();
    empty($('report-rows'), 7, 'Reports could not be loaded. Correct filters or refresh to retry.');
    $('reports-summary').textContent = 'Results unavailable';
    throw error;
  }
}
function shownReports() {
  return $('only-low').checked ? state.reports.filter((row) => state.reportLow.has(reportKey(row))) : state.reports;
}
function renderReports() {
  const tbody = $('report-rows');
  tbody.replaceChildren();
  const reports = shownReports();
  $('reports-summary').textContent = `${reports.length} student–subject pairs shown · ${state.reportLow.size} below 75%`;
  reports.forEach((report) => {
    const row = element('tr');
    personCell(row, report.studentName, `ID ${report.studentId}`);
    personCell(row, report.subjectName, report.subjectCode);
    cell(row, report.totalClasses);
    cell(row, report.presentClasses);
    cell(row, report.absentClasses);
    cell(row, report.totalClasses ? `${Number(report.percentage).toFixed(2)}%` : '—');
    const isLow = state.reportLow.has(reportKey(report));
    cell(row).append(!report.totalClasses ? badge('No data', 'none') : badge(isLow ? 'Below 75%' : 'Meets 75%', isLow ? 'low' : 'good'));
    tbody.append(row);
  });
  if (!reports.length) empty(tbody, 7, $('only-low').checked ? 'No recorded student–subject pairs are below 75% for these filters.' : 'No student–subject pairs found. Add students and subjects, or adjust your filters.');
}

// Event wiring.
window.addEventListener('hashchange', navigate);
$('student-search').addEventListener('input', renderStudents);
$('student-cancel').addEventListener('click', resetStudentForm);
$('subject-cancel').addEventListener('click', resetSubjectForm);
$('student-form').addEventListener('submit', (event) => {
  event.preventDefault();
  run(async () => {
    const id = $('student-edit-id').value;
    const body = {
      name: $('student-name').value.trim(), email: $('student-email').value.trim(),
      department: $('student-department').value.trim(), semester: Number($('student-semester').value)
    };
    if (!body.name || !body.email || !body.department || !Number.isInteger(body.semester) || body.semester < 1 || body.semester > 12) throw new Error('Enter a name, valid email, department, and a whole-number semester from 1 to 12.');
    await api(`/students${id ? '/' + id : ''}`, { method: id ? 'PUT' : 'POST', body: JSON.stringify(body) });
    resetStudentForm();
    await refreshAfterChange(id ? 'Student updated.' : 'Student added.');
  });
});
$('subject-form').addEventListener('submit', (event) => {
  event.preventDefault();
  run(async () => {
    const id = $('subject-edit-id').value;
    const body = { subjectName: $('subject-name').value.trim(), subjectCode: $('subject-code').value.trim() };
    if (!body.subjectName || !body.subjectCode) throw new Error('Enter a subject name and code.');
    await api(`/subjects${id ? '/' + id : ''}`, { method: id ? 'PUT' : 'POST', body: JSON.stringify(body) });
    resetSubjectForm();
    await refreshAfterChange(id ? 'Subject updated.' : 'Subject added.');
  });
});
$('mark-subject').addEventListener('change', renderMarkGrid);
$('mark-date').addEventListener('change', renderMarkGrid);
$('attendance-form').addEventListener('submit', (event) => {
  event.preventDefault();
  run(async () => {
    const subjectId = Number($('mark-subject').value);
    const attendanceDate = $('mark-date').value;
    if (!subjectId || !attendanceDate) throw new Error('Choose a subject and class date.');
    if (attendanceDate > localToday()) throw new Error('Attendance cannot be recorded for a future date.');
    const choices = Array.from(document.querySelectorAll('.status-choice'));
    if (!choices.length || choices.some((select) => !['PRESENT', 'ABSENT'].includes(select.value))) throw new Error('Explicitly choose Present or Absent for every displayed student before saving.');
    const body = choices.map((select) => ({ studentId: Number(select.dataset.studentId), subjectId, attendanceDate, status: select.value }));
    const result = await api('/attendance/batch', { method: 'POST', body: JSON.stringify(body) });
    await refreshAfterChange(result.message || `Attendance saved for ${body.length} students.`);
  });
});
['records', 'reports'].forEach((prefix) => {
  $(prefix + '-filter').addEventListener('submit', (event) => {
    event.preventDefault();
    run(async () => {
      await (prefix === 'records' ? loadRecords() : loadReports());
      feedback(`${prefix === 'records' ? 'Record' : 'Report'} filters applied.`);
    });
  });
  $(prefix + '-reset').addEventListener('click', () => run(async () => {
    $(prefix + '-filter').reset();
    if (prefix === 'reports') $('only-low').checked = false;
    await (prefix === 'records' ? loadRecords() : loadReports());
    feedback('Filters reset.');
  }));
});
$('only-low').addEventListener('change', () => { if (state.reportsLoaded) renderReports(); });
$('mark-date').value = localToday();
$('mark-date').max = localToday();
navigate();
run(async () => {
  feedback('Loading data…', 'info');
  await loadAll();
  feedback('Data loaded.', 'info');
});

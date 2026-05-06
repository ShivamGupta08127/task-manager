import React, { useCallback, useEffect, useMemo, useState } from "react";
import ReactDOM from "react-dom/client";
import axios from "axios";
import "./styles.css";

const defaultApiBaseUrl =
  window.location.port === "5173"
    ? `${window.location.protocol}//${window.location.hostname}:8080/api`
    : "/api";
const apiBaseUrl = defaultApiBaseUrl;
const api = axios.create({ baseURL: apiBaseUrl });
const TASK_STATUSES = ["TODO", "IN_PROGRESS", "DONE"];
const emptyTask = { projectId: "", title: "", description: "", dueDate: "", assignedToId: "", status: "TODO" };

function App() {
  const [token, setToken] = useState(localStorage.getItem("token") || "");
  const [role, setRole] = useState(localStorage.getItem("role") || "");
  const [form, setForm] = useState({ email: "", password: "", fullName: "", role: "MEMBER" });
  const [isSignup, setIsSignup] = useState(false);
  const [dashboard, setDashboard] = useState({});
  const [tasks, setTasks] = useState([]);
  const [projects, setProjects] = useState([]);
  const [users, setUsers] = useState([]);
  const [projectMembers, setProjectMembers] = useState([]);
  const [currentUser, setCurrentUser] = useState(null);
  const [selectedProjectId, setSelectedProjectId] = useState("");
  const [projectForm, setProjectForm] = useState({ name: "", description: "" });
  const [memberForm, setMemberForm] = useState({ projectId: "", userId: "" });
  const [taskForm, setTaskForm] = useState(emptyTask);
  const [editingTaskId, setEditingTaskId] = useState(null);
  const [editForm, setEditForm] = useState(emptyTask);
  const [message, setMessage] = useState("");

  const authHeaders = useCallback(() => (token ? { Authorization: `Bearer ${token}` } : {}), [token]);

  const showError = (error, fallback) => {
    const text = error.response?.data?.message || error.message || fallback;
    setMessage(text);
    window.alert(text);
  };

  const refreshData = useCallback(async () => {
    if (!token) return;
    const headers = authHeaders();
    const [meRes, dashRes, tasksRes, projectsRes] = await Promise.all([
      api.get("/users/me", { headers }),
      api.get("/dashboard", { headers }),
      api.get("/tasks", { headers }),
      api.get("/projects", { headers }),
    ]);
    setCurrentUser(meRes.data);
    setDashboard(dashRes.data);
    setTasks(tasksRes.data);
    setProjects(projectsRes.data);

    if (!selectedProjectId && projectsRes.data.length > 0) {
      const firstProjectId = String(projectsRes.data[0].id);
      setSelectedProjectId(firstProjectId);
      setTaskForm((draft) => ({ ...draft, projectId: firstProjectId }));
    }

    if (meRes.data.role === "ADMIN") {
      const usersRes = await api.get("/users", { headers });
      setUsers(usersRes.data);
    } else {
      setUsers([]);
    }
  }, [token, authHeaders, selectedProjectId]);

  useEffect(() => {
    if (!token) return;
    refreshData().catch((error) => showError(error, "Could not load dashboard"));
  }, [token, refreshData]);

  useEffect(() => {
    if (!token || !selectedProjectId) {
      setProjectMembers([]);
      return;
    }
    api
      .get(`/projects/${selectedProjectId}/members`, { headers: authHeaders() })
      .then(({ data }) => setProjectMembers(data))
      .catch(() => setProjectMembers([]));
  }, [token, selectedProjectId, authHeaders]);

  const selectedProject = useMemo(
    () => projects.find((project) => String(project.id) === String(selectedProjectId)),
    [projects, selectedProjectId]
  );

  const filteredTasks = useMemo(() => {
    if (!selectedProjectId) return tasks;
    return tasks.filter((task) => String(task.project?.id) === String(selectedProjectId));
  }, [tasks, selectedProjectId]);

  const availableAssignees = selectedProjectId ? projectMembers : role === "ADMIN" ? users : currentUser ? [currentUser] : [];

  const submitAuth = async () => {
    try {
      const endpoint = isSignup ? "/auth/signup" : "/auth/login";
      const payload = isSignup
        ? { fullName: form.fullName, email: form.email, password: form.password, role: form.role }
        : { email: form.email, password: form.password };
      const { data } = await api.post(endpoint, payload);
      localStorage.setItem("token", data.token);
      localStorage.setItem("role", data.role);
      localStorage.setItem("fullName", data.fullName);
      setRole(data.role);
      setToken(data.token);
      setMessage("");
    } catch (error) {
      showError(error, "Authentication failed");
    }
  };

  const logout = () => {
    localStorage.clear();
    setToken("");
    setRole("");
    setCurrentUser(null);
    setTasks([]);
    setProjects([]);
    setUsers([]);
    setProjectMembers([]);
    setSelectedProjectId("");
  };

  const createProject = async () => {
    try {
      const { data } = await api.post("/projects", projectForm, { headers: authHeaders() });
      setProjectForm({ name: "", description: "" });
      setSelectedProjectId(String(data.id));
      setTaskForm((draft) => ({ ...draft, projectId: String(data.id) }));
      await refreshData();
    } catch (error) {
      showError(error, "Could not create project");
    }
  };

  const addMember = async () => {
    try {
      if (!memberForm.projectId || !memberForm.userId) return;
      await api.post(`/projects/${memberForm.projectId}/members/${memberForm.userId}`, {}, { headers: authHeaders() });
      setMemberForm({ projectId: selectedProjectId, userId: "" });
      await refreshData();
      const { data } = await api.get(`/projects/${memberForm.projectId}/members`, { headers: authHeaders() });
      setProjectMembers(data);
    } catch (error) {
      showError(error, "Could not add member");
    }
  };

  const createTask = async () => {
    try {
      await api.post("/tasks", taskForm, { headers: authHeaders() });
      setTaskForm({ ...emptyTask, projectId: selectedProjectId });
      await refreshData();
    } catch (error) {
      showError(error, "Could not create task");
    }
  };

  const beginEdit = (task) => {
    setEditingTaskId(task.id);
    setEditForm({
      projectId: String(task.project?.id || ""),
      title: task.title || "",
      description: task.description || "",
      dueDate: task.dueDate || "",
      assignedToId: task.assignedTo?.id ? String(task.assignedTo.id) : "",
      status: task.status,
    });
  };

  const updateTask = async (taskId) => {
    try {
      await api.put(`/tasks/${taskId}`, editForm, { headers: authHeaders() });
      setEditingTaskId(null);
      setEditForm(emptyTask);
      await refreshData();
    } catch (error) {
      showError(error, "Could not update task");
    }
  };

  const handleProjectSelect = (projectId) => {
    setSelectedProjectId(projectId);
    setTaskForm((draft) => ({ ...draft, projectId }));
    setMemberForm((draft) => ({ ...draft, projectId }));
    setEditingTaskId(null);
  };

  if (!token) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <h1>Team Task Manager</h1>
          <p>Sign in to manage projects, teams, tasks, and progress.</p>
          {isSignup && (
            <input placeholder="Full name" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
          )}
          <input placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <input
            placeholder="Password"
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
          />
          {isSignup && (
            <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
              <option value="MEMBER">Member</option>
              <option value="ADMIN">Admin</option>
            </select>
          )}
          <button type="button" onClick={submitAuth}>{isSignup ? "Create account" : "Login"}</button>
          <button type="button" className="secondary" onClick={() => setIsSignup(!isSignup)}>
            {isSignup ? "Use existing account" : "Create new account"}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <h1>Team Task Manager</h1>
          <p>
            {localStorage.getItem("fullName") || currentUser?.fullName || "User"} <span>{role}</span>
          </p>
        </div>
        <button type="button" className="secondary" onClick={logout}>Logout</button>
      </header>

      {message && <div className="notice">{message}</div>}

      <section className="metrics">
        <div><small>Visible Tasks</small><strong>{dashboard.totalTasks || 0}</strong></div>
        <div><small>My Tasks</small><strong>{dashboard.myTasks || 0}</strong></div>
        <div><small>Projects</small><strong>{dashboard.totalProjects || 0}</strong></div>
        <div><small>To Do</small><strong>{dashboard.todo || 0}</strong></div>
        <div><small>In Progress</small><strong>{dashboard.inProgress || 0}</strong></div>
        <div className="danger"><small>Overdue</small><strong>{dashboard.overdue || 0}</strong></div>
      </section>

      <main className="workspace">
        <aside className="sidebar">
          <div className="section-title">
            <h2>Projects</h2>
            <span>{projects.length}</span>
          </div>
          <div className="project-list">
            {projects.map((project) => (
              <button
                type="button"
                key={project.id}
                className={String(project.id) === String(selectedProjectId) ? "project active" : "project"}
                onClick={() => handleProjectSelect(String(project.id))}
              >
                <strong>{project.name}</strong>
                <small>{project.description || "No description"}</small>
              </button>
            ))}
            {projects.length === 0 && <p className="empty">No projects available yet.</p>}
          </div>

          {role === "ADMIN" && (
            <div className="admin-tools">
              <h3>Project Management</h3>
              <input placeholder="Project name" value={projectForm.name} onChange={(e) => setProjectForm({ ...projectForm, name: e.target.value })} />
              <textarea placeholder="Description" value={projectForm.description} onChange={(e) => setProjectForm({ ...projectForm, description: e.target.value })} />
              <button type="button" onClick={createProject}>Create Project</button>
            </div>
          )}
        </aside>

        <section className="content">
          <div className="content-head">
            <div>
              <h2>{selectedProject?.name || "Task Board"}</h2>
              <p>{selectedProject?.description || "Create, assign, update, and track project tasks."}</p>
            </div>
          </div>

          {selectedProjectId && (
            <section className="team-strip">
              <div>
                <h3>Team</h3>
                <p>{projectMembers.length ? projectMembers.map((member) => member.fullName).join(", ") : "No team members assigned."}</p>
              </div>
              {role === "ADMIN" && (
                <div className="member-form">
                  <select value={memberForm.userId} onChange={(e) => setMemberForm({ ...memberForm, projectId: selectedProjectId, userId: e.target.value })}>
                    <option value="">Add user</option>
                    {users.map((user) => (
                      <option key={user.id} value={user.id}>{user.fullName} ({user.email})</option>
                    ))}
                  </select>
                  <button type="button" onClick={addMember}>Add</button>
                </div>
              )}
            </section>
          )}

          {projects.length > 0 && (
            <section className="task-editor">
              <h3>Create Task</h3>
              <div className="form-grid">
                <select value={taskForm.projectId} onChange={(e) => handleProjectSelect(e.target.value)}>
                  <option value="">Select project</option>
                  {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
                </select>
                <input placeholder="Task title" value={taskForm.title} onChange={(e) => setTaskForm({ ...taskForm, title: e.target.value })} />
                <select value={taskForm.assignedToId} onChange={(e) => setTaskForm({ ...taskForm, assignedToId: e.target.value })}>
                  <option value="">Unassigned</option>
                  {availableAssignees.map((user) => <option key={user.id} value={user.id}>{user.fullName}</option>)}
                </select>
                <input type="date" value={taskForm.dueDate} onChange={(e) => setTaskForm({ ...taskForm, dueDate: e.target.value })} />
              </div>
              <textarea placeholder="Description" value={taskForm.description} onChange={(e) => setTaskForm({ ...taskForm, description: e.target.value })} />
              <button type="button" onClick={createTask}>Create Task</button>
            </section>
          )}

          <section className="task-board">
            {filteredTasks.map((task) => {
              const isEditing = editingTaskId === task.id;
              return (
                <article key={task.id} className={`task-card ${task.status.toLowerCase()}`}>
                  {isEditing ? (
                    <>
                      <input value={editForm.title} onChange={(e) => setEditForm({ ...editForm, title: e.target.value })} />
                      <textarea value={editForm.description} onChange={(e) => setEditForm({ ...editForm, description: e.target.value })} />
                      <div className="form-grid compact">
                        <select value={editForm.status} onChange={(e) => setEditForm({ ...editForm, status: e.target.value })}>
                          {TASK_STATUSES.map((status) => <option key={status} value={status}>{status.replace("_", " ")}</option>)}
                        </select>
                        <select value={editForm.assignedToId} onChange={(e) => setEditForm({ ...editForm, assignedToId: e.target.value })}>
                          <option value="">Unassigned</option>
                          {availableAssignees.map((user) => <option key={user.id} value={user.id}>{user.fullName}</option>)}
                        </select>
                        <input type="date" value={editForm.dueDate} onChange={(e) => setEditForm({ ...editForm, dueDate: e.target.value })} />
                      </div>
                      <div className="actions">
                        <button type="button" onClick={() => updateTask(task.id)}>Save</button>
                        <button type="button" className="secondary" onClick={() => setEditingTaskId(null)}>Cancel</button>
                      </div>
                    </>
                  ) : (
                    <>
                      <div className="task-head">
                        <h3>{task.title}</h3>
                        <span>{task.status.replace("_", " ")}</span>
                      </div>
                      <p>{task.description || "No description"}</p>
                      <dl>
                        <div><dt>Assignee</dt><dd>{task.assignedTo?.fullName || "Unassigned"}</dd></div>
                        <div><dt>Due</dt><dd>{task.dueDate || "No due date"}</dd></div>
                      </dl>
                      <button type="button" className="secondary" onClick={() => beginEdit(task)}>Update Task</button>
                    </>
                  )}
                </article>
              );
            })}
            {filteredTasks.length === 0 && <p className="empty board-empty">No tasks in this project yet.</p>}
          </section>
        </section>
      </main>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<App />);

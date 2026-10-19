"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Check, CheckSquare, FolderKanban, Plus, Trash2, X } from "lucide-react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import type { Project } from "@/types/entities";

interface ProjectDetailModalProps {
  project: Project | null;
  isOpen: boolean;
  onClose: () => void;
}

function ProjectDetailForm({ project, onClose }: { project: Project; onClose: () => void }) {
  const { snapshot, updateProject, deleteProject, createFromCapture, toggleTask } = usePersonalOS();
  const [title, setTitle] = useState(project.title);
  const [description, setDescription] = useState(project.description || "");
  const [status, setStatus] = useState<Project["status"]>(project.status || "active");
  const [color, setColor] = useState(project.color || "#3b82f6");
  const [targetDate, setTargetDate] = useState(project.targetDate || "");
  const [progress] = useState(project.progress || 0);
  const [newTaskTitle, setNewTaskTitle] = useState("");
  const [saving, setSaving] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [onClose]);

  const projectTasks = snapshot.tasks.filter((t) => t.projectId === project.id);
  const completedTasks = projectTasks.filter((t) => t.status === "completed");
  const calculatedProgress = projectTasks.length > 0
    ? Math.round((completedTasks.length / projectTasks.length) * 100)
    : progress;

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    setSaving(true);
    try {
      await updateProject({
        ...project,
        title: title.trim(),
        description: description.trim() || undefined,
        status,
        color,
        targetDate: targetDate || undefined,
        progress: calculatedProgress,
      });
      onClose();
    } finally {
      setSaving(false);
    }
  }

  async function handleAddTask() {
    const trimmed = newTaskTitle.trim();
    if (!trimmed) return;
    await createFromCapture({
      type: "task",
      title: trimmed,
      destination: "today",
      priority: "medium",
    });
    setNewTaskTitle("");
  }

  async function handleDelete() {
    await deleteProject(project.id);
    onClose();
  }

  return (
    <div className="capture-modal" role="dialog" aria-modal="true" style={{ maxWidth: "620px" }}>
      <div className="capture-modal__header">
        <div>
          <span className="eyebrow"><FolderKanban size={13} /> Project Workspace</span>
          <h2>Edit Project</h2>
        </div>
        <button type="button" className="icon-button" onClick={onClose} aria-label="Close">
          <X size={18} />
        </button>
      </div>

      <form onSubmit={handleSave} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
        <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
          <input
            type="color"
            value={color}
            onChange={(e) => setColor(e.target.value)}
            title="Project accent color"
            style={{
              width: "38px",
              height: "38px",
              border: "none",
              borderRadius: "8px",
              cursor: "pointer",
              background: "transparent",
            }}
          />
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Project title..."
            style={{
              flex: 1,
              padding: "10px 14px",
              background: "var(--surface-color, #151a26)",
              border: "1px solid var(--border-color, #232b3e)",
              borderRadius: "8px",
              color: "#fff",
              fontSize: "1rem",
              fontWeight: 600,
            }}
          />
        </div>

        <div>
          <textarea
            rows={2}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Project outcome, target, or key result..."
            style={{
              width: "100%",
              padding: "8px 12px",
              background: "var(--surface-color, #151a26)",
              border: "1px solid var(--border-color, #232b3e)",
              borderRadius: "8px",
              color: "#e2e8f0",
              fontSize: "0.88rem",
              fontFamily: "inherit",
              resize: "vertical",
            }}
          />
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
          <div>
            <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "4px" }}>
              Status
            </label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as Project["status"])}
              style={{
                width: "100%",
                padding: "8px 10px",
                background: "var(--surface-color, #151a26)",
                border: "1px solid var(--border-color, #232b3e)",
                borderRadius: "6px",
                color: "#fff",
                fontSize: "0.85rem",
              }}
            >
              <option value="planning">Planning</option>
              <option value="active">Active</option>
              <option value="paused">Paused</option>
              <option value="completed">Completed</option>
              <option value="archived">Archived</option>
            </select>
          </div>

          <div>
            <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "4px" }}>
              Target Date
            </label>
            <input
              type="date"
              value={targetDate}
              onChange={(e) => setTargetDate(e.target.value)}
              style={{
                width: "100%",
                padding: "7px 10px",
                background: "var(--surface-color, #151a26)",
                border: "1px solid var(--border-color, #232b3e)",
                borderRadius: "6px",
                color: "#fff",
                fontSize: "0.85rem",
              }}
            />
          </div>
        </div>

        {/* Progress Bar */}
        <div>
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.82rem", color: "var(--muted-color, #8b9bb4)", marginBottom: "4px" }}>
            <span>Completion ({completedTasks.length}/{projectTasks.length} tasks)</span>
            <strong>{calculatedProgress}%</strong>
          </div>
          <div className="progress-line" style={{ height: "6px" }}>
            <span style={{ width: `${calculatedProgress}%`, background: color }} />
          </div>
        </div>

        {/* Project Tasks */}
        <div style={{ borderTop: "1px solid var(--border-color, #232b3e)", paddingTop: "10px" }}>
          <label style={{ fontSize: "0.82rem", fontWeight: 600, color: "#cbd5e1", display: "block", marginBottom: "6px" }}>
            <CheckSquare size={13} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} />
            Project Tasks
          </label>
          <div style={{ display: "flex", gap: "8px", marginBottom: "8px" }}>
            <input
              type="text"
              value={newTaskTitle}
              onChange={(e) => setNewTaskTitle(e.target.value)}
              onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); handleAddTask(); } }}
              placeholder="Add task to this project..."
              style={{
                flex: 1,
                padding: "6px 10px",
                background: "var(--surface-color, #151a26)",
                border: "1px solid var(--border-color, #232b3e)",
                borderRadius: "6px",
                color: "#fff",
                fontSize: "0.85rem",
              }}
            />
            <button type="button" className="text-button" onClick={handleAddTask}>
              <Plus size={13} /> Add
            </button>
          </div>

          <div style={{ maxHeight: "150px", overflowY: "auto", display: "flex", flexDirection: "column", gap: "4px" }}>
            {projectTasks.map((t) => (
              <div
                key={t.id}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "8px",
                  padding: "4px 8px",
                  background: "rgba(21, 26, 38, 0.4)",
                  borderRadius: "4px",
                  fontSize: "0.85rem",
                }}
              >
                <button
                  type="button"
                  onClick={() => toggleTask(t.id)}
                  style={{
                    width: "16px",
                    height: "16px",
                    borderRadius: "3px",
                    border: "1px solid #475569",
                    background: t.status === "completed" ? "var(--primary-color, #3b82f6)" : "transparent",
                    color: "#fff",
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    padding: 0,
                  }}
                >
                  {t.status === "completed" && <Check size={11} />}
                </button>
                <span style={{ textDecoration: t.status === "completed" ? "line-through" : "none", color: t.status === "completed" ? "#64748b" : "#e2e8f0" }}>
                  {t.title}
                </span>
              </div>
            ))}
            {projectTasks.length === 0 && (
              <small style={{ color: "var(--muted-color, #8b9bb4)" }}>No tasks yet in this project.</small>
            )}
          </div>
        </div>

        <div className="capture-modal__footer" style={{ marginTop: "12px", borderTop: "1px solid var(--border-color, #232b3e)", paddingTop: "12px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          {!confirmDelete ? (
            <button
              type="button"
              className="text-button"
              style={{ color: "#ef4444" }}
              onClick={() => setConfirmDelete(true)}
            >
              <Trash2 size={14} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} /> Delete Project
            </button>
          ) : (
            <div style={{ display: "flex", gap: "6px", alignItems: "center" }}>
              <small style={{ color: "#ef4444" }}>Confirm?</small>
              <button type="button" className="danger-button" style={{ padding: "4px 8px", fontSize: "0.8rem" }} onClick={handleDelete}>Yes, delete</button>
              <button type="button" className="text-button" onClick={() => setConfirmDelete(false)}>Cancel</button>
            </div>
          )}

          <div style={{ display: "flex", gap: "8px" }}>
            <button type="button" className="text-button" onClick={onClose}>Cancel</button>
            <button type="submit" className="primary-button" disabled={!title.trim() || saving}>
              {saving ? "Saving…" : "Save Changes"}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}

export function ProjectDetailModal({ project, isOpen, onClose }: ProjectDetailModalProps) {
  if (!isOpen || !project) return null;
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <ProjectDetailForm key={project.id} project={project} onClose={onClose} />
    </div>
  );
}

"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Calendar, CheckSquare, Flag, FolderKanban, Trash2, X } from "lucide-react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import type { Task } from "@/types/entities";
import { addDays, toLocalDate } from "@/utils/date";

interface TaskDetailModalProps {
  task: Task | null;
  isOpen: boolean;
  onClose: () => void;
}

function TaskDetailForm({ task, onClose }: { task: Task; onClose: () => void }) {
  const { snapshot, updateTask, deleteTask } = usePersonalOS();
  const [title, setTitle] = useState(task.title);
  const [description, setDescription] = useState(task.description || "");
  const [priority, setPriority] = useState<Task["priority"]>(task.priority || "none");
  const [status, setStatus] = useState<Task["status"]>(task.status || "todo");
  const [dueDate, setDueDate] = useState<string>(task.dueDate || "");
  const [projectId, setProjectId] = useState<string>(task.projectId || "");
  const [saving, setSaving] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [onClose]);

  const today = toLocalDate(new Date());
  const tomorrow = toLocalDate(addDays(new Date(), 1));

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    setSaving(true);
    try {
      const isCompleted = status === "completed";
      await updateTask({
        ...task,
        title: title.trim(),
        description: description.trim() || undefined,
        priority,
        status,
        dueDate: dueDate || undefined,
        projectId: projectId || undefined,
        completedAt: isCompleted ? (task.completedAt || new Date().toISOString()) : undefined,
      });
      onClose();
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    await deleteTask(task.id);
    onClose();
  }

  return (
    <div className="capture-modal" role="dialog" aria-modal="true" style={{ maxWidth: "560px" }}>
      <div className="capture-modal__header">
        <div>
          <span className="eyebrow"><CheckSquare size={13} /> Task Manager</span>
          <h2>Edit Task</h2>
        </div>
        <button type="button" className="icon-button" onClick={onClose} aria-label="Close">
          <X size={18} />
        </button>
      </div>

      <form onSubmit={handleSave} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
        <div>
          <label className="sr-only" htmlFor="task-title-input">Task Title</label>
          <input
            id="task-title-input"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Task title..."
            style={{
              width: "100%",
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
            rows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Description or notes about this task..."
            style={{
              width: "100%",
              padding: "10px 14px",
              background: "var(--surface-color, #151a26)",
              border: "1px solid var(--border-color, #232b3e)",
              borderRadius: "8px",
              color: "#e2e8f0",
              fontSize: "0.9rem",
              fontFamily: "inherit",
              resize: "vertical",
            }}
          />
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
          <div>
            <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "4px" }}>
              <Flag size={12} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} />
              Priority
            </label>
            <select
              value={priority}
              onChange={(e) => setPriority(e.target.value as Task["priority"])}
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
              <option value="none">None</option>
              <option value="low">Low</option>
              <option value="medium">Medium</option>
              <option value="high">High</option>
              <option value="urgent">Urgent</option>
            </select>
          </div>

          <div>
            <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "4px" }}>
              Status
            </label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as Task["status"])}
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
              <option value="inbox">Inbox</option>
              <option value="todo">To Do</option>
              <option value="in_progress">In Progress</option>
              <option value="completed">Completed</option>
              <option value="cancelled">Cancelled</option>
            </select>
          </div>
        </div>

        {/* Project Assignment */}
        <div>
          <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "4px" }}>
            <FolderKanban size={12} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} />
            Project
          </label>
          <select
            value={projectId}
            onChange={(e) => setProjectId(e.target.value)}
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
            <option value="">No Project (Standalone)</option>
            {snapshot.projects.map((p) => (
              <option key={p.id} value={p.id}>{p.title}</option>
            ))}
          </select>
        </div>

        {/* Due Date & Quick Presets */}
        <div>
          <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "4px" }}>
            <Calendar size={12} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} />
            Due Date
          </label>
          <div style={{ display: "flex", gap: "8px", alignItems: "center", marginBottom: "6px" }}>
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              style={{
                flex: 1,
                padding: "7px 10px",
                background: "var(--surface-color, #151a26)",
                border: "1px solid var(--border-color, #232b3e)",
                borderRadius: "6px",
                color: "#fff",
                fontSize: "0.85rem",
              }}
            />
            <button
              type="button"
              className="text-button"
              style={{ fontSize: "0.8rem" }}
              onClick={() => setDueDate(today)}
            >
              Today
            </button>
            <button
              type="button"
              className="text-button"
              style={{ fontSize: "0.8rem" }}
              onClick={() => setDueDate(tomorrow)}
            >
              Tomorrow
            </button>
            {dueDate && (
              <button
                type="button"
                className="text-button"
                style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)" }}
                onClick={() => setDueDate("")}
              >
                Clear
              </button>
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
              <Trash2 size={14} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} /> Delete
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

export function TaskDetailModal({ task, isOpen, onClose }: TaskDetailModalProps) {
  if (!isOpen || !task) return null;
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <TaskDetailForm key={task.id} task={task} onClose={onClose} />
    </div>
  );
}

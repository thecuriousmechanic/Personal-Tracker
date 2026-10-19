"use client";

import { useEffect, useState, type FormEvent } from "react";
import { FolderKanban, Target, Trash2, X } from "lucide-react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import type { Goal } from "@/types/entities";

interface GoalDetailModalProps {
  goal: Goal | null;
  isOpen: boolean;
  onClose: () => void;
}

function GoalDetailForm({ goal, onClose }: { goal: Goal; onClose: () => void }) {
  const { snapshot, updateGoal, deleteGoal } = usePersonalOS();
  const [title, setTitle] = useState(goal.title);
  const [description, setDescription] = useState(goal.description || "");
  const [status, setStatus] = useState<Goal["status"]>(goal.status || "active");
  const [targetDate, setTargetDate] = useState(goal.targetDate || "");
  const [progress, setProgress] = useState(goal.progress || 0);
  const [saving, setSaving] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [onClose]);

  const linkedProjects = snapshot.projects.filter((p) => p.goalId === goal.id);

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    setSaving(true);
    try {
      await updateGoal({
        ...goal,
        title: title.trim(),
        description: description.trim() || undefined,
        status,
        targetDate: targetDate || undefined,
        progress,
      });
      onClose();
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    await deleteGoal(goal.id);
    onClose();
  }

  return (
    <div className="capture-modal" role="dialog" aria-modal="true" style={{ maxWidth: "560px" }}>
      <div className="capture-modal__header">
        <div>
          <span className="eyebrow"><Target size={13} /> Strategic Goal</span>
          <h2>Edit Goal</h2>
        </div>
        <button type="button" className="icon-button" onClick={onClose} aria-label="Close">
          <X size={18} />
        </button>
      </div>

      <form onSubmit={handleSave} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
        <div>
          <label className="sr-only" htmlFor="goal-title-input">Goal Title</label>
          <input
            id="goal-title-input"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Goal title..."
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
            rows={2}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Why this goal matters and what success looks like..."
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
              onChange={(e) => setStatus(e.target.value as Goal["status"])}
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
              <option value="active">Active</option>
              <option value="paused">Paused</option>
              <option value="completed">Completed</option>
              <option value="cancelled">Cancelled</option>
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

        {/* Progress Slider */}
        <div>
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.82rem", color: "var(--muted-color, #8b9bb4)", marginBottom: "4px" }}>
            <span>Progress</span>
            <strong>{progress}%</strong>
          </div>
          <input
            type="range"
            min="0"
            max="100"
            step="5"
            value={progress}
            onChange={(e) => setProgress(Number(e.target.value))}
            style={{ width: "100%", accentColor: "var(--primary-color, #3b82f6)" }}
          />
        </div>

        {/* Linked Projects */}
        {linkedProjects.length > 0 && (
          <div style={{ borderTop: "1px solid var(--border-color, #232b3e)", paddingTop: "10px" }}>
            <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "6px" }}>
              <FolderKanban size={12} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} />
              Contributing Projects ({linkedProjects.length})
            </label>
            <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
              {linkedProjects.map((p) => (
                <div
                  key={p.id}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    padding: "6px 10px",
                    background: "rgba(21, 26, 38, 0.4)",
                    borderRadius: "6px",
                    fontSize: "0.85rem",
                    color: "#e2e8f0",
                  }}
                >
                  <span>{p.title}</span>
                  <small style={{ color: p.color }}>{p.progress}%</small>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="capture-modal__footer" style={{ marginTop: "12px", borderTop: "1px solid var(--border-color, #232b3e)", paddingTop: "12px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          {!confirmDelete ? (
            <button
              type="button"
              className="text-button"
              style={{ color: "#ef4444" }}
              onClick={() => setConfirmDelete(true)}
            >
              <Trash2 size={14} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} /> Delete Goal
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

export function GoalDetailModal({ goal, isOpen, onClose }: GoalDetailModalProps) {
  if (!isOpen || !goal) return null;
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <GoalDetailForm key={goal.id} goal={goal} onClose={onClose} />
    </div>
  );
}

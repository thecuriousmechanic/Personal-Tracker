"use client";

import { useEffect, useState, type FormEvent } from "react";
import { BookOpen, GraduationCap, Trash2, X } from "lucide-react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import type { LearningItem } from "@/types/entities";

interface LearningItemModalProps {
  item: LearningItem | null;
  isOpen: boolean;
  onClose: () => void;
  defaultParentId?: string;
}

function LearningItemForm({
  item,
  onClose,
  defaultParentId,
}: {
  item: LearningItem | null;
  onClose: () => void;
  defaultParentId?: string;
}) {
  const { snapshot, updateLearningItem, createLearningItem, deleteLearningItem } = usePersonalOS();
  const isEditing = Boolean(item);

  const [title, setTitle] = useState(item?.title || "");
  const [description, setDescription] = useState(item?.description || "");
  const [type, setType] = useState<LearningItem["type"]>(item?.type || "topic");
  const [status, setStatus] = useState<LearningItem["status"]>(item?.status || "learning");
  const [mastery, setMastery] = useState<number>(item?.mastery ?? 25);
  const [parentId, setParentId] = useState<string>(item?.parentId || defaultParentId || "");
  const [saving, setSaving] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [onClose]);

  // Parents can be domains or subjects (excluding itself to avoid circular refs)
  const potentialParents = snapshot.learningItems.filter(
    (li) => li.id !== item?.id && (li.type === "domain" || li.type === "subject"),
  );

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    setSaving(true);
    try {
      if (isEditing && item) {
        await updateLearningItem({
          ...item,
          title: title.trim(),
          description: description.trim() || undefined,
          type,
          status,
          mastery: Math.min(100, Math.max(0, mastery)),
          parentId: parentId || undefined,
        });
      } else {
        await createLearningItem({
          title: title.trim(),
          description: description.trim() || undefined,
          type,
          status,
          mastery: Math.min(100, Math.max(0, mastery)),
          parentId: parentId || undefined,
          position: snapshot.learningItems.length,
        });
      }
      onClose();
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!item) return;
    await deleteLearningItem(item.id);
    onClose();
  }

  return (
    <div className="capture-modal" role="dialog" aria-modal="true" style={{ maxWidth: "540px" }}>
      <div className="capture-modal__header">
        <div>
          <span className="eyebrow"><GraduationCap size={13} /> Learning Path</span>
          <h2>{isEditing ? "Edit Learning Item" : "New Learning Topic"}</h2>
        </div>
        <button type="button" className="icon-button" onClick={onClose} aria-label="Close">
          <X size={18} />
        </button>
      </div>

      <form onSubmit={handleSave} style={{ display: "flex", flexDirection: "column", gap: "14px", marginTop: "14px" }}>
        <div>
          <label className="sr-only" htmlFor="learning-title">Title</label>
          <input
            id="learning-title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Topic or concept title..."
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
            placeholder="Summary, resources, or curriculum notes..."
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

        {/* Type & Status */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
          <div>
            <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "4px" }}>
              Level / Type
            </label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value as LearningItem["type"])}
              style={{
                width: "100%",
                padding: "8px 10px",
                background: "var(--surface-color, #151a26)",
                border: "1px solid var(--border-color, #232b3e)",
                borderRadius: "8px",
                color: "#fff",
                fontSize: "0.88rem",
              }}
            >
              <option value="domain">Domain (High level)</option>
              <option value="subject">Subject</option>
              <option value="topic">Topic</option>
              <option value="concept">Concept (Fine grained)</option>
            </select>
          </div>

          <div>
            <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "4px" }}>
              Status
            </label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as LearningItem["status"])}
              style={{
                width: "100%",
                padding: "8px 10px",
                background: "var(--surface-color, #151a26)",
                border: "1px solid var(--border-color, #232b3e)",
                borderRadius: "8px",
                color: "#fff",
                fontSize: "0.88rem",
              }}
            >
              <option value="not_started">Not Started</option>
              <option value="learning">In Progress</option>
              <option value="reviewing">Reviewing</option>
              <option value="mastered">Mastered</option>
            </select>
          </div>
        </div>

        {/* Parent Domain/Subject selector */}
        <div>
          <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "4px" }}>
            Parent Area / Domain (optional)
          </label>
          <select
            value={parentId}
            onChange={(e) => setParentId(e.target.value)}
            style={{
              width: "100%",
              padding: "8px 10px",
              background: "var(--surface-color, #151a26)",
              border: "1px solid var(--border-color, #232b3e)",
              borderRadius: "8px",
              color: "#fff",
              fontSize: "0.88rem",
            }}
          >
            <option value="">None (Top-level)</option>
            {potentialParents.map((parent) => (
              <option key={parent.id} value={parent.id}>
                {parent.title} ({parent.type})
              </option>
            ))}
          </select>
        </div>

        {/* Mastery Slider & Direct Input */}
        <div style={{ background: "rgba(255, 255, 255, 0.03)", padding: "12px 14px", borderRadius: "8px", border: "1px solid rgba(255, 255, 255, 0.05)" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
            <label style={{ fontSize: "0.84rem", fontWeight: 600, color: "#cbd5e1", display: "flex", alignItems: "center", gap: "6px" }}>
              <BookOpen size={14} color="#a78bfa" /> Mastery Progress
            </label>
            <span style={{ fontSize: "0.92rem", fontWeight: 700, color: "#a78bfa" }}>
              {mastery}%
            </span>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
            <input
              type="range"
              min={0}
              max={100}
              step={5}
              value={mastery}
              onChange={(e) => setMastery(Number(e.target.value))}
              style={{ flex: 1, accentColor: "#a78bfa" }}
            />
            <input
              type="number"
              min={0}
              max={100}
              value={mastery}
              onChange={(e) => setMastery(Math.min(100, Math.max(0, Number(e.target.value))))}
              style={{
                width: "60px",
                padding: "4px 6px",
                background: "var(--surface-color, #151a26)",
                border: "1px solid var(--border-color, #232b3e)",
                borderRadius: "6px",
                color: "#fff",
                fontSize: "0.85rem",
                textAlign: "center",
              }}
            />
          </div>
        </div>

        {/* Footer actions */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginTop: "10px",
            paddingTop: "12px",
            borderTop: "1px solid rgba(255, 255, 255, 0.06)",
          }}
        >
          {isEditing && (
            !confirmDelete ? (
              <button
                type="button"
                className="text-button"
                style={{ color: "#ef4444", display: "flex", alignItems: "center", gap: "6px", padding: 0 }}
                onClick={() => setConfirmDelete(true)}
              >
                <Trash2 size={14} /> Delete
              </button>
            ) : (
              <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                <button type="button" className="danger-button" style={{ padding: "4px 8px", fontSize: "0.8rem" }} onClick={handleDelete}>
                  Confirm Delete
                </button>
                <button type="button" className="text-button" onClick={() => setConfirmDelete(false)}>
                  Cancel
                </button>
              </div>
            )
          )}
          {!isEditing && <div />}

          <div style={{ display: "flex", gap: "8px" }}>
            <button type="button" className="text-button" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="primary-button" disabled={!title.trim() || saving}>
              {saving ? "Saving…" : isEditing ? "Save Changes" : "Create Topic"}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}

export function LearningItemModal({
  item,
  isOpen,
  onClose,
  defaultParentId,
}: LearningItemModalProps) {
  if (!isOpen) return null;
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <LearningItemForm
        key={item ? item.id : "new-item"}
        item={item}
        onClose={onClose}
        defaultParentId={defaultParentId}
      />
    </div>
  );
}

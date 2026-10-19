"use client";

import { useEffect, useState, type FormEvent } from "react";
import { BookOpen, Eye, Edit3, Link2, Plus, Tag, Trash2, X } from "lucide-react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import type { Note } from "@/types/entities";

interface NoteEditorModalProps {
  note: Note | null;
  isOpen: boolean;
  onClose: () => void;
}

function NoteEditorForm({ note, onClose }: { note: Note; onClose: () => void }) {
  const { snapshot, updateNote, deleteNote, addRelation, deleteRelation } = usePersonalOS();
  const [title, setTitle] = useState(note.title);
  const [content, setContent] = useState(note.content || "");
  const [tagInput, setTagInput] = useState("");
  const [tags, setTags] = useState<string[]>(note.tags || []);
  const [mode, setMode] = useState<"edit" | "preview">("edit");
  const [selectedProjectId, setSelectedProjectId] = useState<string>("");
  const [saving, setSaving] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [onClose]);

  const noteRelations = snapshot.relations.filter(
    (r) =>
      (r.sourceEntityType === "note" && r.sourceEntityId === note.id) ||
      (r.targetEntityType === "note" && r.targetEntityId === note.id),
  );

  const linkedProjectIds = new Set(
    noteRelations
      .map((r) => (r.sourceEntityType === "project" ? r.sourceEntityId : r.targetEntityType === "project" ? r.targetEntityId : null))
      .filter(Boolean),
  );

  const availableProjects = snapshot.projects.filter((p) => !linkedProjectIds.has(p.id));

  function handleAddTag() {
    const trimmed = tagInput.trim().replace(/^#/, "");
    if (trimmed && !tags.includes(trimmed)) {
      setTags([...tags, trimmed]);
      setTagInput("");
    }
  }

  function handleRemoveTag(tagToRemove: string) {
    setTags(tags.filter((t) => t !== tagToRemove));
  }

  async function handleLinkProject() {
    if (!selectedProjectId) return;
    await addRelation({
      sourceEntityType: "note",
      sourceEntityId: note.id,
      targetEntityType: "project",
      targetEntityId: selectedProjectId,
      relationshipType: "supports",
    });
    setSelectedProjectId("");
  }

  async function handleUnlink(relationId: string) {
    await deleteRelation(relationId);
  }

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    setSaving(true);
    try {
      await updateNote({
        ...note,
        title: title.trim(),
        content,
        tags,
        linkedEntityCount: noteRelations.length,
      });
      onClose();
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    await deleteNote(note.id);
    onClose();
  }

  return (
    <div className="capture-modal note-editor-modal" role="dialog" aria-modal="true" style={{ maxWidth: "680px" }}>
      <div className="capture-modal__header">
        <div>
          <span className="eyebrow"><BookOpen size={13} /> Knowledge Editor</span>
          <h2 id="note-modal-title">Edit Note</h2>
        </div>
        <button type="button" className="icon-button" onClick={onClose} aria-label="Close">
          <X size={18} />
        </button>
      </div>

      <form onSubmit={handleSave} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
        <div>
          <label className="sr-only" htmlFor="note-title-input">Title</label>
          <input
            id="note-title-input"
            type="text"
            className="text-input"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Note title..."
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

        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div className="capture-types" style={{ marginBottom: 0 }}>
            <button
              type="button"
              data-active={mode === "edit"}
              onClick={() => setMode("edit")}
            >
              <Edit3 size={14} /> Write
            </button>
            <button
              type="button"
              data-active={mode === "preview"}
              onClick={() => setMode("preview")}
            >
              <Eye size={14} /> Preview
            </button>
          </div>
          <small style={{ color: "var(--muted-color, #8b9bb4)" }}>Supports Markdown</small>
        </div>

        {mode === "edit" ? (
          <textarea
            rows={8}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Write your thoughts, ideas, or markdown notes here..."
            style={{
              width: "100%",
              padding: "12px 14px",
              background: "var(--surface-color, #151a26)",
              border: "1px solid var(--border-color, #232b3e)",
              borderRadius: "8px",
              color: "#e2e8f0",
              fontSize: "0.92rem",
              fontFamily: "inherit",
              resize: "vertical",
              minHeight: "140px",
            }}
          />
        ) : (
          <div
            style={{
              minHeight: "140px",
              padding: "14px",
              background: "rgba(21, 26, 38, 0.6)",
              border: "1px solid var(--border-color, #232b3e)",
              borderRadius: "8px",
              color: "#e2e8f0",
              lineHeight: 1.6,
              fontSize: "0.92rem",
              whiteSpace: "pre-wrap",
            }}
          >
            {content.trim() ? content : <em style={{ color: "#64748b" }}>Empty note preview.</em>}
          </div>
        )}

        {/* Tags Section */}
        <div>
          <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "6px" }}>
            <Tag size={12} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} />
            Tags
          </label>
          <div style={{ display: "flex", gap: "8px", marginBottom: "8px" }}>
            <input
              type="text"
              value={tagInput}
              onChange={(e) => setTagInput(e.target.value)}
              onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); handleAddTag(); } }}
              placeholder="Add tag (press Enter)..."
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
            <button type="button" className="text-button" onClick={handleAddTag}>Add</button>
          </div>
          <div className="tag-list">
            {tags.map((tag) => (
              <span key={tag} style={{ cursor: "pointer", display: "inline-flex", alignItems: "center", gap: "4px" }} onClick={() => handleRemoveTag(tag)}>
                #{tag} <X size={11} />
              </span>
            ))}
          </div>
        </div>

        {/* Linked Projects/Entities */}
        <div style={{ borderTop: "1px solid var(--border-color, #232b3e)", paddingTop: "10px" }}>
          <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "6px" }}>
            <Link2 size={12} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} />
            Linked Projects ({noteRelations.length})
          </label>
          {noteRelations.length > 0 && (
            <div style={{ display: "flex", flexWrap: "wrap", gap: "6px", marginBottom: "8px" }}>
              {noteRelations.map((r) => {
                const otherId = r.sourceEntityType === "note" ? r.targetEntityId : r.sourceEntityId;
                const proj = snapshot.projects.find((p) => p.id === otherId);
                return (
                  <span
                    key={r.id}
                    style={{
                      background: "rgba(59, 130, 246, 0.15)",
                      border: "1px solid rgba(59, 130, 246, 0.3)",
                      padding: "3px 8px",
                      borderRadius: "4px",
                      fontSize: "0.8rem",
                      color: "#93c5fd",
                      display: "inline-flex",
                      alignItems: "center",
                      gap: "6px",
                    }}
                  >
                    {proj?.title || otherId}
                    <button type="button" onClick={() => handleUnlink(r.id)} style={{ background: "none", border: "none", color: "inherit", cursor: "pointer", padding: 0 }}>
                      <X size={12} />
                    </button>
                  </span>
                );
              })}
            </div>
          )}
          {availableProjects.length > 0 && (
            <div style={{ display: "flex", gap: "8px" }}>
              <select
                value={selectedProjectId}
                onChange={(e) => setSelectedProjectId(e.target.value)}
                style={{
                  flex: 1,
                  padding: "6px 10px",
                  background: "var(--surface-color, #151a26)",
                  border: "1px solid var(--border-color, #232b3e)",
                  borderRadius: "6px",
                  color: "#fff",
                  fontSize: "0.85rem",
                }}
              >
                <option value="">Link to project...</option>
                {availableProjects.map((p) => (
                  <option key={p.id} value={p.id}>{p.title}</option>
                ))}
              </select>
              <button type="button" className="text-button" disabled={!selectedProjectId} onClick={handleLinkProject}>
                <Plus size={13} /> Link
              </button>
            </div>
          )}
        </div>

        <div className="capture-modal__footer" style={{ marginTop: "12px", borderTop: "1px solid var(--border-color, #232b3e)", paddingTop: "12px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          {!confirmDelete ? (
            <button
              type="button"
              className="text-button"
              style={{ color: "#ef4444" }}
              onClick={() => setConfirmDelete(true)}
            >
              <Trash2 size={14} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} /> Delete Note
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

export function NoteEditorModal({ note, isOpen, onClose }: NoteEditorModalProps) {
  if (!isOpen || !note) return null;
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <NoteEditorForm key={note.id} note={note} onClose={onClose} />
    </div>
  );
}

"use client";

import { BookOpen, Braces, Link2, Network, NotebookPen, Plus, Search } from "lucide-react";
import { useMemo, useState } from "react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import { LoadingScreen } from "@/components/ui/LoadingScreen";
import { NoteEditorModal } from "@/components/modals/NoteEditorModal";
import { LearningItemModal } from "@/components/modals/LearningItemModal";
import type { LearningItem, Note } from "@/types/entities";

export function KnowledgeWorkspace() {
  const { snapshot, isReady, openCapture } = usePersonalOS();
  const [query, setQuery] = useState("");
  const [selectedNote, setSelectedNote] = useState<Note | null>(null);
  const [selectedLearningItem, setSelectedLearningItem] = useState<LearningItem | null>(null);
  const [learningModalOpen, setLearningModalOpen] = useState(false);

  const notes = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return snapshot.notes;
    return snapshot.notes.filter((note) =>
      [note.title, note.content, ...note.tags].some((value) => value.toLowerCase().includes(needle)),
    );
  }, [query, snapshot.notes]);

  if (!isReady) return <LoadingScreen />;

  return (
    <div className="page-shell">
      <header className="page-header">
        <div><span className="eyebrow"><BookOpen size={14} /> Knowledge</span><h1>Turn information into context.</h1><p>Notes become useful when they connect to the work and ideas around them.</p></div>
        <button type="button" className="primary-button page-header__action" onClick={() => openCapture("note")}><Plus size={17} /> New note</button>
      </header>

      <div className="search-box">
        <Search size={19} />
        <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search notes, ideas, and tags…" aria-label="Search knowledge" />
        <kbd>/</kbd>
      </div>

      <div className="knowledge-grid">
        <section>
          <div className="section-heading"><div><span className="eyebrow">Library</span><h2>Recent notes</h2></div><span className="muted-label">{notes.length} notes (click to edit)</span></div>
          <div className="note-grid">
            {notes.map((note, index) => (
              <article
                className="note-card"
                key={note.id}
                onClick={() => setSelectedNote(note)}
                role="button"
                tabIndex={0}
                style={{ cursor: "pointer", transition: "transform 0.15s ease, border-color 0.15s ease" }}
                onKeyDown={(e) => { if (e.key === "Enter") setSelectedNote(note); }}
              >
                <div className="note-card__icon">{index % 2 === 0 ? <NotebookPen size={18} /> : <Braces size={18} />}</div>
                <div className="note-card__meta"><span>{note.tags[0] || "note"}</span><span><Link2 size={12} /> {note.linkedEntityCount || 0}</span></div>
                <h3>{note.title}</h3>
                <p>{note.content || "Click to add markdown content..."}</p>
                <div className="tag-list">{note.tags.map((tag) => <span key={tag}>#{tag}</span>)}</div>
              </article>
            ))}
            {notes.length === 0 && <div className="empty-state"><Search size={22} /><strong>No matching notes</strong><span>Try a broader word or tag.</span></div>}
          </div>
        </section>

        <aside className="knowledge-rail">
          <section className="panel">
            <div className="section-heading">
              <div>
                <span className="eyebrow">Learning path</span>
                <h2>In progress</h2>
              </div>
              <button
                type="button"
                className="text-button"
                onClick={() => {
                  setSelectedLearningItem(null);
                  setLearningModalOpen(true);
                }}
                style={{ fontSize: "0.8rem", display: "inline-flex", alignItems: "center", gap: "4px" }}
              >
                <Plus size={13} /> Add topic
              </button>
            </div>
            <div className="learning-list">
              {snapshot.learningItems.filter((item) => item.type !== "domain").map((item) => (
                <div
                  className="learning-row"
                  key={item.id}
                  onClick={() => {
                    setSelectedLearningItem(item);
                    setLearningModalOpen(true);
                  }}
                  role="button"
                  tabIndex={0}
                  style={{ cursor: "pointer", transition: "transform 0.1s ease" }}
                  title="Click to edit topic or update mastery"
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      setSelectedLearningItem(item);
                      setLearningModalOpen(true);
                    }
                  }}
                >
                  <div><span>{item.type}</span><strong>{item.title}</strong></div>
                  <div className="learning-row__progress"><span style={{ width: `${item.mastery}%` }} /></div>
                  <small>{item.mastery}%</small>
                </div>
              ))}
              {snapshot.learningItems.filter((item) => item.type !== "domain").length === 0 && (
                <div className="empty-state" style={{ padding: "16px" }}>
                  <strong>No learning topics</strong>
                  <span>Click &ldquo;Add topic&rdquo; to build your curriculum.</span>
                </div>
              )}
            </div>
          </section>
          <section className="connection-card">
            <span><Network size={18} /></span>
            <div>
              <small>Knowledge graph</small>
              <strong>{snapshot.relations.length} active connections</strong>
              <p>Notes, projects, goals, and tasks share one context layer.</p>
            </div>
            {snapshot.relations.length > 0 && (
              <div style={{ marginTop: "10px", display: "flex", flexDirection: "column", gap: "6px" }}>
                {snapshot.relations.slice(0, 4).map((r) => (
                  <div key={r.id} style={{ fontSize: "0.78rem", color: "#93c5fd", background: "rgba(59, 130, 246, 0.1)", padding: "4px 8px", borderRadius: "4px" }}>
                    {r.sourceEntityType} → {r.relationshipType} → {r.targetEntityType}
                  </div>
                ))}
              </div>
            )}
          </section>
        </aside>
      </div>

      <NoteEditorModal
        note={selectedNote}
        isOpen={Boolean(selectedNote)}
        onClose={() => setSelectedNote(null)}
      />

      <LearningItemModal
        item={selectedLearningItem}
        isOpen={learningModalOpen}
        onClose={() => setLearningModalOpen(false)}
      />
    </div>
  );
}

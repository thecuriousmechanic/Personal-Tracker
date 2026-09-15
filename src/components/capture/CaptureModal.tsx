"use client";

import { useEffect, useId, useState, type FormEvent, type MouseEvent } from "react";
import { CheckSquare2, Flag, FolderKanban, Goal, NotebookPen, Sparkles, X } from "lucide-react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import type { CaptureType, Task } from "@/types/entities";

const captureTypes: Array<{ type: CaptureType; label: string; icon: typeof CheckSquare2 }> = [
  { type: "task", label: "Task", icon: CheckSquare2 },
  { type: "note", label: "Note", icon: NotebookPen },
  { type: "project", label: "Project", icon: FolderKanban },
  { type: "goal", label: "Goal", icon: Goal },
  { type: "habit", label: "Habit", icon: Sparkles },
];

const placeholders: Record<CaptureType, string> = {
  task: "What needs to move forward?",
  note: "What do you want to remember?",
  project: "What outcome are you building toward?",
  goal: "What change do you want to create?",
  habit: "What would you like to repeat?",
};

export function CaptureModal() {
  const { captureOpen, captureType, closeCapture, createFromCapture } = usePersonalOS();
  const [type, setType] = useState<CaptureType>(captureType);
  const [title, setTitle] = useState("");
  const [destination, setDestination] = useState<"inbox" | "today" | "tomorrow">("inbox");
  const [priority, setPriority] = useState<Task["priority"]>("none");
  const [saving, setSaving] = useState(false);
  const titleId = useId();

  useEffect(() => {
    if (!captureOpen) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") closeCapture();
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [captureOpen, closeCapture]);

  if (!captureOpen) return null;
  const selectedType = type;

  function handleDismiss() {
    setTitle("");
    setType("task");
    closeCapture();
  }

  function handleBackdrop(event: MouseEvent<HTMLDivElement>) {
    if (event.target === event.currentTarget) handleDismiss();
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmed = title.trim();
    if (!trimmed) return;
    setSaving(true);
    try {
      await createFromCapture({ type: selectedType, title: trimmed, destination, priority });
      setTitle("");
      setType("task");
      setDestination("inbox");
      setPriority("none");
    } catch {
      // The provider surfaces persistence errors in the global error banner.
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={handleBackdrop}>
      <section className="capture-modal" role="dialog" aria-modal="true" aria-labelledby="capture-title">
        <div className="capture-modal__header">
          <div>
            <span className="eyebrow">Universal capture</span>
            <h2 id="capture-title">Clear your mind</h2>
          </div>
          <button type="button" className="icon-button" onClick={handleDismiss} aria-label="Close capture">
            <X size={19} />
          </button>
        </div>

        <div className="capture-types" aria-label="Capture type">
          {captureTypes.map((item) => {
            const Icon = item.icon;
            return (
              <button
                key={item.type}
                type="button"
                data-active={selectedType === item.type}
                onClick={() => setType(item.type)}
              >
                <Icon size={16} />
                {item.label}
              </button>
            );
          })}
        </div>

        <form onSubmit={handleSubmit}>
          <label className="sr-only" htmlFor={titleId}>Title</label>
          <textarea
            id={titleId}
            autoFocus
            rows={3}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder={placeholders[selectedType]}
          />

          {selectedType === "task" && (
            <div className="capture-options">
              <label>
                <span>Send to</span>
                <select value={destination} onChange={(event) => setDestination(event.target.value as typeof destination)}>
                  <option value="inbox">Inbox</option>
                  <option value="today">Today</option>
                  <option value="tomorrow">Tomorrow</option>
                </select>
              </label>
              <label>
                <span><Flag size={13} /> Priority</span>
                <select value={priority} onChange={(event) => setPriority(event.target.value as Task["priority"])}>
                  <option value="none">None</option>
                  <option value="low">Low</option>
                  <option value="medium">Medium</option>
                  <option value="high">High</option>
                  <option value="urgent">Urgent</option>
                </select>
              </label>
            </div>
          )}

          <div className="capture-modal__footer">
            <p>Stored on this device first.</p>
            <button type="submit" className="primary-button" disabled={!title.trim() || saving}>
              {saving ? "Saving…" : `Create ${selectedType}`}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

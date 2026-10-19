"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Check, Flame, Repeat, Trash2, X } from "lucide-react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import type { Habit, LocalDate } from "@/types/entities";
import { addDays, toLocalDate } from "@/utils/date";

interface HabitDetailModalProps {
  habit: Habit | null;
  isOpen: boolean;
  onClose: () => void;
}

const PRESET_COLORS = [
  "#6d8cff",
  "#a78bfa",
  "#34d399",
  "#f5b85b",
  "#ec4899",
  "#06b6d4",
  "#f97316",
  "#10b981",
];

function HabitDetailForm({ habit, onClose }: { habit: Habit; onClose: () => void }) {
  const { updateHabit, deleteHabit } = usePersonalOS();
  const [title, setTitle] = useState(habit.title);
  const [description, setDescription] = useState(habit.description || "");
  const [frequencyType, setFrequencyType] = useState<Habit["frequencyType"]>(habit.frequencyType || "daily");
  const [targetCount, setTargetCount] = useState(habit.targetCount || 1);
  const [active, setActive] = useState(habit.active ?? true);
  const [color, setColor] = useState(habit.color || "#6d8cff");
  const [completedDates, setCompletedDates] = useState<LocalDate[]>(habit.completedDates || []);
  const [streak, setStreak] = useState(habit.streak || 0);
  const [saving, setSaving] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [onClose]);

  // Generate last 7 days for quick check/uncheck
  const pastDays = Array.from({ length: 7 }, (_, i) => {
    const date = addDays(new Date(), -i);
    return {
      dateKey: toLocalDate(date),
      label: i === 0 ? "Today" : i === 1 ? "Yest." : date.toLocaleDateString("en-US", { weekday: "narrow" }),
    };
  }).reverse();

  function toggleDate(dateKey: LocalDate) {
    if (completedDates.includes(dateKey)) {
      setCompletedDates(completedDates.filter((d) => d !== dateKey));
      setStreak((prev) => Math.max(0, prev - 1));
    } else {
      setCompletedDates([...completedDates, dateKey]);
      setStreak((prev) => prev + 1);
    }
  }

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    setSaving(true);
    try {
      await updateHabit({
        ...habit,
        title: title.trim(),
        description: description.trim() || undefined,
        frequencyType,
        targetCount: Math.max(1, Number(targetCount)),
        active,
        color,
        completedDates,
        streak,
      });
      onClose();
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    await deleteHabit(habit.id);
    onClose();
  }

  return (
    <div className="capture-modal" role="dialog" aria-modal="true" style={{ maxWidth: "540px" }}>
      <div className="capture-modal__header">
        <div>
          <span className="eyebrow"><Repeat size={13} /> Routine / Habit</span>
          <h2>Edit Habit</h2>
        </div>
        <button type="button" className="icon-button" onClick={onClose} aria-label="Close">
          <X size={18} />
        </button>
      </div>

      <form onSubmit={handleSave} style={{ display: "flex", flexDirection: "column", gap: "14px", marginTop: "14px" }}>
        <div>
          <label className="sr-only" htmlFor="habit-title">Habit Title</label>
          <input
            id="habit-title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Habit title..."
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
            placeholder="Why does this habit matter? (optional notes)"
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

        {/* Frequency & Target */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
          <div>
            <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "4px" }}>
              Frequency
            </label>
            <select
              value={frequencyType}
              onChange={(e) => setFrequencyType(e.target.value as Habit["frequencyType"])}
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
              <option value="daily">Daily</option>
              <option value="weekly">Weekly</option>
              <option value="custom">Custom</option>
            </select>
          </div>

          <div>
            <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "4px" }}>
              Target times ({frequencyType})
            </label>
            <input
              type="number"
              min={1}
              max={30}
              value={targetCount}
              onChange={(e) => setTargetCount(Math.max(1, parseInt(e.target.value, 10) || 1))}
              style={{
                width: "100%",
                padding: "8px 10px",
                background: "var(--surface-color, #151a26)",
                border: "1px solid var(--border-color, #232b3e)",
                borderRadius: "8px",
                color: "#fff",
                fontSize: "0.88rem",
              }}
            />
          </div>
        </div>

        {/* Color picker */}
        <div>
          <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "6px" }}>
            Habit Accent Color
          </label>
          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
            {PRESET_COLORS.map((c) => (
              <button
                type="button"
                key={c}
                onClick={() => setColor(c)}
                style={{
                  width: "26px",
                  height: "26px",
                  borderRadius: "50%",
                  background: c,
                  border: color === c ? "2px solid #fff" : "2px solid transparent",
                  cursor: "pointer",
                  boxShadow: color === c ? `0 0 8px ${c}` : "none",
                  transition: "transform 0.1s ease",
                  transform: color === c ? "scale(1.15)" : "scale(1)",
                }}
                aria-label={`Color ${c}`}
              />
            ))}
          </div>
        </div>

        {/* Active Toggle & Streak stats */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            background: "rgba(255, 255, 255, 0.03)",
            padding: "10px 14px",
            borderRadius: "8px",
            border: "1px solid rgba(255, 255, 255, 0.05)",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <span style={{ color: "#f5b85b", display: "flex", alignItems: "center", gap: "4px", fontWeight: 600 }}>
              <Flame size={16} /> {streak} day streak
            </span>
            <span style={{ fontSize: "0.82rem", color: "#8b9bb4" }}>
              • {completedDates.length} total completions
            </span>
          </div>

          <label style={{ display: "flex", alignItems: "center", gap: "6px", cursor: "pointer", fontSize: "0.84rem", color: "#cbd5e1" }}>
            <input
              type="checkbox"
              checked={active}
              onChange={(e) => setActive(e.target.checked)}
              style={{ accentColor: color }}
            />
            Active
          </label>
        </div>

        {/* Recent 7-Day History quick toggle */}
        <div>
          <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "6px" }}>
            Recent 7-Day History (click to toggle date)
          </label>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(7, 1fr)", gap: "6px" }}>
            {pastDays.map(({ dateKey, label }) => {
              const isDone = completedDates.includes(dateKey);
              return (
                <button
                  type="button"
                  key={dateKey}
                  onClick={() => toggleDate(dateKey)}
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    gap: "4px",
                    padding: "8px 4px",
                    background: isDone ? `${color}25` : "rgba(255, 255, 255, 0.03)",
                    border: isDone ? `1px solid ${color}80` : "1px solid rgba(255, 255, 255, 0.06)",
                    borderRadius: "6px",
                    cursor: "pointer",
                    color: isDone ? "#fff" : "#94a3b8",
                  }}
                  title={dateKey}
                >
                  <span style={{ fontSize: "0.72rem", fontWeight: 500 }}>{label}</span>
                  <span
                    style={{
                      width: "16px",
                      height: "16px",
                      borderRadius: "50%",
                      background: isDone ? color : "rgba(255, 255, 255, 0.1)",
                      display: "grid",
                      placeItems: "center",
                    }}
                  >
                    {isDone && <Check size={10} color="#fff" strokeWidth={3} />}
                  </span>
                </button>
              );
            })}
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
          {!confirmDelete ? (
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
          )}

          <div style={{ display: "flex", gap: "8px" }}>
            <button type="button" className="text-button" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="primary-button" disabled={!title.trim() || saving}>
              {saving ? "Saving…" : "Save Changes"}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}

export function HabitDetailModal({ habit, isOpen, onClose }: HabitDetailModalProps) {
  if (!isOpen || !habit) return null;
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <HabitDetailForm key={habit.id} habit={habit} onClose={onClose} />
    </div>
  );
}

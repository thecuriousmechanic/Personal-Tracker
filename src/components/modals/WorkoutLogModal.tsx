"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Dumbbell, Flame, HeartPulse, Timer, X } from "lucide-react";
import { usePersonalOS } from "@/app/providers/AppProvider";

interface WorkoutLogModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const routinePresets = [
  { title: "Full Body Functional", duration: 45, volume: 3200, category: "Full Body" },
  { title: "Upper Body Push & Pull", duration: 50, volume: 4800, category: "Upper Body" },
  { title: "Lower Body Strength", duration: 45, volume: 5500, category: "Legs" },
  { title: "Outdoor Run & Endurance", duration: 35, volume: 0, category: "Cardio" },
  { title: "Core & Mobility", duration: 25, volume: 0, category: "Core" },
];

export function WorkoutLogModal({ isOpen, onClose }: WorkoutLogModalProps) {
  const { logWorkout } = usePersonalOS();
  const [title, setTitle] = useState("Full Body Functional");
  const [durationMinutes, setDurationMinutes] = useState(45);
  const [volumeKg, setVolumeKg] = useState(3200);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  function selectPreset(preset: typeof routinePresets[0]) {
    setTitle(preset.title);
    setDurationMinutes(preset.duration);
    setVolumeKg(preset.volume);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!title.trim() || durationMinutes <= 0) return;
    setSaving(true);
    try {
      const now = new Date();
      await logWorkout({
        title: title.trim(),
        startedAt: new Date(now.getTime() - durationMinutes * 60000).toISOString(),
        completedAt: now.toISOString(),
        durationMinutes,
        volumeKg,
      });
      onClose();
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="capture-modal" role="dialog" aria-modal="true" style={{ maxWidth: "540px" }}>
        <div className="capture-modal__header">
          <div>
            <span className="eyebrow"><HeartPulse size={13} /> Health & Training</span>
            <h2>Log Workout</h2>
          </div>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Close">
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          {/* Quick Presets */}
          <div>
            <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "6px" }}>
              Quick Presets
            </label>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "6px" }}>
              {routinePresets.map((preset) => (
                <button
                  key={preset.title}
                  type="button"
                  onClick={() => selectPreset(preset)}
                  style={{
                    padding: "4px 9px",
                    borderRadius: "6px",
                    background: title === preset.title ? "rgba(59, 130, 246, 0.2)" : "rgba(21, 26, 38, 0.6)",
                    border: `1px solid ${title === preset.title ? "var(--primary-color, #3b82f6)" : "var(--border-color, #232b3e)"}`,
                    color: title === preset.title ? "#93c5fd" : "#cbd5e1",
                    fontSize: "0.8rem",
                    cursor: "pointer",
                  }}
                >
                  {preset.title}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="sr-only" htmlFor="workout-title-input">Workout Title</label>
            <input
              id="workout-title-input"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Workout title or session name..."
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

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
            <div>
              <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "4px" }}>
                <Timer size={12} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} />
                Duration (minutes)
              </label>
              <input
                type="number"
                min="1"
                max="360"
                value={durationMinutes}
                onChange={(e) => setDurationMinutes(Number(e.target.value))}
                style={{
                  width: "100%",
                  padding: "8px 10px",
                  background: "var(--surface-color, #151a26)",
                  border: "1px solid var(--border-color, #232b3e)",
                  borderRadius: "6px",
                  color: "#fff",
                  fontSize: "0.88rem",
                }}
              />
            </div>

            <div>
              <label style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)", display: "block", marginBottom: "4px" }}>
                <Dumbbell size={12} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} />
                Total Volume (kg)
              </label>
              <input
                type="number"
                min="0"
                step="50"
                value={volumeKg}
                onChange={(e) => setVolumeKg(Number(e.target.value))}
                placeholder="0 for cardio/aerobic"
                style={{
                  width: "100%",
                  padding: "8px 10px",
                  background: "var(--surface-color, #151a26)",
                  border: "1px solid var(--border-color, #232b3e)",
                  borderRadius: "6px",
                  color: "#fff",
                  fontSize: "0.88rem",
                }}
              />
            </div>
          </div>

          <div className="capture-modal__footer" style={{ marginTop: "12px", borderTop: "1px solid var(--border-color, #232b3e)", paddingTop: "12px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontSize: "0.8rem", color: "var(--muted-color, #8b9bb4)" }}>
              <Flame size={13} style={{ display: "inline", verticalAlign: "middle", color: "#f59e0b", marginRight: "3px" }} />
              Records toward daily energy & consistency
            </span>
            <div style={{ display: "flex", gap: "8px" }}>
              <button type="button" className="text-button" onClick={onClose}>Cancel</button>
              <button type="submit" className="primary-button" disabled={!title.trim() || saving}>
                {saving ? "Saving…" : "Log Workout"}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}

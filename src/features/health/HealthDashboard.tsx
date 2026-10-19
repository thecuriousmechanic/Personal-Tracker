"use client";

import { Activity, Dumbbell, Footprints, HeartPulse, Moon, Plus, Settings2, TrendingDown, TrendingUp } from "lucide-react";
import { useMemo, useState, type CSSProperties } from "react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import { LoadingScreen } from "@/components/ui/LoadingScreen";
import { WorkoutLogModal } from "@/components/modals/WorkoutLogModal";
import { HabitDetailModal } from "@/components/modals/HabitDetailModal";
import type { Habit } from "@/types/entities";
import { toLocalDate } from "@/utils/date";

const metricIcons = [Moon, Activity, TrendingDown];

export function HealthDashboard() {
  const { snapshot, isReady, openCapture, toggleHabit, deleteWorkout } = usePersonalOS();
  const [workoutModalOpen, setWorkoutModalOpen] = useState(false);
  const [selectedHabit, setSelectedHabit] = useState<Habit | null>(null);
  const today = useMemo(() => toLocalDate(new Date()), []);
  if (!isReady) return <LoadingScreen />;
  const healthHabits = snapshot.habits.filter((habit) => ["Move or train", "Read for 20 minutes"].includes(habit.title));

  return (
    <div className="page-shell">
      <header className="page-header">
        <div><span className="eyebrow"><HeartPulse size={14} /> Health</span><h1>Build energy that lasts.</h1><p>Training, recovery, and body signals—without turning health into a separate app.</p></div>
        <button type="button" className="primary-button page-header__action" onClick={() => openCapture("habit")}><Plus size={17} /> Add health habit</button>
      </header>

      <div className="metric-grid">
        {snapshot.metrics.filter((metric) => ["recovery", "body", "productivity"].includes(metric.category)).map((metric, index) => {
          const Icon = metricIcons[index % metricIcons.length];
          const delta = metric.previousValue === undefined ? 0 : metric.currentValue - metric.previousValue;
          return (
            <article className="metric-card" key={metric.id} style={{ "--metric-color": metric.color } as CSSProperties}>
              <span className="metric-card__icon"><Icon size={19} /></span>
              <div><small>{metric.name}</small><strong>{metric.currentValue}<em>{metric.unit}</em></strong></div>
              <span className={`metric-delta ${delta >= 0 ? "positive" : "calm"}`}>
                {delta >= 0 ? <TrendingUp size={13} /> : <TrendingDown size={13} />}{Math.abs(delta).toFixed(1)}
              </span>
            </article>
          );
        })}
      </div>

      <div className="health-grid">
        <section className="panel">
          <div className="section-heading">
            <div>
              <span className="eyebrow">Training log</span>
              <h2>Recent movement</h2>
            </div>
            <button
              type="button"
              className="text-button"
              onClick={() => setWorkoutModalOpen(true)}
            >
              <Plus size={14} /> Log workout
            </button>
          </div>
          <div className="workout-list">
            {snapshot.workouts.map((workout) => (
              <article className="workout-row" key={workout.id}>
                <span><Dumbbell size={18} /></span>
                <div>
                  <strong>{workout.title}</strong>
                  <small>{new Intl.DateTimeFormat("en", { month: "short", day: "numeric" }).format(new Date(workout.startedAt))}</small>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                  <div>
                    <strong>{workout.durationMinutes} min</strong>
                    <small>{workout.volumeKg > 0 ? `${workout.volumeKg.toLocaleString()} kg volume` : "Aerobic"}</small>
                  </div>
                  <button
                    type="button"
                    onClick={() => deleteWorkout(workout.id)}
                    title="Delete workout entry"
                    style={{ background: "none", border: "none", color: "#64748b", cursor: "pointer", padding: "4px" }}
                  >
                    ×
                  </button>
                </div>
              </article>
            ))}
            {snapshot.workouts.length === 0 && (
              <div className="empty-state" style={{ padding: "20px" }}>
                <Dumbbell size={22} />
                <strong>No workouts logged</strong>
                <span>Tap &ldquo;Log workout&rdquo; above to record movement.</span>
              </div>
            )}
          </div>
        </section>
        <section className="panel">
          <div className="section-heading"><div><span className="eyebrow">Foundations</span><h2>Health routines</h2></div></div>
          <div className="habit-list">
            {healthHabits.map((habit) => {
              const complete = habit.completedDates.includes(today);
              return (
                <button
                  className="health-habit"
                  type="button"
                  key={habit.id}
                  data-completed={complete}
                  onClick={() => toggleHabit(habit.id)}
                >
                  <span>{habit.title.includes("Move") ? <Footprints size={18} /> : <Moon size={18} />}</span>
                  <div><strong>{habit.title}</strong><small>{habit.streak} day streak</small></div>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <span
                      role="button"
                      tabIndex={0}
                      title="Edit habit details"
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedHabit(habit);
                      }}
                      onKeyDown={(e) => {
                        if (e.key === "Enter") {
                          e.stopPropagation();
                          setSelectedHabit(habit);
                        }
                      }}
                      style={{ padding: "4px", color: "#64748b" }}
                    >
                      <Settings2 size={14} />
                    </span>
                    <i>{complete ? "Done" : "Do today"}</i>
                  </div>
                </button>
              );
            })}
          </div>
        </section>
      </div>

      <WorkoutLogModal
        isOpen={workoutModalOpen}
        onClose={() => setWorkoutModalOpen(false)}
      />

      <HabitDetailModal
        habit={selectedHabit}
        isOpen={Boolean(selectedHabit)}
        onClose={() => setSelectedHabit(null)}
      />
    </div>
  );
}

"use client";

import { Activity, ArrowRight, Dumbbell, Footprints, HeartPulse, Moon, Plus, TrendingDown, TrendingUp } from "lucide-react";
import { useMemo, type CSSProperties } from "react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import { LoadingScreen } from "@/components/ui/LoadingScreen";
import { toLocalDate } from "@/utils/date";

const metricIcons = [Moon, Activity, TrendingDown];

export function HealthDashboard() {
  const { snapshot, isReady, openCapture, toggleHabit } = usePersonalOS();
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
          <div className="section-heading"><div><span className="eyebrow">Training log</span><h2>Recent movement</h2></div><span className="muted-label">Workout logging next <ArrowRight size={12} /></span></div>
          <div className="workout-list">
            {snapshot.workouts.map((workout) => (
              <article className="workout-row" key={workout.id}>
                <span><Dumbbell size={18} /></span>
                <div><strong>{workout.title}</strong><small>{new Intl.DateTimeFormat("en", { month: "short", day: "numeric" }).format(new Date(workout.startedAt))}</small></div>
                <div><strong>{workout.durationMinutes} min</strong><small>{workout.volumeKg > 0 ? `${workout.volumeKg.toLocaleString()} kg volume` : "Aerobic"}</small></div>
              </article>
            ))}
          </div>
        </section>
        <section className="panel">
          <div className="section-heading"><div><span className="eyebrow">Foundations</span><h2>Health routines</h2></div></div>
          <div className="habit-list">
            {healthHabits.map((habit) => {
              const complete = habit.completedDates.includes(today);
              return (
                <button className="health-habit" type="button" key={habit.id} data-completed={complete} onClick={() => toggleHabit(habit.id)}>
                  <span>{habit.title.includes("Move") ? <Footprints size={18} /> : <Moon size={18} />}</span>
                  <div><strong>{habit.title}</strong><small>{habit.streak} day streak</small></div>
                  <i>{complete ? "Done" : "Do today"}</i>
                </button>
              );
            })}
          </div>
        </section>
      </div>
    </div>
  );
}

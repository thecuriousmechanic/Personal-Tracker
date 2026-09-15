"use client";

import { ArrowRight, CalendarDays, Check, Flame, Plus, Sparkles, Target } from "lucide-react";
import Link from "next/link";
import { useMemo, type CSSProperties } from "react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import { LoadingScreen } from "@/components/ui/LoadingScreen";
import { ProgressRing } from "@/components/ui/ProgressRing";
import { TaskRow } from "@/components/ui/TaskRow";
import { getTodayOverview } from "@/services/dashboard.service";

export function TodayDashboard() {
  const { snapshot, isReady, openCapture, toggleTask, toggleHabit } = usePersonalOS();
  const now = useMemo(() => new Date(), []);
  const overview = useMemo(() => getTodayOverview(snapshot, now), [snapshot, now]);

  if (!isReady) return <LoadingScreen />;

  const greeting = now.getHours() < 12 ? "Good morning" : now.getHours() < 18 ? "Good afternoon" : "Good evening";
  const dateLabel = new Intl.DateTimeFormat("en", { weekday: "long", month: "long", day: "numeric" }).format(now);
  const projectFor = (projectId?: string) => snapshot.projects.find((project) => project.id === projectId);
  const completedCount = overview.completedTasks.length + overview.completedHabits.length;
  const totalCount = overview.openTasks.length + overview.completedTasks.length + overview.habits.length;

  return (
    <div className="page-shell today-page">
      <header className="page-header">
        <div>
          <span className="eyebrow"><CalendarDays size={14} /> {dateLabel}</span>
          <h1>{greeting}, Krishna.</h1>
          <p>Here’s the clearest path through today.</p>
        </div>
        <button type="button" className="primary-button page-header__action" onClick={() => openCapture("task")}>
          <Plus size={17} /> Add task
        </button>
      </header>

      <section className="focus-card">
        <div className="focus-card__glow" />
        <div className="focus-card__copy">
          <span className="focus-label"><Sparkles size={14} /> Daily focus</span>
          <h2>{overview.focusCount > 0 ? `${overview.focusCount} important ${overview.focusCount === 1 ? "move" : "moves"} left` : "The important work is clear"}</h2>
          <p>
            {overview.openTasks[0]
              ? <>Start with <strong>{overview.openTasks[0].title}</strong>. Everything else can wait.</>
              : "Your scheduled work is complete. Use the space for recovery or reflection."}
          </p>
          {overview.openTasks[0] && (
            <button type="button" className="focus-action" onClick={() => toggleTask(overview.openTasks[0].id)}>
              Mark focus complete <ArrowRight size={16} />
            </button>
          )}
        </div>
        <div className="focus-card__progress">
          <ProgressRing value={overview.completionRate} size={128} strokeWidth={9} label="complete" />
          <span>{completedCount} of {totalCount} actions</span>
        </div>
      </section>

      <div className="dashboard-grid">
        <section className="panel task-panel">
          <div className="section-heading">
            <div>
              <span className="eyebrow">Execution</span>
              <h2>Today’s tasks</h2>
            </div>
            <Link href="/plan">View plan <ArrowRight size={14} /></Link>
          </div>
          <div className="task-list">
            {overview.openTasks.map((task) => (
              <TaskRow key={task.id} task={task} today={overview.dateKey} project={projectFor(task.projectId)} onToggle={toggleTask} />
            ))}
            {overview.completedTasks.map((task) => (
              <TaskRow key={task.id} task={task} today={overview.dateKey} project={projectFor(task.projectId)} onToggle={toggleTask} compact />
            ))}
            {overview.openTasks.length + overview.completedTasks.length === 0 && (
              <div className="empty-state"><Check size={22} /><strong>Nothing scheduled</strong><span>Capture a task when you’re ready.</span></div>
            )}
          </div>
        </section>

        <section className="panel habit-panel">
          <div className="section-heading">
            <div>
              <span className="eyebrow">Daily rhythm</span>
              <h2>Habits</h2>
            </div>
            <span className="streak-badge"><Flame size={14} /> 8 day best</span>
          </div>
          <div className="habit-list">
            {overview.habits.map((habit) => {
              const completed = habit.completedDates.includes(overview.dateKey);
              return (
                <button
                  type="button"
                  className="habit-row"
                  data-completed={completed}
                  key={habit.id}
                  onClick={() => toggleHabit(habit.id)}
                >
                  <span className="habit-row__color" style={{ background: habit.color }} />
                  <span className="habit-row__copy"><strong>{habit.title}</strong><small>{habit.streak} day streak</small></span>
                  <span className="habit-row__check">{completed && <Check size={14} strokeWidth={3} />}</span>
                </button>
              );
            })}
          </div>
        </section>
      </div>

      <section className="projects-section">
        <div className="section-heading">
          <div><span className="eyebrow">Momentum</span><h2>Active projects</h2></div>
          <Link href="/plan">All projects <ArrowRight size={14} /></Link>
        </div>
        <div className="project-strip">
          {snapshot.projects.filter((project) => project.status === "active").map((project) => {
            const remaining = snapshot.tasks.filter((task) => task.projectId === project.id && task.status !== "completed").length;
            return (
              <article className="project-card" key={project.id} style={{ "--project-color": project.color } as CSSProperties}>
                <div className="project-card__top"><span className="project-icon"><Target size={17} /></span><span>{remaining} open</span></div>
                <h3>{project.title}</h3>
                <div className="progress-line"><span style={{ width: `${project.progress}%` }} /></div>
                <div className="project-card__footer"><span>{project.progress}% complete</span><ArrowRight size={15} /></div>
              </article>
            );
          })}
        </div>
      </section>
    </div>
  );
}

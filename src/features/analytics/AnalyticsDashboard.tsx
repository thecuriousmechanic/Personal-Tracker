"use client";

import { BarChart3, BookOpen, CheckCircle2, Flame, Lightbulb, Target, TrendingUp } from "lucide-react";
import { useMemo } from "react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import { LoadingScreen } from "@/components/ui/LoadingScreen";
import { getHabitConsistency, getTaskCompletionRate } from "@/services/dashboard.service";

export function AnalyticsDashboard() {
  const { snapshot, isReady } = usePersonalOS();
  const now = useMemo(() => new Date(), []);
  if (!isReady) return <LoadingScreen />;
  const completionRate = getTaskCompletionRate(snapshot.tasks);
  const consistency = getHabitConsistency(snapshot.habits, now);
  const learningProgress = snapshot.learningItems.length
    ? Math.round(snapshot.learningItems.reduce((sum, item) => sum + item.mastery, 0) / snapshot.learningItems.length)
    : 0;
  const stats = [
    { label: "Task completion", value: `${completionRate}%`, note: "Across active tasks", icon: CheckCircle2, color: "blue" },
    { label: "Habit consistency", value: `${consistency}%`, note: "Last seven days", icon: Flame, color: "amber" },
    { label: "Learning mastery", value: `${learningProgress}%`, note: "Across current topics", icon: BookOpen, color: "violet" },
  ];

  return (
    <div className="page-shell">
      <header className="page-header"><div><span className="eyebrow"><BarChart3 size={14} /> Analytics</span><h1>See the signal, not just the score.</h1><p>Use patterns across work, learning, and health to decide what to change.</p></div></header>
      <div className="analytics-stats">
        {stats.map((stat) => { const Icon = stat.icon; return <article key={stat.label} data-color={stat.color}><span><Icon size={18} /></span><small>{stat.label}</small><strong>{stat.value}</strong><p>{stat.note}</p></article>; })}
      </div>
      <div className="analytics-grid">
        <section className="panel">
          <div className="section-heading"><div><span className="eyebrow">Portfolio</span><h2>Project momentum</h2></div><span className="streak-badge"><TrendingUp size={14} /> On track</span></div>
          <div className="momentum-list">
            {snapshot.projects.map((project) => (
              <div key={project.id}><div><strong>{project.title}</strong><span>{project.progress}%</span></div><div className="progress-line"><span style={{ width: `${project.progress}%`, background: project.color }} /></div></div>
            ))}
          </div>
        </section>
        <section className="insight-card">
          <span className="insight-card__icon"><Lightbulb size={20} /></span>
          <small>Pattern detected</small>
          <h2>Your best days begin with planning.</h2>
          <p>The “Plan the day” habit has your strongest streak and supports your highest-priority project work.</p>
          <div><Target size={15} /><span>Keep this habit visible in Today</span></div>
        </section>
      </div>
      <section className="panel activity-panel">
        <div className="section-heading"><div><span className="eyebrow">History</span><h2>Recent activity</h2></div></div>
        <div className="activity-list">
          {snapshot.activities.slice(0, 8).map((event) => (
            <div key={event.id}><span className="activity-dot" /><div><strong>{event.label}</strong><small>{new Intl.DateTimeFormat("en", { month: "short", day: "numeric", hour: "numeric", minute: "2-digit" }).format(new Date(event.occurredAt))}</small></div><em>{event.entityType.replace("_", " ")}</em></div>
          ))}
        </div>
      </section>
    </div>
  );
}

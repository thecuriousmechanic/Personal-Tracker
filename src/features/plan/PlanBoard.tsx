"use client";

import { ArrowRight, CalendarClock, Inbox, Layers3, Plus, Target } from "lucide-react";
import { useMemo, useState } from "react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import { LoadingScreen } from "@/components/ui/LoadingScreen";
import { TaskRow } from "@/components/ui/TaskRow";
import { TaskDetailModal } from "@/components/modals/TaskDetailModal";
import { ProjectDetailModal } from "@/components/modals/ProjectDetailModal";
import { GoalDetailModal } from "@/components/modals/GoalDetailModal";
import type { Goal, Project, Task } from "@/types/entities";
import { toLocalDate } from "@/utils/date";

type View = "inbox" | "today" | "upcoming" | "all";

export function PlanBoard() {
  const { snapshot, isReady, openCapture, toggleTask, scheduleTaskForToday } = usePersonalOS();
  const [view, setView] = useState<View>("inbox");
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [selectedProject, setSelectedProject] = useState<Project | null>(null);
  const [selectedGoal, setSelectedGoal] = useState<Goal | null>(null);

  const today = useMemo(() => toLocalDate(new Date()), []);
  const tasks = useMemo(() => snapshot.tasks.filter((task) => {
    if (task.deletedAt || task.status === "cancelled") return false;
    if (view === "inbox") return task.status === "inbox";
    if (view === "today") return task.dueDate === today;
    if (view === "upcoming") return Boolean(task.dueDate && task.dueDate > today && task.status !== "completed");
    return true;
  }), [snapshot.tasks, today, view]);

  if (!isReady) return <LoadingScreen />;
  const projectFor = (projectId?: string) => snapshot.projects.find((project) => project.id === projectId);

  return (
    <div className="page-shell">
      <header className="page-header">
        <div><span className="eyebrow"><Layers3 size={14} /> Plan</span><h1>Make space for what matters.</h1><p>Capture quickly, then give each commitment a home.</p></div>
        <button type="button" className="primary-button page-header__action" onClick={() => openCapture("task")}><Plus size={17} /> Add task</button>
      </header>

      <div className="view-tabs" role="tablist" aria-label="Task views">
        {(["inbox", "today", "upcoming", "all"] as View[]).map((item) => (
          <button key={item} type="button" role="tab" aria-selected={view === item} onClick={() => setView(item)}>
            {item === "inbox" && <Inbox size={15} />}
            {item === "today" && <Target size={15} />}
            {item === "upcoming" && <CalendarClock size={15} />}
            <span>{item[0].toUpperCase() + item.slice(1)}</span>
            {item === "inbox" && <small>{snapshot.tasks.filter((task) => task.status === "inbox").length}</small>}
          </button>
        ))}
      </div>

      <section className="panel plan-list-panel">
        <div className="section-heading">
          <div><span className="eyebrow">{view === "inbox" ? "Unsorted" : "Schedule"}</span><h2>{view[0].toUpperCase() + view.slice(1)}</h2></div>
          <span className="muted-label">{tasks.length} {tasks.length === 1 ? "item" : "items"} (click to edit)</span>
        </div>
        <div className="task-list">
          {tasks.map((task) => (
            <div className="plan-task" key={task.id}>
              <TaskRow
                task={task}
                today={today}
                project={projectFor(task.projectId)}
                onToggle={toggleTask}
                onEdit={(t) => setSelectedTask(t)}
              />
              {view === "inbox" && (
                <button type="button" className="text-button" onClick={() => scheduleTaskForToday(task.id)}>Move to today <ArrowRight size={13} /></button>
              )}
            </div>
          ))}
          {tasks.length === 0 && <div className="empty-state"><Inbox size={22} /><strong>This view is clear</strong><span>New captures will appear here.</span></div>}
        </div>
      </section>

      <div className="plan-grid">
        <section>
          <div className="section-heading"><div><span className="eyebrow">Outcomes</span><h2>Projects</h2></div><button type="button" className="text-button" onClick={() => openCapture("project")}>New project <Plus size={14} /></button></div>
          <div className="stack-list">
            {snapshot.projects.map((project) => (
              <article
                className="compact-card"
                key={project.id}
                onClick={() => setSelectedProject(project)}
                role="button"
                tabIndex={0}
                style={{ cursor: "pointer", transition: "transform 0.15s ease" }}
                onKeyDown={(e) => { if (e.key === "Enter") setSelectedProject(project); }}
              >
                <span className="compact-card__indicator" style={{ background: project.color }} />
                <div><h3>{project.title}</h3><p>{project.description || "Define the next useful outcome."}</p><div className="progress-line"><span style={{ width: `${project.progress}%`, background: project.color }} /></div></div>
                <strong>{project.progress}%</strong>
              </article>
            ))}
          </div>
        </section>
        <section>
          <div className="section-heading"><div><span className="eyebrow">Direction</span><h2>Goals</h2></div><button type="button" className="text-button" onClick={() => openCapture("goal")}>New goal <Plus size={14} /></button></div>
          <div className="stack-list">
            {snapshot.goals.map((goal) => (
              <article
                className="goal-card"
                key={goal.id}
                onClick={() => setSelectedGoal(goal)}
                role="button"
                tabIndex={0}
                style={{ cursor: "pointer", transition: "transform 0.15s ease" }}
                onKeyDown={(e) => { if (e.key === "Enter") setSelectedGoal(goal); }}
              >
                <span><Target size={17} /></span>
                <div><h3>{goal.title}</h3><p>{goal.progress}% progress</p></div>
                <ArrowRight size={15} />
              </article>
            ))}
          </div>
        </section>
      </div>

      <TaskDetailModal
        task={selectedTask}
        isOpen={Boolean(selectedTask)}
        onClose={() => setSelectedTask(null)}
      />

      <ProjectDetailModal
        project={selectedProject}
        isOpen={Boolean(selectedProject)}
        onClose={() => setSelectedProject(null)}
      />

      <GoalDetailModal
        goal={selectedGoal}
        isOpen={Boolean(selectedGoal)}
        onClose={() => setSelectedGoal(null)}
      />
    </div>
  );
}

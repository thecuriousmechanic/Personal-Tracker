"use client";

import { CalendarDays, Check, CornerDownRight, Folder } from "lucide-react";
import type { Project, Task } from "@/types/entities";
import { formatRelativeDate } from "@/utils/date";

interface TaskRowProps {
  task: Task;
  today: string;
  project?: Project;
  onToggle: (id: string) => void;
  onEdit?: (task: Task) => void;
  compact?: boolean;
}

export function TaskRow({ task, today, project, onToggle, onEdit, compact = false }: TaskRowProps) {
  const completed = task.status === "completed";
  return (
    <div className={`task-row ${completed ? "task-row--completed" : ""} ${compact ? "task-row--compact" : ""}`}>
      <button
        type="button"
        className="check-button"
        data-checked={completed}
        aria-label={`${completed ? "Reopen" : "Complete"} ${task.title}`}
        aria-pressed={completed}
        onClick={() => onToggle(task.id)}
      >
        {completed && <Check size={14} strokeWidth={3} />}
      </button>
      <div
        className="task-row__body"
        onClick={() => onEdit?.(task)}
        style={{ cursor: onEdit ? "pointer" : "default" }}
      >
        <div className="task-row__title-line">
          {task.parentTaskId && <CornerDownRight size={13} aria-hidden="true" />}
          <span className="task-row__title">{task.title}</span>
          {(task.priority === "urgent" || task.priority === "high") && (
            <span className={`priority-dot priority-dot--${task.priority}`} title={`${task.priority} priority`} />
          )}
        </div>
        <div className="task-row__meta">
          {project && <span><Folder size={12} />{project.title}</span>}
          {!compact && <span><CalendarDays size={12} />{formatRelativeDate(task.dueDate, today)}</span>}
        </div>
      </div>
    </div>
  );
}

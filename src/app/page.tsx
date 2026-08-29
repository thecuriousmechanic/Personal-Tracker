import React from 'react';
import { executionService } from '@/services/execution.service';

export default async function TodayPage() {
  // In a real app, userId would come from auth
  const userId = 'user-123';
  const overview = await executionService.getTodayOverview(userId);

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h1 className="text-4xl font-bold mb-8 text-primary">Today</h1>

      <section className="mb-12">
        <h2 className="text-xl font-semibold mb-4 text-foreground/80">Priority</h2>
        <div className="space-y-2">
          {overview.priorityTasks.map(task => (
            <div key={task.id} className="p-4 bg-surface rounded-lg border border-primary/20 flex items-center gap-3">
              <input type="checkbox" className="w-5 h-5 accent-primary" />
              <span>{task.title}</span>
            </div>
          ))}
          {overview.priorityTasks.length === 0 && <p className="text-foreground/50">No priority tasks today.</p>}
        </div>
      </section>

      <section className="mb-12">
        <h2 className="text-xl font-semibold mb-4 text-foreground/80">Tasks</h2>
        <div className="space-y-2">
          {overview.todayTasks.map(task => (
            <div key={task.id} className="p-4 bg-surface rounded-lg border border-foreground/10 flex items-center gap-3">
              <input type="checkbox" className="w-5 h-5 accent-primary" />
              <span>{task.title}</span>
            </div>
          ))}
          {overview.todayTasks.length === 0 && <p className="text-foreground/50">No tasks scheduled for today.</p>}
        </div>
      </section>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <section>
          <h2 className="text-xl font-semibold mb-4 text-foreground/80">Habits</h2>
          <div className="space-y-2">
            {overview.activeHabits.map(habit => (
              <div key={habit.id} className="p-4 bg-surface rounded-lg border border-foreground/10 flex items-center justify-between">
                <span>{habit.title}</span>
                <button className="px-3 py-1 bg-primary text-white rounded-md text-sm">Complete</button>
              </div>
            ))}
            {overview.activeHabits.length === 0 && <p className="text-foreground/50">No active habits.</p>}
          </div>
        </section>

        <section>
          <h2 className="text-xl font-semibold mb-4 text-foreground/80">Active Projects</h2>
          <div className="space-y-2">
            {overview.activeProjects.map(project => (
              <div key={project.id} className="p-4 bg-surface rounded-lg border border-foreground/10">
                <span className="font-medium">{project.title}</span>
              </div>
            ))}
            {overview.activeProjects.length === 0 && <p className="text-foreground/50">No active projects.</p>}
          </div>
        </section>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mt-8">
        <section>
          <h2 className="text-xl font-semibold mb-4 text-foreground/80">Current Learning</h2>
          <div className="space-y-2">
            {overview.currentLearning.map(item => (
              <div key={item.id} className="p-4 bg-surface rounded-lg border border-foreground/10 flex items-center justify-between">
                <span>{item.title}</span>
                <span className="text-xs bg-primary/20 text-primary px-2 py-1 rounded">{item.type}</span>
              </div>
            ))}
            {overview.currentLearning.length === 0 && <p className="text-foreground/50">Nothing currently being learned.</p>}
          </div>
        </section>

        <section>
          <h2 className="text-xl font-semibold mb-4 text-foreground/80">Key Metrics</h2>
          <div className="space-y-2">
            {overview.keyMetrics.map(metric => (
              <div key={metric.id} className="p-4 bg-surface rounded-lg border border-foreground/10 flex items-center justify-between">
                <span>{metric.name}</span>
                <span className="text-foreground/60">{metric.unit}</span>
              </div>
            ))}
            {overview.keyMetrics.length === 0 && <p className="text-foreground/50">No metrics tracked.</p>}
          </div>
        </section>
      </div>
    </div>
  );
}

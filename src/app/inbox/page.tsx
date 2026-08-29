import React from 'react';
import { executionService } from '@/services/execution.service';

export default async function InboxPage() {
  const userId = 'user-123';
  const tasks = await executionService.getInboxTasks(userId);

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h1 className="text-4xl font-bold mb-8 text-primary">Inbox</h1>
      <div className="space-y-2">
        {tasks.map(task => (
          <div key={task.id} className="p-4 bg-surface rounded-lg border border-foreground/10 flex items-center gap-3">
            <input type="checkbox" className="w-5 h-5 accent-primary" />
            <span className="flex-1">{task.title}</span>
            <button className="text-sm text-primary/70 hover:text-primary">Organize</button>
          </div>
        ))}
        {tasks.length === 0 && <p className="text-foreground/50">Your inbox is empty!</p>}
      </div>
    </div>
  );
}

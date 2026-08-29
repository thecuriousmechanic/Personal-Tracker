import React from 'react';
import { activityService } from '@/services/activity.service';

export default async function AnalyticsPage() {
  const userId = 'user-123';
  const activities = await activityService.getRecentActivity(userId);

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h1 className="text-4xl font-bold mb-8 text-primary">Analytics</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-12">
        <div className="p-6 bg-surface rounded-lg border border-foreground/10 text-center">
          <div className="text-sm text-foreground/60 mb-2">Task Completion Rate</div>
          <div className="text-3xl font-bold text-primary">84%</div>
          <div className="text-xs text-success mt-2">↑ 5% from last week</div>
        </div>
        <div className="p-6 bg-surface rounded-lg border border-foreground/10 text-center">
          <div className="text-sm text-foreground/60 mb-2">Habit Consistency</div>
          <div className="text-3xl font-bold text-primary">72%</div>
          <div className="text-xs text-warning mt-2">↓ 2% from last week</div>
        </div>
        <div className="p-6 bg-surface rounded-lg border border-foreground/10 text-center">
          <div className="text-sm text-foreground/60 mb-2">Learning Progress</div>
          <div className="text-3xl font-bold text-primary">12h</div>
          <div className="text-xs text-success mt-2">↑ 3h from last week</div>
        </div>
      </div>

      <section>
        <h2 className="text-xl font-semibold mb-4 text-foreground/80">Activity Timeline</h2>
        <div className="space-y-4">
          {activities.map(event => (
            <div key={event.id} className="flex gap-4 p-4 bg-surface rounded-lg border border-foreground/10 items-start">
              <div className="w-2 h-2 rounded-full bg-primary mt-2"></div>
              <div>
                <p className="text-sm">
                  <span className="font-medium text-foreground">{event.action}</span>{' '}
                  <span className="text-foreground/60">{event.entityType}</span>
                </p>
                <p className="text-xs text-foreground/40">{event.occurredAt?.toLocaleString()}</p>
              </div>
            </div>
          ))}
          {activities.length === 0 && <p className="text-foreground/50">No activity recorded yet.</p>}
        </div>
      </section>
    </div>
  );
}

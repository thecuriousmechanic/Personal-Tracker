import React from 'react';
import { healthService } from '@/services/health.service';

export default async function HealthPage() {
  const userId = 'user-123';
  const recentWorkouts = await healthService.getRecentWorkouts(userId);
  const metrics = await healthService.getBodyMetrics(userId);

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h1 className="text-4xl font-bold mb-8 text-primary">Health</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div className="md:col-span-2 space-y-8">
          <section>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold text-foreground/80">Recent Workouts</h2>
              <button className="px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium">Start Workout</button>
            </div>
            <div className="space-y-3">
              {recentWorkouts.map(workout => (
                <div key={workout.id} className="p-4 bg-surface rounded-lg border border-foreground/10 flex justify-between items-center">
                  <div>
                    <h3 className="font-medium">{workout.title}</h3>
                    <p className="text-xs text-foreground/60">{workout.completedAt?.toDateString() || 'In progress'}</p>
                  </div>
                  <span className="text-xs bg-primary/20 text-primary px-2 py-1 rounded">Completed</span>
                </div>
              ))}
              {recentWorkouts.length === 0 && <p className="text-foreground/50">No recent workouts.</p>}
            </div>
          </section>

          <section>
            <h2 className="text-xl font-semibold mb-4 text-foreground/80">Current Routine</h2>
            <div className="p-6 bg-surface rounded-lg border border-foreground/10">
              <div className="space-y-4">
                <div className="flex items-center justify-between p-3 bg-foreground/5 rounded-md">
                  <span>Bench Press</span>
                  <span className="text-sm text-foreground/60">3 sets x 8-12 reps</span>
                </div>
                <div className="flex items-center justify-between p-3 bg-foreground/5 rounded-md">
                  <span>Squats</span>
                  <span className="text-sm text-foreground/60">4 sets x 6-10 reps</span>
                </div>
                <div className="flex items-center justify-between p-3 bg-foreground/5 rounded-md">
                  <span>Overhead Press</span>
                  <span className="text-sm text-foreground/60">3 sets x 8-12 reps</span>
                </div>
              </div>
            </div>
          </section>
        </div>

        <div className="space-y-8">
          <section>
            <h2 className="text-xl font-semibold mb-4 text-foreground/80">Body Metrics</h2>
            <div className="space-y-3">
              {metrics.map(metric => (
                <div key={metric.id} className="p-4 bg-surface rounded-lg border border-foreground/10">
                  <div className="text-sm text-foreground/60">{metric.name}</div>
                  <div className="text-2xl font-bold text-primary">-- {metric.unit}</div>
                </div>
              ))}
              {metrics.length === 0 && (
                <div className="p-4 bg-surface rounded-lg border border-foreground/10 text-center">
                  <p className="text-sm text-foreground/60 mb-3">No metrics tracked</p>
                  <button className="text-xs text-primary hover:underline">Add Metric</button>
                </div>
              )}
            </div>
          </section>

          <section>
            <h2 className="text-xl font-semibold mb-4 text-foreground/80">Nutrition</h2>
            <div className="p-4 bg-surface rounded-lg border border-foreground/10 space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-sm">Calories</span>
                <span className="font-medium">1,800 / 2,500</span>
              </div>
              <div className="w-full bg-foreground/10 h-2 rounded-full overflow-hidden">
                <div className="bg-primary h-full" style={{ width: '72%' }}></div>
              </div>
              <div className="grid grid-cols-3 gap-2 text-center text-xs">
                <div className="p-2 bg-foreground/5 rounded">
                  <div className="text-foreground/60">Prot</div>
                  <div className="font-bold">120g</div>
                </div>
                <div className="p-2 bg-foreground/5 rounded">
                  <div className="text-foreground/60">Carb</div>
                  <div className="font-bold">200g</div>
                </div>
                <div className="p-2 bg-foreground/5 rounded">
                  <div className="text-foreground/60">Fat</div>
                  <div className="font-bold">60g</div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}

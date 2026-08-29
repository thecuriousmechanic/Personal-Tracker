import { workoutRepository } from '@/database/repositories/workout.repository';
import { exerciseRepository } from '@/database/repositories/exercise.repository';
import { workoutSetRepository } from '@/database/repositories/workout_set.repository';
import { metricRepository } from '@/database/repositories/metric.repository';
import { Workout } from '@/entities/models/workout';
import { Exercise } from '@/entities/models/exercise';
import { WorkoutSet } from '@/entities/models/workout_set';
import { Metric } from '@/entities/models/metric';

export class HealthService {
  async getRecentWorkouts(userId: string, limit = 5) {
    const allWorkouts = await workoutRepository.list();
    return allWorkouts
      .filter(w => w.userId === userId)
      .sort((a, b) => (b.completedAt?.getTime() || 0) - (a.completedAt?.getTime() || 0))
      .slice(0, limit);
  }

  async createWorkout(userId: string, title: string) {
    return await workoutRepository.create({
      userId,
      title,
      startedAt: new Date(),
    });
  }

  async logSet(workoutId: string, exerciseId: string, setNumber: number, weight: number, reps: number) {
    return await workoutSetRepository.create({
      workoutId,
      exerciseId,
      setNumber,
      weight,
      reps,
      completed: true,
    });
  }

  async getBodyMetrics(userId: string) {
    const allMetrics = await metricRepository.list();
    return allMetrics.filter(m => m.userId === userId);
  }
}

export const healthService = new HealthService();

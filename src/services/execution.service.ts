import { taskRepository } from '@/database/repositories/task.repository';
import { habitRepository } from '@/database/repositories/habit.repository';
import { projectRepository } from '@/database/repositories/project.repository';
import { metricRepository } from '@/database/repositories/metric.repository';
import { learningItemRepository } from '@/database/repositories/learning_item.repository';
import { Task } from '@/entities/models/task';
import { Habit } from '@/entities/models/habit';
import { Project } from '@/entities/models/project';
import { Metric } from '@/entities/models/metric';
import { LearningItem } from '@/entities/models/learning_item';

export interface TodayOverview {
  priorityTasks: Task[];
  todayTasks: Task[];
  activeHabits: Habit[];
  activeProjects: Project[];
  keyMetrics: Metric[];
  currentLearning: LearningItem[];
}

export class ExecutionService {
  async getTodayOverview(userId: string): Promise<TodayOverview> {
    const allTasks = await taskRepository.list();
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const priorityTasks = allTasks.filter(t =>
      t.userId === userId &&
      t.status !== 'completed' &&
      (t.priority === 'urgent' || t.priority === 'high')
    );

    const todayTasks = allTasks.filter(t =>
      t.userId === userId &&
      t.status !== 'completed' &&
      t.dueDate && new Date(t.dueDate).toDateString() === today.toDateString()
    );

    const allHabits = await habitRepository.list();
    const activeHabits = allHabits.filter(h => h.userId === userId && h.active);

    const allProjects = await projectRepository.list();
    const activeProjects = allProjects.filter(p => p.userId === userId && p.status === 'active');

    const allMetrics = await metricRepository.list();
    const keyMetrics = allMetrics.filter(m => m.userId === userId).slice(0, 5);

    const allLearning = await learningItemRepository.list();
    const currentLearning = allLearning.filter(l => l.userId === userId && l.status === 'learning');

    return {
      priorityTasks,
      todayTasks,
      activeHabits,
      activeProjects,
      keyMetrics,
      currentLearning,
    };
  }

  async getInboxTasks(userId: string): Promise<Task[]> {
    const allTasks = await taskRepository.list();
    return allTasks.filter(t => t.userId === userId && t.status === 'inbox');
  }
}

export const executionService = new ExecutionService();

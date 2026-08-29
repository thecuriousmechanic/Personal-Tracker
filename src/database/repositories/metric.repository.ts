import { database } from '../index';
import { WatermelonRepository } from './watermelon.repository';
import { Metric } from '@/entities/models/metric';
import { IBaseRepository } from './base.repository';

export class MetricRepository extends WatermelonRepository<Metric> implements IBaseRepository<Metric> {
  constructor() {
    super(database, Metric);
  }
}

export const metricRepository = new MetricRepository();

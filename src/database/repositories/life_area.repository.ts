import { database } from '../index';
import { WatermelonRepository } from './watermelon.repository';
import { LifeArea } from '@/entities/models/life_area';
import { IBaseRepository } from './base.repository';

export class LifeAreaRepository extends WatermelonRepository<LifeArea> implements IBaseRepository<LifeArea> {
  constructor() {
    super(database, LifeArea);
  }
}

export const lifeAreaRepository = new LifeAreaRepository();

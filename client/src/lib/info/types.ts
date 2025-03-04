import { z } from 'zod';
import type { personInfoSchema } from './schemas';

export type PersonInfo = z.infer<typeof personInfoSchema>;
export type InfoChecklist = {
  key: string;
  label: string;
  checked: boolean;
}[];

import { z } from 'zod';

export const personInfoSchema = z.object({
  image: z.string().optional(),
  birth: z.string().optional(),
  death: z.string().optional(),
  description: z.string().optional(),
  wikiLink: z.string().optional()
});

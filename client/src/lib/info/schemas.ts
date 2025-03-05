import { z } from 'zod';

export const personInfoSchema = z.object({
  image: z.string().optional(),
  birth: z.string().optional(),
  death: z.string().optional(),
  residence: z.string().optional(),
  description: z.string().optional(),
  wikiLink: z.string().optional(),
  bcoords: z.string().optional(),
  dcoords: z.string().optional(),
  rcoords: z.string().optional()
});

import { z } from "zod";

export const personInfoSchema = z.object({
    image: z.string(),
    description: z.string(),
    attributes: z.record(z.string()),
    wikipedia_link: z.string()
});

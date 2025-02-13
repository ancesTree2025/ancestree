import { z } from "zod";
import type { personInfoSchema } from "./schemas";

export type PersonInfo = z.infer<typeof personInfoSchema>;

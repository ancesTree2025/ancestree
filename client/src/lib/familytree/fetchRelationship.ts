import { z } from 'zod';
import { apiResponseToTree, treeSchema } from './fetchTree';
import { Result } from 'typescript-result';
import type { PersonID } from './types';
import { env } from '$env/dynamic/public';

const relationshipSchema = z.object({
  relation: z.string(),
  links: treeSchema
});

export type Relationship = z.infer<typeof relationshipSchema>;

export async function fetchRelationship(
  fromId: PersonID,
  toName: string
): Promise<Result<Relationship, string>> {
  const response = await Result.fromAsyncCatching(
    fetch(`${env.PUBLIC_API_BASE_URL}/relation?orig=${fromId}&dest=${toName}`)
  ).mapError(() => 'Could not connect to server');
  if (response.getOrNull()?.status === 404) {
    return Result.error('Person not found');
  }
  const parsed = response.mapCatching(
    async (response) => (await response.json()) as Relationship,
    () => 'Could not parse server response'
  );
  return parsed.mapCatching(
    (json) => {
      console.log(apiResponseToTree(relationshipSchema.parse(json).links))
      return Result.ok(relationshipSchema.parse(json));
    },
    () => {
      const errorMessage = 'Server data in wrong format';
      console.error(errorMessage);
      return errorMessage;
    }
  );
}

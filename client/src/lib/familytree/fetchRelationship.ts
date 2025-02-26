import { z } from 'zod';
import { personIdSchema } from './fetchTree';
import exampleRelationship from '../data/exampleRelationship.json';
import { Result } from 'typescript-result';
import type { PersonID } from './types';
import { env } from '$env/dynamic/public';

const relationshipSchema = z.object({
  relation: z.string(),
  links: z.array(personIdSchema)
});

export type Relationship = z.infer<typeof relationshipSchema>;

export async function fetchRelationship(
  from: PersonID,
  to: PersonID,
  useFakeData: boolean
): Promise<Result<Relationship, string>> {
  if (useFakeData) {
    if (from === 'F' && to === 'GC') {
      return Result.ok({
        ...exampleRelationship,
        links: [from].concat(exampleRelationship.links)
      });
    }
    return Result.error('Only have sample data for qid1=D&qid2=C3');
  }
  const response = await Result.fromAsyncCatching(
    fetch(`${env.PUBLIC_API_BASE_URL}/relation?orig=${from}&dest=${to}`)
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
      const relationship = relationshipSchema.parse(json);
      return Result.ok({
        ...relationship,
        links: [from].concat(relationship.links)
      });
    },
    () => 'Server data in wrong format'
  );
}

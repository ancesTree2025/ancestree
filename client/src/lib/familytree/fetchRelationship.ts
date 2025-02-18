import { z } from 'zod';
import { personIdSchema } from './fetchTree';
import exampleRelationship from '../data/exampleRelationship.json';
import { Result } from 'typescript-result';
import type { PersonID } from './types';

const relationshipSchema = z.object({
  RelationDescription: z.string(),
  chain: z.array(personIdSchema)
});

export type Relationship = z.infer<typeof relationshipSchema>;

export async function fetchRelationship(
  from: PersonID,
  to: PersonID,
  useFakeData: boolean
): Promise<Result<Relationship, string>> {
  console.log(from, to)
  if (useFakeData) {
    if (from === "F" && to === "C3") {
      console.log('ok')
      return Result.ok({
        ...exampleRelationship,
        chain: [from].concat(exampleRelationship.chain).concat([to])
      });
    }
    console.log('err')
    return Result.error("Only have sample data for qid1=D&qid2=C3");
  }
  const response = await Result.fromAsyncCatching(
    fetch(`http://localhost:8080/relationship?qid1=${from}&qid2=${to}`)
  ).mapError(() => 'Could not connect to server');
  if (response.getOrNull()?.status === 404) {
    return Result.error('Person not found');
  }
  const parsed = response.mapCatching(
    async (response) => (await response.json()) as Relationship,
    () => 'Could not parse server response'
  );
  return parsed.mapCatching(
    (json) => relationshipSchema.parse(json),
    () => 'Server data in wrong format'
  );
}

import { Result } from 'typescript-result';
import { z } from 'zod';
import exampleData from './exampleData.json';

const personInfoSchema = z.object({
  image: z.string(),
  description: z.string(),
  attributes: z.record(z.string()),
  wikipedia_link: z.string()
});

export type PersonInfo = z.infer<typeof personInfoSchema>;

export async function fetchInfo(
  name: string,
  useFakeData: boolean
): Promise<Result<PersonInfo, string>> {
  if (useFakeData) {
    return Result.ok(exampleData);
  }
  const response = await Result.fromAsyncCatching(
    fetch(`http://localhost:8080/info?${new URLSearchParams({ name })}`)
  ).mapError(() => 'Could not connect to server');
  if (response.getOrNull()?.status === 404) {
    return Result.error('Person not found');
  }
  const parsed = response.mapCatching(
    async (response) => (await response.json()) as PersonInfo,
    () => 'Could not parse server response'
  );
  return parsed.mapCatching(
    (json) => personInfoSchema.parse(json),
    () => 'Server data in wrong format'
  );
}

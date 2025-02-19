import { Result } from 'typescript-result';
import exampleData from './exampleData.json';
import { personInfoSchema } from './schemas';
import type { PersonInfo } from './types';

export async function fetchInfo(
  qid: string,
  useFakeData: boolean
): Promise<Result<PersonInfo, string>> {
  if (useFakeData) {
    return Result.ok(exampleData);
  }
  const baseUrl = process.env.API_BASE_URL;
  const response = await Result.fromAsyncCatching(
    fetch(`${baseUrl}/info?${new URLSearchParams({ qid })}`)
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

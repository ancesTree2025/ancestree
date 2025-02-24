import { Result } from 'typescript-result';
import { z } from 'zod';
import { env } from '$env/dynamic/public';

const exampleNames = ['Alice', 'Bob', 'Charlie', 'David', 'Eve', 'Frank'];

const namesApiSchema = z.object({
  query: z.string(),
  autocomplete: z.array(z.string())
});

export async function fetchNames(
  searchQuery: string,
  useFakeData: boolean = false
): Promise<Result<string[], string>> {
  if (useFakeData) {
    return Result.ok(exampleNames);
  }

  const response = Result.fromAsyncCatching(
    fetch(`${env.PUBLIC_API_BASE_URL}/search/${searchQuery}`)
  ).mapError(() => 'Failed to fetch names');

  return response.mapCatching(
    async (res) => {
      const parsed = await namesApiSchema.safeParseAsync(await res.json());

      if (!parsed.success || searchQuery !== parsed.data.query) throw new Error();
      return parsed.data.autocomplete;
    },
    () => 'Failed to parse names response'
  );
}

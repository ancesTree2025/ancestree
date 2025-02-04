import { Result } from 'typescript-result';
import { z } from 'zod';

const exampleNames = ['Alice', 'Bob', 'Charlie', 'David', 'Eve', 'Frank'];

const namesApiSchema = z.object({
  query: z.string(),
  results: z.array(z.string())
});
type NamesApiResponse = z.infer<typeof namesApiSchema>;

export async function fetchNames(
  searchQuery: string,
  useFakeData: boolean = false
): Promise<Result<string[], string>> {
  if (useFakeData) {
    return Result.ok(exampleNames);
  }

  const response = Result.fromAsyncCatching(
    fetch(`http://localhost:8080/search/${searchQuery}`)
  ).mapError(() => 'Failed to fetch names');

  return response.mapCatching(
    async (res) => {
      const parsed = await namesApiSchema.safeParseAsync(await res.json());

      if (!parsed.success || searchQuery !== parsed.data.query) throw new Error();
      return parsed.data.results;
    },
    () => 'Failed to parse names response'
  );
}

import { transformResToTree } from '$lib';
import type { PageLoad } from './$types';
import { exampleTree } from '$lib/stubs';
import type { Tree } from '$lib/types';

export const load: PageLoad = async ({ url, fetch }) => {
  const { searchParams } = url;
  const useFakeData = searchParams.get('useFakeData') === 'true';
  const searchQuery = searchParams.get('q');

  if (!searchQuery)
    return {
      useFakeData,
      tree: null
    };

  if (useFakeData) return { useFakeData, tree: exampleTree as Tree };

  const treeResult = await transformResToTree(fetch(`http://localhost:8080/${searchQuery}`));

  if (treeResult.isOk()) {
    return {
      useFakeData,
      tree: treeResult.value
    };
  }
  return {
    useFakeData,
    tree: null
  };
};

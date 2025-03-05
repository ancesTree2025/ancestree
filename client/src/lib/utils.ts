export function zip<S1, S2>(
  firstCollection: Array<S1>,
  lastCollection: Array<S2>
): Array<[S1, S2]> {
  const length = Math.min(firstCollection.length, lastCollection.length);
  const zipped: Array<[S1, S2]> = [];

  for (let index = 0; index < length; index++) {
    zipped.push([firstCollection[index], lastCollection[index]]);
  }

  return zipped;
}

export function titleCase(str: string) {
  return str
    .toLowerCase()
    .split(' ')
    .map(function (word) {
      return word.charAt(0).toUpperCase() + word.slice(1);
    })
    .join(' ');
}

export function removeDuplicatesBy<T, R>(array: Iterable<T>, selector: (_: T) => R): T[] {
  const set = new Set<R>();
  const nubbed: T[] = [];

  for (const elem of array) {
    if (set.has(selector(elem))) continue;

    set.add(selector(elem));
    nubbed.push(elem);
  }

  return nubbed;
}

export type PersonID = string;

export type Person = {
  name: PersonID;
  x: number;
  y: number;
};

export type People = Map<string, Person>;

export type Marriage = {
  parents: PersonID[];
  children: PersonID[];
};

export type Marriages = Marriage[];

export type Tree = {
  people: People;
  marriages: Marriages;
};

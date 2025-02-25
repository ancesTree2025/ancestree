export type PersonID = string;

export type Person = {
  name: string;
};

// Needs to be an array for deep reactivity to work
export type People = [PersonID, Person][];

export type Marriage = {
  parents: PersonID[];
  children: PersonID[];
};

export type Marriages = Marriage[];

export type Tree = {
  focus: PersonID;
  people: People;
  marriages: Marriages;
};

export type Position = {
  x: number;
  y: number;
};

export type Positions = Record<PersonID, Position>;

export type MarriageHeights = number[];

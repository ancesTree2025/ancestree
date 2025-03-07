export type PersonID = string;

export type Person = {
  name: string;
};

// Needs to be an array for deep reactivity to work
export type People = [PersonID, Person, Gender][];

export type Gender = string;

export type Marriage = {
  parents: PersonID[];
  children: PersonID[];
  type: 'married' | 'unmarried';
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

export type MarriagePosition = {
  parent1: Position;
  parent2: Position;
  children: Position[];
  height: number;
  distance: number;
  offset: number;
  parent1ID: PersonID;
  parent2ID: PersonID;
  childrenIDs: PersonID[];
  type: 'married' | 'unmarried';
};

export type MarriagePositions = MarriagePosition[];

export type GroupID = number;
/**
 * Represents the assignments of people to "marriage groups" within a family tree.
 *
 * @property groups - A mapping from group ID to the members of that group.
 * @property members - A mapping from person ID to the group they are in.
 */
export type GroupAssignments = {
  groups: Map<GroupID, PersonID[]>;
  members: Record<PersonID, GroupID>;
};

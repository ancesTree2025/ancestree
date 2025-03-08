export type LoadingStatus = { state: 'idle' | 'loading' } | { state: 'error'; error: string };

export type PopupStatus = 'relationfinder' | 'filter' | null;
export type FilterOption = 'spousefamily' | 'sibling' | 'ancestor' | 'descendant' | 'unmarried';

export * from './familytree/types';
export * from './info/types';

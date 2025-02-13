export type LoadingStatus = { state: 'idle' | 'loading' } | { state: 'error'; error: string };

export * from './familytree/types';
export * from './info/types';

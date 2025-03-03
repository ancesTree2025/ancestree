import type { InfoChecklist } from '$lib/types';


export const checkboxOptions: InfoChecklist = [
	{ key: 'image', label: 'Show Image', checked: true },
	{ key: 'birth', label: 'Show Birth Date', checked: true },
	{ key: 'death', label: 'Show Death Date', checked: true },
	{ key: 'description', label: 'Show Description', checked: true },
	{ key: 'wikiLink', label: 'Show Wikipedia Link', checked: true }
];
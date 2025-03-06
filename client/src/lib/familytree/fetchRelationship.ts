import { z } from 'zod';
import { apiResponseToTree, treeSchema } from './fetchTree';
import { Result } from 'typescript-result';
import type { PersonID } from './types';
import { env } from '$env/dynamic/public';

const relationshipSchema = z.object({
  relation: z.string(),
  links: treeSchema
});

export type Relationship = z.infer<typeof relationshipSchema>;

export async function fetchRelationship(
  fromId: PersonID,
  toName: string
): Promise<Result<Relationship, string>> {
  const response = await Result.fromAsyncCatching(
    fetch(`${env.PUBLIC_API_BASE_URL}/relation?orig=${fromId}&dest=${toName}`)
  ).mapError(() => 'Could not connect to server');
  if (response.getOrNull()?.status === 404) {
    return Result.error('Person not found');
  }
  const parsed = response.mapCatching(
    async (response) => (await response.json()) as Relationship,
    () => 'Could not parse server response'
  );
  return parsed.mapCatching(
    (json) => {
      console.log(apiResponseToTree(relationshipSchema.parse(json).links))
      return Result.ok(relationshipSchema.parse(json));
    },
    () => {
      const errorMessage = 'Server data in wrong format';
      console.error(errorMessage);
      return errorMessage;
    }
  );
}

{
    "relation": "Brother's Wife's Wife's Wife",
    "links": {
        "root": {
            "data": {
                "id": "Elon Musk",
                "name": "Elon Musk",
                "gender": "male"
            },
            "id": "Elon Musk",
            "depth": 0
        },
        "nodes": [
            {
                "data": {
                    "id": "Elon Musk",
                    "name": "Elon Musk",
                    "gender": "male"
                },
                "id": "Elon Musk",
                "depth": 0
            },
            {
                "data": {
                    "id": "Errol Musk",
                    "name": "Errol Musk",
                    "gender": "male"
                },
                "id": "Errol Musk",
                "depth": -1
            },
            {
                "data": {
                    "id": "Kimbal Musk",
                    "name": "Kimbal Musk",
                    "gender": "male"
                },
                "id": "Kimbal Musk",
                "depth": 0
            },
            {
                "data": {
                    "id": "Christiana Wyly",
                    "name": "Christiana Wyly",
                    "gender": "female"
                },
                "id": "Christiana Wyly",
                "depth": 0
            },
            {
                "data": {
                    "id": "Skin",
                    "name": "Skin",
                    "gender": "female"
                },
                "id": "Skin",
                "depth": 0
            },
            {
                "data": {
                    "id": "Ladyfag",
                    "name": "Ladyfag",
                    "gender": "female"
                },
                "id": "Ladyfag",
                "depth": 0
            },
            {
                "data": {
                    "id": "Maye Musk",
                    "name": "Maye Musk",
                    "gender": "female"
                },
                "id": "Maye Musk",
                "depth": -1
            }
        ],
        "edges": [
            {
                "node1": "Elon Musk",
                "node2": "Errol Musk",
                "tag": ""
            },
            {
                "node1": "Errol Musk",
                "node2": "Kimbal Musk",
                "tag": ""
            },
            {
                "node1": "Kimbal Musk",
                "node2": "Christiana Wyly",
                "tag": ""
            },
            {
                "node1": "Christiana Wyly",
                "node2": "Skin",
                "tag": ""
            },
            {
                "node1": "Skin",
                "node2": "Ladyfag",
                "tag": ""
            },
            {
                "node1": "Errol Musk",
                "node2": "Maye Musk",
                "tag": ""
            },
            {
                "node1": "Maye Musk",
                "node2": "Elon Musk",
                "tag": ""
            }
        ]
    }
}

{
  "focus": "Elon Musk",
  "people": [
    [
      "Elon Musk",
      {
        "name": "Elon Musk"
      },
      "male"
    ],
    [
      "Errol Musk",
      {
        "name": "Errol Musk"
      },
      "male"
    ],
    [
      "Kimbal Musk",
      {
        "name": "Kimbal Musk"
      },
      "male"
    ],
    [
      "Christiana Wyly",
      {
        "name": "Christiana Wyly"
      },
      "female"
    ],
    [
      "Skin",
      {
        "name": "Skin"
      },
      "female"
    ],
    [
      "Ladyfag",
      {
        "name": "Ladyfag"
      },
      "female"
    ],
    [
      "Maye Musk",
      {
        "name": "Maye Musk"
      },Maye Musk
      "female"
    ]
  ],
  "marriages": [
    {
      "parents": [
        "Kimbal Musk",
        "Christiana Wyly"
      ],
      "children": []
    },
    {
      "parents": [
        "Christiana Wyly",
        "Skin"
      ],
      "children": []
    },
    {
      "parents": [
        "Skin",
        "Ladyfag"
      ],
      "children": []
    },
    {
      "parents": [
        "Errol Musk",
        "Maye Musk"
      ],
      "children": [
        "Elon Musk"
      ]
    }
  ]
}

*/
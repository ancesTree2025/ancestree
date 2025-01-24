package org.data.models

import kotlinx.serialization.json.JsonObject

typealias QID = String

typealias Label = String

typealias Relation = Map<String, List<String>>

typealias Claim = JsonObject

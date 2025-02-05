package org.data.models

import kotlinx.serialization.Serializable

/* The classes below are used to extract the information made in ChatGPT requests. */

/** Response from asking for a summary on a person */
@Serializable data class GPTResponse(val choices: List<Choice>)

@Serializable data class Choice(val message: Message)

@Serializable data class Message(val content: String)

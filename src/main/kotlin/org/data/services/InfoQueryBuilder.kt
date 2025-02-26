package org.data.services

class InfoQueryBuilder(
  var image: Boolean = false,
  var birth: Boolean = false,
  var death: Boolean = false,
  var description: Boolean = false,
  var wikiLink: Boolean = false,
) {

  // For testing purposes
  fun withAll(): InfoQueryBuilder {
    this.image = true
    this.birth = true
    this.death = true
    this.description = true
    this.wikiLink = true
    return this
  }

  fun withImage(): InfoQueryBuilder {
    this.image = true
    return this
  }

  fun withBirth(): InfoQueryBuilder {
    this.birth = true
    return this
  }

  fun withDeath(): InfoQueryBuilder {
    this.death = true
    return this
  }

  fun withDescription(): InfoQueryBuilder {
    this.description = true
    return this
  }

  fun withWikiLink(): InfoQueryBuilder {
    this.wikiLink = true
    return this
  }
}

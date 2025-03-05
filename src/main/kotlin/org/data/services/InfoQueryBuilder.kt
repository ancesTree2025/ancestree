package org.data.services

class InfoQueryBuilder(
  var image: Boolean = false,
  var birth: Boolean = false,
  var death: Boolean = false,
  var description: Boolean = false,
  var wikiLink: Boolean = false,
  var bcoords: Boolean = false,
  var dcoords: Boolean = false,
  var start: Boolean = false,
  var end: Boolean = false,
  var residence: Boolean = false,
  var rcoords: Boolean = false,
) {

  // For testing purposes
  fun withAll(): InfoQueryBuilder {
    this.image = true
    this.birth = true
    this.death = true
    this.description = true
    this.wikiLink = true
    this.bcoords = true
    this.dcoords = true
    this.start = true
    this.end = true
    this.residence = true
    this.rcoords = true
    return this
  }

  fun withImage(): InfoQueryBuilder {
    this.image = true
    return this
  }

  fun withBCoords(): InfoQueryBuilder {
    this.bcoords = true
    return this
  }

  fun withDCoords(): InfoQueryBuilder {
    this.dcoords = true
    return this
  }

  fun withResidence(): InfoQueryBuilder {
    this.residence = true
    return this
  }

  fun withResCoords(): InfoQueryBuilder {
    this.rcoords = true
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

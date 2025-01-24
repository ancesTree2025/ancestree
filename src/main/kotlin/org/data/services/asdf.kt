package org.data.services

suspend fun main() {

  println(WikiLookupService().query("Henry 8th"))
}

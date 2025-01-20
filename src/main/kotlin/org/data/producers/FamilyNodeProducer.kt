package org.data.producers

import org.data.models.FamilyData
import org.data.services.LookupService
import org.domain.models.*
import org.domain.producers.NodeProducer

/**
 * A class to produce family nodes, which will be connected by marriage edges.
 */
class FamilyNodeProducer : NodeProducer<String, FamilyData> {


    /**
     * Produces a node for a particular person using cached results and Wiki queries.
     *
     * @param input An input string of a person's name.
     * @returns A node housing FamilyData, containing individual-specific information.
     */
    override suspend fun produce(input: String): Node<FamilyData> {
        val tuple3 = LookupService.query(input)

        val qid = tuple3.first
        val label = tuple3.second.first
        val relation = tuple3.second.second

        val familyInfo = FamilyData(qid, label, relation["Gender"]!![0], 0)

        return Node(familyInfo)
    }

    /**
     * Produces a node for some number of people using cached results and Wiki queries.
     *
     * @param input ???
     * @returns A list of FamilyData nodes with information for each person passed in.
     */
    override suspend fun produceAll(input: String): List<Node<FamilyData>> {
        TODO()
    }
}
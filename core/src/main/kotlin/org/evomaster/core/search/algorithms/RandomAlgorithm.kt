package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.SearchAlgorithm


class RandomAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.RANDOM
    }

    override fun setupBeforeSearch() {
        // Nothing needs to be done before starting the search
    }

    override fun searchOnce() {

        val individual = sampler.sampleAtRandom()

        ff.calculateCoverage(individual)?.run { archive.addIfNeeded(this) }

    }

}

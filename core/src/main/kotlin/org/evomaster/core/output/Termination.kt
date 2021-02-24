package org.evomaster.core.output

enum class Termination(val suffix: String,
                       val comment: String){
        FAULTS("_faults", "This file contains test cases that are likely to indicate faults."),
        SUCCESSES("_successes", "This file contains test cases that represent successful calls."),
        OTHER("_others", "This file contains test cases that represent failed calls, but not indicative of faults."),
        SUMMARY("_fault_representatives", "This file contains one example of each category of fault. The test cases in this file are a subset of the set of test cases likely to indicate faults."),
        CLUSTERED("_clustered", "This file contains test cases that are likely to indicate faults, clustered by the selected criteria."),
        IN_PROGRESS("_snapshot", "This file contains a partial solution."),
        NONE("","")
}
